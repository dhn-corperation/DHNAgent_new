package com.dhn.client.controller;

import com.dhn.client.bean.*;
import com.dhn.client.service.FTRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Component
@Slf4j
public class KFSendRequest implements ApplicationListener<ContextRefreshedEvent> {

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
    private FTRequestService ftRequestService;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private ScheduledAnnotationBeanPostProcessor posts;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        param.setMsg_table(appContext.getEnvironment().getProperty("dhnclient.msg_table"));
        param.setKakao_use(appContext.getEnvironment().getProperty("dhnclient.kakao_use"));
        param.setDatabase(appContext.getEnvironment().getProperty("dhnclient.database"));
        param.setSequence(appContext.getEnvironment().getProperty("dhnclient.msg_seq"));
        param.setMsg_type("KF");

        dhnServer = appContext.getEnvironment().getProperty("dhnclient.server");
        userid = appContext.getEnvironment().getProperty("dhnclient.userid");
        log_back = appContext.getEnvironment().getProperty("dhnclient.log_back","Y");
        log_table = appContext.getEnvironment().getProperty("dhnclient.log_table");


        if (param.getKakao_use() != null && param.getKakao_use().equalsIgnoreCase("Y")) {
            isStart = true;
            log.info("FT (친구톡-구) 초기화 완료");
        } else {
            posts.postProcessBeforeDestruction(this, null);
        }

    }

    @Scheduled(fixedDelay = 100)
    public void GetFTImage(){
        if(isStart && !isProcImg) {
            isProcImg = true;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
            LocalDateTime now = LocalDateTime.now();
            String img_group_no = "IMG" + now.format(formatter);

            if(!imgPreGroupNo.equals(img_group_no)) {
                try{


                    int cnt = ftRequestService.selectFtImageCount(param);

                    if(cnt > 0){

                        param.setImg_group_no(img_group_no);
                        ftRequestService.updateFTImageGroup(param);

                        List<ImageBean> ftimages = ftRequestService.selectFtImage(param);

                        for (ImageBean ftimage : ftimages) {
                            SQLParameter ftiparam = new SQLParameter();
                            ftiparam.setMsg_table(param.getMsg_table());
                            ftiparam.setDatabase(param.getDatabase());
                            ftiparam.setSequence(param.getSequence());
                            ftiparam.setMms_key(ftimage.getFtimagepath());
                            ftiparam.setDist_value(ftimage.getWide());
                            ftiparam.setImg_group_no(img_group_no);

                            LocalDate now_log = LocalDate.now();
                            DateTimeFormatter formatter_log = DateTimeFormatter.ofPattern("yyyyMM");
                            String currentMonth_log = now_log.format(formatter_log);

                            HttpHeaders headers = new HttpHeaders();
                            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                            headers.set("userid", userid);

                            // MultiValueMap을 사용해 파일 데이터 전송 준비
                            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                            body.add("userid", userid);

                            if (ftimage.getFtimagepath() != null && !ftimage.getFtimagepath().isEmpty()) {
                                String rawPath = ftimage.getFtimagepath();
                                File file = new File(rawPath);

                                if (!file.exists() || !file.isFile()) {
                                    log.warn("FT Image 파일 없음 : " + rawPath);

                                    if(param.getLog_back() != null && param.getLog_back().equalsIgnoreCase("Y")){
                                        ftiparam.setLog_table(log_table + "_" + currentMonth_log);
                                    }else{
                                        ftiparam.setLog_table(log_table);
                                    }

                                    ftiparam.setImg_err_msg("FT Image 파일 없음");
                                    ftiparam.setFt_image_code("9999");
                                    ftRequestService.updateFTImageFail(ftiparam);

                                    continue;
                                }

                                body.add("image", new org.springframework.core.io.FileSystemResource(file));
                            }

                            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                            RestTemplate restTemplate = new RestTemplate();
                            try{

                                String url = "ft/image";

                                if ("Y".equals(ftimage.getWide())) {
                                    url = "ft/wide/image";
                                } else {
                                    url = "ft/image";
                                }

                                ResponseEntity<String> response = restTemplate.exchange(dhnServer + url, HttpMethod.POST, requestEntity, String.class);


                                if (response.getStatusCode() == HttpStatus.OK) {
                                    String responseBody = response.getBody();
                                    ObjectMapper mapper = new ObjectMapper();
                                    Map<String, String> res = mapper.readValue(responseBody, Map.class);

                                    ftiparam.setFt_image_code(res.get("code"));
                                    ftiparam.setImg_err_msg(res.get("message"));

                                    if(ftiparam.getFt_image_code().equals("0000")){
                                        log.info("친구톡 이미지 URL : "+res.get("image"));

                                        ftiparam.setFt_image_url(res.get("image"));
                                        ftRequestService.updateFTImageUrl(ftiparam);
                                    }else{

                                        log.warn("친구톡 이미지 등록 실패 : "+res.toString());

                                        if(param.getLog_back() != null && param.getLog_back().equalsIgnoreCase("Y")){
                                            ftiparam.setLog_table(log_table + "_" + currentMonth_log);
                                        }else{
                                            ftiparam.setLog_table(log_table);
                                        }
                                        if(ftiparam.getFt_image_code().equals("error")){
                                            ftiparam.setFt_image_code("9999");
                                        }
                                        ftiparam.setImg_err_msg(res.get("message"));
                                        ftRequestService.updateFTImageFail(ftiparam);
                                    }
                                } else {
                                    log.error("친구톡 이미지 등록 실패 통신오류 : "+response.getBody());
                                    if(param.getLog_back() != null && param.getLog_back().equalsIgnoreCase("Y")){
                                        ftiparam.setLog_table(log_table + "_" + currentMonth_log);
                                    }else{
                                        ftiparam.setLog_table(log_table);
                                    }
                                    if(ftiparam.getFt_image_code().equals("error")){
                                        ftiparam.setFt_image_code("9999");
                                    }
                                    ftiparam.setImg_err_msg("KAKAO 통신 오류");
                                    ftRequestService.updateFTImageFail(ftiparam);
                                }

                            }catch (Exception e){
                                log.error("FT Image URL 등록 오류: ", e.getMessage());
                            }
                        }


                    }

                }catch (Exception e) {
                    log.error("FT Image 등록 오류 : " + e.toString());
                }
                imgPreGroupNo = img_group_no;
            }
            isProcImg = false;
        }

    }

    @Scheduled(fixedDelay = 100)
    public void SendProcess() {
        if(isStart && !isProc) {
            isProc = true;

            ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor) executorService;
            int activeThreads = poolExecutor.getActiveCount();

            if(activeThreads < 2){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
                LocalDateTime now = LocalDateTime.now();
                String group_no = "KF" + now.format(formatter);

                if(!group_no.equals(preGroupNo)) {
                    try {
                        int cnt = ftRequestService.selectOldFTRequestCount(param);

                        if(cnt > 0){
                            param.setGroup_no(group_no);
                            ftRequestService.updateOldFTGroupNo(param);

                            executorService.submit(() -> APIProcess(group_no));
                        }

                    }catch (Exception e){
                        log.error("FT 메세지 전송 오류 : " + e.toString());
                    }
                    preGroupNo = group_no;
                }
            }
            isProc = false;
        }
    }

    private void APIProcess(String group_no) {
        try{
            SQLParameter sendParam = new SQLParameter();
            sendParam.setGroup_no(group_no);
            sendParam.setMsg_table(param.getMsg_table());
            sendParam.setDatabase(param.getDatabase());
            sendParam.setSequence(param.getSequence());
            sendParam.setMsg_type(param.getMsg_type());

            List<FTDataBean> _list = ftRequestService.selectOldFTRequests(sendParam);

            List<FTRequestBean> sendList = new ArrayList<>();
            List<String> invalidList = new ArrayList<>();

            for (FTDataBean ftDataBean : _list ) {
                FTRequestBean sendBean = new FTRequestBean();
                sendBean.setMsgid(ftDataBean.getMsgid());
                sendBean.setAdflag(ftDataBean.getAdflag());
                sendBean.setMessagetype("F1");
                sendBean.setMsg(ftDataBean.getMsg());
                sendBean.setMsgsms(ftDataBean.getMsgsms());
                sendBean.setPcom("P");
                sendBean.setPinvoice(ftDataBean.getPinvoice());
                sendBean.setPhn(ftDataBean.getPhn());
                sendBean.setProfile(ftDataBean.getProfile());
                sendBean.setRegdt(ftDataBean.getRegdt());
                sendBean.setReservedt(ftDataBean.getReservedt());
                sendBean.setSmskind(ftDataBean.getSmskind());
                sendBean.setSmslmstit(ftDataBean.getSmslmstit());
                sendBean.setSmssender(ftDataBean.getSmssender());
                sendBean.setHeader(ftDataBean.getHeader());
                sendBean.setKisacode(ftDataBean.getKisacode());
                sendBean.setPushalarm(ftDataBean.getPushalarm());

                String wide = ftDataBean.getWide();
                sendBean.setWide( (wide != null && wide.equalsIgnoreCase("Y")) ? "Y" : "N");

                ObjectMapper mapper = new ObjectMapper();

                ObjectNode attNode = mapper.createObjectNode();

                if (ftDataBean.getImageurl() != null && !ftDataBean.getImageurl().trim().isEmpty()) {
                    ObjectNode imageNode = mapper.createObjectNode();
                    imageNode.put("img_url", ftDataBean.getImageurl().trim());

                    if (ftDataBean.getImagelink() != null && !ftDataBean.getImagelink().trim().isEmpty()) {
                        imageNode.put("img_link", ftDataBean.getImagelink().trim());
                    }
                    attNode.set("image", imageNode);

                    if ("Y".equals(sendBean.getWide())) {
                        sendBean.setMessagetype("F3");
                    } else {
                        sendBean.setMessagetype("F2");
                    }
                }

                ArrayNode buttonArray = mapper.createArrayNode();
                String[] btnStrs = new String[] {
                        ftDataBean.getButton1(),
                        ftDataBean.getButton2(),
                        ftDataBean.getButton3(),
                        ftDataBean.getButton4(),
                        ftDataBean.getButton5()
                };

                for (String btnStr : btnStrs) {
                    if (btnStr == null || btnStr.trim().isEmpty()) break; // 중간에서 끊기면 거기까지
                    // 그대로 JSON 문자열 → JsonNode 로 변환해서 추가
                    buttonArray.add(mapper.readTree(btnStr));
                }

                if (buttonArray.size() > 0) {
                    attNode.set("button", buttonArray);
                }

                if (attNode.size() > 0) {
                    sendBean.setAttachments(attNode.toString());
                }

                sendList.add(sendBean);
            }

            if (!sendList.isEmpty()) {
                StringWriter sw = new StringWriter();
                ObjectMapper om = new ObjectMapper();
                om.writeValue(sw, sendList); // List를 Json화 하여 문자열 저장

                HttpHeaders header = new HttpHeaders();

                header.setContentType(MediaType.APPLICATION_JSON);
                header.set("userid", userid);

                RestTemplate rt = new RestTemplate();
                HttpEntity<String> entity = new HttpEntity<String>(sw.toString(), header);

                try {
                    ResponseEntity<String> response = rt.postForEntity(dhnServer + "req", entity, String.class);
                    Map<String, String> res = om.readValue(response.getBody().toString(), Map.class);
                    log.info(res.toString());
                    if (response.getStatusCode() == HttpStatus.OK) { // 데이터 정상적으로 전달
                        ftRequestService.updateFTSendComplete(sendParam);
                        log.info("FT 메세지 전송 완료 : " + response.getStatusCode() + " / " + group_no + " / " + _list.size() + " 건");
                    } else { // API 전송 실패시
                        log.error("({}) FT 메세지 전송오류 : {}",res.get("userid"), res.get("message"));
                        ftRequestService.updateFTSendInit(sendParam);
                    }
                } catch (Exception e) {
                    log.error("FT 메세지 전송 오류 : " + e.toString());
                    ftRequestService.updateFTSendInit(sendParam);
                }
            }
        }catch (Exception e){
            log.error("FT 메세지 전송 오류 : " + e.toString());
        }
    }


}
