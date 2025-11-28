package com.dhn.client.controller;

import com.dhn.client.bean.*;
import com.dhn.client.service.BMRequestService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        param.setBrand_use(appContext.getEnvironment().getProperty("dhnclient.upbrand_use"));
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

                            log.info("test : {}",bmimage.toString());
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

                                body.add("image", new org.springframework.core.io.FileSystemResource(file));
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

                                        bmiparam.setFt_image_url(res.get("image"));
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

    @Scheduled(fixedDelay = 100)
    public void SendProcess() {
        if(isStart && !isProc) {
            isProc = true;

            ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor) executorService;
            int activeThreads = poolExecutor.getActiveCount();

            if(activeThreads < 2){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
                LocalDateTime now = LocalDateTime.now();
                String group_no = "IBM" + now.format(formatter);

                if(!group_no.equals(preGroupNo)) {
                    try{
                        int cnt = bmRequestService.selectIBMRequestCount(param);

                        if(cnt > 0){
                            param.setGroup_no(group_no);
                            bmRequestService.updateIBMGroupNo(param);

                            executorService.submit(() -> APIProcess(group_no));
                        }

                    }catch (Exception e){
                        log.error("IBM (파일업로드) 메세지 전송 오류 : " + e.toString());
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


            List<BMDataBean> _list = bmRequestService.selectIBMRequests(sendParam);

            List<BMRequestBean> sendList = new ArrayList<>();
            List<String> invalidList = new ArrayList<>();

            for (BMDataBean bmDataBean  : _list ) {
                BMRequestBean sendBean = new BMRequestBean();
                sendBean.setMsgid(bmDataBean.getMsgid());
                sendBean.setPushalarm(bmDataBean.getPushalarm());
                sendBean.setMessagetype("B1");
                sendBean.setMsg(bmDataBean.getMsg());
                sendBean.setMsgsms(bmDataBean.getMsgsms());
                sendBean.setPcom("P");
                sendBean.setPinvoice(bmDataBean.getPinvoice());
                sendBean.setPhn(bmDataBean.getPhn());
                sendBean.setProfile(bmDataBean.getProfile());
                sendBean.setRegdt(bmDataBean.getRegdt());
                sendBean.setReservedt(bmDataBean.getReservedt());
                sendBean.setSmskind(bmDataBean.getSmskind());
                sendBean.setSmslmstit(bmDataBean.getSmslmstit());
                sendBean.setSmssender(bmDataBean.getSmssender());
                sendBean.setTmplid(bmDataBean.getTmplid());
                sendBean.setCurrencytype(bmDataBean.getCurrencytype());
                sendBean.setHeader(bmDataBean.getHeader());
                sendBean.setKisacode(bmDataBean.getKisacode());
                sendBean.setKind(bmDataBean.getKind());
                sendBean.setSupplement(bmDataBean.getSupplement());
                sendBean.setAttitems(bmDataBean.getGrouptag());

                ObjectMapper mapper = new ObjectMapper();

                ObjectNode attNode = mapper.createObjectNode();

                if(bmDataBean.getImageurl() != null && !bmDataBean.getImageurl().trim().isEmpty()){
                    ObjectNode imageNode = mapper.createObjectNode();
                    imageNode.put("img_url", bmDataBean.getImageurl());

                    if(bmDataBean.getImagelink() != null && !bmDataBean.getImagelink().trim().isEmpty()){
                        imageNode.put("img_link", bmDataBean.getImagelink());
                    }

                    if ("Y".equals(bmDataBean.getWide())) {
                        sendBean.setMessagetype("B3");
                    } else {
                        sendBean.setMessagetype("B2");
                    }

                    attNode.set("image", imageNode);
                }

                JsonStatus stBtn = isValidJson(bmDataBean.getAttbutton());
                if (stBtn == JsonStatus.VALID) {
                    attNode.set("button", mapper.readTree(bmDataBean.getAttbutton()));
                } else if (stBtn == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (button) msgid={}", bmDataBean.getMsgid());
                    invalidList.add(bmDataBean.getMsgid());
                    continue;
                }

                JsonStatus stCoupon = isValidJson(bmDataBean.getAttcoupon());
                if (stCoupon == JsonStatus.VALID) {
                    attNode.set("coupon", mapper.readTree(bmDataBean.getAttcoupon()));
                } else if (stCoupon == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (coupon) msgid={}", bmDataBean.getMsgid());
                    invalidList.add(bmDataBean.getMsgid());
                    continue;
                }

                if (attNode.size() > 0) {
                    sendBean.setAttachments(mapper.writeValueAsString(attNode));
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

                    bmRequestService.updateInvalidData(invalidList, ml);
                    log.info("IBM (파일업로드) Invalid 데이터 {}건 처리 완료", invalidList.size());
                } catch (Exception e) {
                    log.error("IBM (파일업로드) Invalid 데이터 처리 오류: {}", e.getMessage());
                }
            }

            if (!sendList.isEmpty()) {

                StringWriter sw = new StringWriter();
                ObjectMapper om = new ObjectMapper();
                om.writeValue(sw, sendList);

                HttpHeaders header = new HttpHeaders();

                header.setContentType(MediaType.APPLICATION_JSON);
                header.set("userid", userid);

                RestTemplate rt = new RestTemplate();
                HttpEntity<String> entity = new HttpEntity<String>(sw.toString(), header);

                try {
                    ResponseEntity<String> response = rt.postForEntity(dhnServer + "req", entity, String.class);
                    Map<String, String> res = om.readValue(response.getBody().toString(), Map.class);
                    log.info(res.toString());
                    if (response.getStatusCode() == HttpStatus.OK) {
                        bmRequestService.updateBMSendComplete(sendParam);
                        log.info("IBM (파일업로드) 메세지 전송 완료 : " + response.getStatusCode() + " / " + group_no + " / " + sendList.size() + " 건");
                    }else {
                        log.error("({}) IBM (파일업로드) 메세지 전송오류 : {}",res.get("userid"), res.get("message"));
                        bmRequestService.updateBMSendInit(sendParam);
                    }
                } catch (Exception e) {
                    log.error("IBM (파일업로드) 메세지 전송 오류 : " + e.toString());
                    bmRequestService.updateBMSendInit(sendParam);
                }

            }
        }catch (Exception e){
            log.error("IBM (파일업로드) 메세지 전송 오류 : " + e.toString());
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
