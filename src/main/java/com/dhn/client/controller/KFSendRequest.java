package com.dhn.client.controller;

import com.dhn.client.bean.*;
import com.dhn.client.service.FTRequestService;
import com.fasterxml.jackson.databind.JsonNode;
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
            log.info("KF (친구톡-구) 초기화 완료");
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

                            if ("Y".equals(ftimage.getWide())) {
                                headers.set("messagetype","F3");
                            } else {
                                headers.set("messagetype","F2");
                            }

                            // MultiValueMap을 사용해 파일 데이터 전송 준비
                            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

                            if (ftimage.getFtimagepath() != null && !ftimage.getFtimagepath().isEmpty()) {
                                String rawPath = ftimage.getFtimagepath();
                                File file = new File(rawPath);

                                if (!file.exists() || !file.isFile()) {
                                    log.warn("KF Image 파일 없음 : " + rawPath);

                                    if(param.getLog_back() != null && param.getLog_back().equalsIgnoreCase("Y")){
                                        ftiparam.setLog_table(log_table + "_" + currentMonth_log);
                                    }else{
                                        ftiparam.setLog_table(log_table);
                                    }

                                    ftiparam.setImg_err_msg("KF Image 파일 없음");
                                    ftiparam.setFt_image_code("0019");
                                    ftRequestService.updateFTImageFail(ftiparam);

                                    continue;
                                }

                                body.add("image1", new org.springframework.core.io.FileSystemResource(file));
                                body.add("image_path1", rawPath);
                            }

                            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                            RestTemplate restTemplate = new RestTemplate();
                            try{
                                String url = "dhn/ft/image";

                                ResponseEntity<String> response = null;

                                try{
                                    response = restTemplate.exchange(dhnServer + url, HttpMethod.POST, requestEntity, String.class);
                                }catch (Exception e){
                                    log.error("친구톡 이미지 등록 실패 통신오류 : "+e.getMessage());
                                    ftRequestService.updateFTImageUploadFail(ftiparam);
                                    continue;
                                }

                                if (response.getStatusCode() == HttpStatus.OK) {
                                    String responseBody = response.getBody();
                                    ObjectMapper mapper = new ObjectMapper();

                                    Map<String, String> res = mapper.readValue(responseBody, Map.class);

                                    log.info("친구톡 이미지 등록 결과 : {}",res.toString());

                                    ftiparam.setFt_image_code(res.get("code"));
                                    ftiparam.setImg_err_msg(res.get("message"));

                                    if(ftiparam.getFt_image_code().equals("0000")){

                                        ftiparam.setFt_image_url(res.get("image1"));
                                        ftRequestService.updateFTImageUrl(ftiparam);
                                    }else{

                                        log.warn("친구톡 이미지 등록 실패 : "+res.toString());

                                        if(param.getLog_back() != null && param.getLog_back().equalsIgnoreCase("Y")){
                                            ftiparam.setLog_table(log_table + "_" + currentMonth_log);
                                        }else{
                                            ftiparam.setLog_table(log_table);
                                        }

                                        ftiparam.setFt_image_code(res.get("code"));
                                        ftiparam.setImg_err_msg(res.get("message"));
                                        ftRequestService.updateFTImageFail(ftiparam);
                                    }
                                } else {
                                    log.error("친구톡 이미지 등록 실패 통신오류 : "+response.getBody());
                                    ftRequestService.updateFTImageUploadFail(ftiparam);
                                }

                            }catch (Exception e){
                                log.error("KF Image URL 등록 오류: ", e.getMessage());
                            }
                        }


                    }

                }catch (Exception e) {
                    log.error("KF Image 등록 오류 : " + e.toString());
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
                        log.error("KF 메세지 전송 오류 : " + e.toString());
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
                sendBean.setCurrencytype(ftDataBean.getCurrencytype());

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

                JsonStatus ftBtn = isValidJson(ftDataBean.getAttbutton());
                if (ftBtn == JsonStatus.VALID) {
                    attNode.set("button", mapper.readTree(ftDataBean.getAttbutton()));
                } else if (ftBtn == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (button) msgid={}", ftDataBean.getMsgid());
                    invalidList.add(ftDataBean.getMsgid());
                    continue;
                }

                JsonStatus stCoupon = isValidJson(ftDataBean.getAttcoupon());
                if (stCoupon == JsonStatus.VALID) {
                    attNode.set("coupon", mapper.readTree(ftDataBean.getAttcoupon()));
                } else if (stCoupon == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (coupon) msgid={}", ftDataBean.getMsgid());
                    invalidList.add(ftDataBean.getMsgid());
                    continue;
                }

                if (attNode.size() > 0) {
                    sendBean.setAttachments(mapper.writeValueAsString(attNode)); // String
                }


                sendList.add(sendBean);
            }

            if (!invalidList.isEmpty()) {
                try {
                    Msg_Log ml = new Msg_Log();
                    ml.setMsg_table(param.getMsg_table());
                    ml.setDatabase(param.getDatabase());

                    if(log_back.equalsIgnoreCase("Y")){
                        LocalDate now = LocalDate.now();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
                        String currentMonth = now.format(formatter);

                        ml.setLog_table(log_table+"_"+currentMonth);
                    }else{
                        ml.setLog_table(log_table);
                    }

                    ml.setStatus("4");
                    ml.setResult_message("(AGENT) JSON/ARRAY 데이터 형식 오류");
                    ml.setCode("7999");

                    ftRequestService.updateFTInvalidData(invalidList, ml);
                    log.info("KF Invalid 데이터 {}건 처리 완료", invalidList.size());
                } catch (Exception e) {
                    log.error("KF Invalid 데이터 처리 오류: {}", e.getMessage());
                }
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
                        log.info("KF 메세지 전송 완료 : " + response.getStatusCode() + " / " + group_no + " / " + _list.size() + " 건");
                    } else { // API 전송 실패시
                        log.error("({}) KF 메세지 전송오류 : {}",res.get("userid"), res.get("message"));
                        ftRequestService.updateFTSendInit(sendParam);
                    }
                } catch (Exception e) {
                    log.error("KF 메세지 전송 오류 : " + e.toString());
                    ftRequestService.updateFTSendInit(sendParam);
                }
            }
        }catch (Exception e){
            log.error("KF 메세지 전송 오류 : " + e.toString());
        }
    }

    private JsonStatus isValidJson(String str) {
        if (str == null || str.trim().isEmpty()) return JsonStatus.EMPTY;

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(str);

            if (node.isArray()) {
                return node.size() > 0 ? JsonStatus.VALID : JsonStatus.EMPTY;
            }
            if (node.isObject()) {
                return node.fieldNames().hasNext() ? JsonStatus.VALID : JsonStatus.EMPTY;
            }
            return JsonStatus.INVALID;

        } catch (Exception e) {
            return JsonStatus.INVALID;
        }
    }


}
