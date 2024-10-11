package com.dhn.client.controller;

import com.dhn.client.bean.ImageBean;
import com.dhn.client.bean.KAORequestBean;
import com.dhn.client.bean.SQLParameter;
import com.dhn.client.service.KAORequestService;
import com.dhn.client.service.KAOService;
import com.dhn.client.service.SendService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@Order(5)
public class FTSendRequest implements ApplicationListener<ContextRefreshedEvent> {
    public static boolean isStart = false;
    private boolean isProc = false;
    private boolean isProcImg = false;
    private SQLParameter param = new SQLParameter();
    private String dhnServer;
    private String userid;
    private String preGroupNo = "";
    private String basepath = "";
    private String at_log_table;

    @Autowired
    private KAORequestService kaoRequestService;

    @Autowired
    private KAOService kaoService;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private SendService sendService;

    @Autowired
    private ScheduledAnnotationBeanPostProcessor posts;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        param.setAt_table(appContext.getEnvironment().getProperty("dhnclient.at_table"));
        at_log_table = appContext.getEnvironment().getProperty("dhnclient.at_log_table");
        param.setFtkao_use(appContext.getEnvironment().getProperty("dhnclient.ftkao_use"));
        param.setDatabase(appContext.getEnvironment().getProperty("dhnclient.database"));
        param.setSequence(appContext.getEnvironment().getProperty("dhnclient.at_seq"));
        basepath = appContext.getEnvironment().getProperty("dhnclient.file_base_path")==null?"":appContext.getEnvironment().getProperty("dhnclient.file_base_path");
        param.setMsg_type("FT");

        dhnServer = appContext.getEnvironment().getProperty("dhnclient.server");
        userid = appContext.getEnvironment().getProperty("dhnclient.userid");

        if (param.getFtkao_use() != null && param.getFtkao_use().equalsIgnoreCase("Y")) {
            try{
                kaoRequestService.atTableCheck(param);
                isStart = true;
                log.info("FT 초기화 완료");
            }catch (Exception e){
                log.error("{}테이블 생성 오류 : ", param.getAt_table() + e.getMessage());
            }
        } else {
            posts.postProcessBeforeDestruction(this, null);
        }
    }

    @Scheduled(fixedDelay = 100)
    public void SendProcess() {
        if(isStart && !isProc && sendService.getActiveFTThreads() < SendService.MAX_THREADS) {
            isProc = true;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
            LocalDateTime now = LocalDateTime.now();
            String group_no = "5" + now.format(formatter);

            if(!group_no.equals(preGroupNo)) {

                try {
                    int cnt = kaoRequestService.selectFTRequestCount(param);

                    if(cnt > 0) {
                        param.setGroup_no(group_no);
                        kaoRequestService.updateFTGroupNo(param);
                        List<KAORequestBean> _list = kaoRequestService.selectFTRequests(param);

                        SQLParameter paramCopy = param.toBuilder().build();

                        sendService.FTSendAsync(_list, paramCopy, group_no);

                    }

                }catch (Exception e) {
                    log.error("FT 메세지 전송 오류 : " + e.toString());
                }
                preGroupNo = group_no;
            }
            isProc = false;
        } else if (sendService.getActiveFTThreads() >= SendService.MAX_THREADS) {
            //log.info("KAO 스케줄러: 최대 활성화된 쓰레드 수에 도달했습니다. 다음 주기에 다시 시도합니다.");
        }
    }


    @Scheduled(fixedDelay = 100)
    public void GetFTImage(){
        if(isStart && !isProcImg) {
            isProcImg = true;

            try{
                int cnt = kaoRequestService.selectFtImageCount(param);

                if(cnt > 0){
                    List<ImageBean> ftimages = kaoRequestService.selectFtImage(param);

                    for (ImageBean ftimage : ftimages) {

                        // 헤더 설정
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                        headers.set("userid", userid);

                        // MultiValueMap을 사용해 파일 데이터 전송 준비
                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("userid", userid);

                        if (ftimage.getFtimagepath() != null && !ftimage.getFtimagepath().isEmpty()) {
                            File file = new File(basepath + ftimage.getFtimagepath());
                            body.add("image", new org.springframework.core.io.FileSystemResource(file));
                        }

                        // HttpEntity 생성
                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        RestTemplate restTemplate = new RestTemplate();
                        try{
                            ResponseEntity<String> response = restTemplate.exchange(dhnServer + "ft/image", HttpMethod.POST, requestEntity, String.class);

                            LocalDate now = LocalDate.now();
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
                            String currentMonth = now.format(formatter);

                            if (response.getStatusCode() == HttpStatus.OK) {
                                String responseBody = response.getBody();
                                ObjectMapper mapper = new ObjectMapper();
                                Map<String, String> res = mapper.readValue(responseBody, Map.class);

                                param.setFt_image_code(res.get("code"));
                                param.setMsgid(ftimage.getMsgid());

                                if(param.getFt_image_code().equals("0000")){
                                    log.info("친구톡 이미지 URL : "+res.get("image"));

                                    param.setFt_image_url(res.get("image"));
                                    kaoRequestService.updateFTImageUrl(param);
                                }else{

                                    log.error("친구톡 이미지 등록 실패 : "+res.toString());

                                    param.setAt_log_table(at_log_table+"_"+currentMonth);
                                    if(param.getFt_image_code().equals("error")){
                                        param.setFt_image_code("9999");
                                    }
                                    kaoRequestService.updateFTImageFail(param);
                                }
                            } else {
                                log.error("친구톡 이미지 등록 실패 통신오류 : "+response.getBody());
                                param.setAt_log_table(at_log_table+"_"+currentMonth);
                                if(param.getFt_image_code().equals("error")){
                                    param.setFt_image_code("9999");
                                }
                                kaoRequestService.updateFTImageFail(param);
                            }

                        }catch (Exception e){
                            log.error("FT Image URL 등록 오류: ", e.getMessage());
                        }
                    }
                }

            }catch (Exception e) {
                log.error("FT Image 등록 오류 : " + e.toString());
            }
        }
        isProcImg = false;
    }
}
