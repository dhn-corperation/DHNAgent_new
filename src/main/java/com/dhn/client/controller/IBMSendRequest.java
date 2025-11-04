package com.dhn.client.controller;

import com.dhn.client.bean.ImageBean;
import com.dhn.client.bean.SQLParameter;
import com.dhn.client.service.BMRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Component
@Slf4j
public class IBMSendRequest implements ApplicationListener<ContextRefreshedEvent> {

    public static boolean isStart = false;
    private boolean isProc = false;
    private boolean isProcImg = false;
    private SQLParameter param = new SQLParameter();
    private String dhnServer;
    private String userid;
    private String preGroupNo = "";
    private String imgPreGroupNo = "";
    private String log_back = "";
    private String log_table = "";

    private static final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Autowired
    private BMRequestService bmRequestService;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private ScheduledAnnotationBeanPostProcessor posts;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        param.setMsg_table(appContext.getEnvironment().getProperty("dhnclient.msg_table"));
        param.setBrand_use(appContext.getEnvironment().getProperty("dhnclient.brand_use"));
        param.setDatabase(appContext.getEnvironment().getProperty("dhnclient.database"));
        param.setSequence(appContext.getEnvironment().getProperty("dhnclient.msg_seq"));
        param.setMsg_type("IBM");

        dhnServer = appContext.getEnvironment().getProperty("dhnclient.server");
        userid = appContext.getEnvironment().getProperty("dhnclient.userid");
        log_back = appContext.getEnvironment().getProperty("dhnclient.log_back","Y");
        log_table = appContext.getEnvironment().getProperty("dhnclient.log_table");


        if (param.getBrand_use() != null && param.getBrand_use().equalsIgnoreCase("Y")) {
            isStart = true;
            log.info("파일전송 브랜드메시지 자유형 초기화 완료");
        } else {
            posts.postProcessBeforeDestruction(this, null);
        }

    }

    @Scheduled(fixedDelay = 100)
    public void GetBMImage(){
        if(isStart && !isProcImg) {
            isProcImg = true;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
            LocalDateTime now = LocalDateTime.now();
            String img_group_no = "IMG" + now.format(formatter);

            if(!imgPreGroupNo.equals(img_group_no)) {
                try{


                    int cnt = bmRequestService.selectIBMImageCount(param);

                    if(cnt > 0){

                        param.setImg_group_no(img_group_no);
                        bmRequestService.updateIBMImageGroup(param);

                        List<ImageBean> bmimages = bmRequestService.selectIBMImage(param);

                        for (ImageBean bmimage : bmimages) {
                            SQLParameter bmiparam = new SQLParameter();
                            bmiparam.setMsg_table(param.getMsg_table());
                            bmiparam.setDatabase(param.getDatabase());
                            bmiparam.setSequence(param.getSequence());
                            bmiparam.setMms_key(bmimage.getBmimagepath());
                            bmiparam.setDist_value(bmimage.getWide());
                            bmiparam.setImg_group_no(img_group_no);

                            LocalDate now_log = LocalDate.now();
                            DateTimeFormatter formatter_log = DateTimeFormatter.ofPattern("yyyyMM");
                            String currentMonth_log = now_log.format(formatter_log);

                            HttpHeaders headers = new HttpHeaders();
                            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                            headers.set("userid", userid);

                            String url = "";

                            if ("Y".equals(bmimage.getWide())) {
                                headers.set("messagetype","B3");
                                url = "/bm/image/wide";

                            } else {
                                headers.set("messagetype","B2");
                                url = "/bm/image/default";
                            }

                            // MultiValueMap을 사용해 파일 데이터 전송 준비
                            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

                            if (bmimage.getBmimagepath() != null && !bmimage.getBmimagepath().isEmpty()) {
                                String rawPath = bmimage.getBmimagepath();
                                File file = new File(rawPath);

                                if (!file.exists() || !file.isFile()) {
                                    log.warn("IBM Image 파일 없음 : " + rawPath);

                                    if(param.getLog_back() != null && param.getLog_back().equalsIgnoreCase("Y")){
                                        bmiparam.setLog_table(log_table + "_" + currentMonth_log);
                                    }else{
                                        bmiparam.setLog_table(log_table);
                                    }

                                    bmiparam.setImg_err_msg("IBM Image 파일 없음");
                                    bmiparam.setFt_image_code("0019");
                                    bmRequestService.updateIBMImageFail(bmiparam);

                                    continue;
                                }

                                body.add("image1", new org.springframework.core.io.FileSystemResource(file));
                                body.add("image_path1", rawPath);
                            }

                            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                            RestTemplate restTemplate = new RestTemplate();
                            try{

                                ResponseEntity<String> response = null;

                                try{
                                    response = restTemplate.exchange(dhnServer + url, HttpMethod.POST, requestEntity, String.class);
                                }catch (Exception e){
                                    log.error("브랜드 이미지 등록 실패 통신오류 : {}", e.getMessage());
                                    bmRequestService.updateIBMImageUploadFail(bmiparam);
                                    continue;
                                }

                                if (response.getStatusCode() == HttpStatus.OK) {
                                    String responseBody = response.getBody();
                                    ObjectMapper mapper = new ObjectMapper();

                                    Map<String, String> res = mapper.readValue(responseBody, Map.class);

                                    log.info("브랜드 이미지 등록 결과 : {}",res.toString());

                                    bmiparam.setFt_image_code(res.get("code"));
                                    bmiparam.setImg_err_msg(res.get("message"));

                                    if(bmiparam.getFt_image_code().equals("0000")){

                                        bmiparam.setFt_image_url(res.get("image1"));
                                        bmRequestService.updateIBMImageUrl(bmiparam);
                                    }else{

                                        if(param.getLog_back() != null && param.getLog_back().equalsIgnoreCase("Y")){
                                            bmiparam.setLog_table(log_table + "_" + currentMonth_log);
                                        }else{
                                            bmiparam.setLog_table(log_table);
                                        }

                                        bmiparam.setFt_image_code(res.get("code"));
                                        bmiparam.setImg_err_msg(res.get("message"));
                                        bmRequestService.updateIBMImageFail(bmiparam);
                                    }
                                } else {
                                    log.error("브랜드 이미지 등록 실패 통신오류 : {}",response.getBody());
                                    bmRequestService.updateIBMImageUploadFail(bmiparam);
                                }

                            }catch (Exception e){
                                log.error("IBM Image URL 등록 오류: {}", e.getMessage());
                            }
                        }


                    }

                }catch (Exception e) {
                    log.error("IBM Image 등록 오류 : {}",e.toString());
                }
                imgPreGroupNo = img_group_no;
            }
            isProcImg = false;
        }
    }

}
