package com.dhn.client.controller;

import com.dhn.client.bean.*;
import com.dhn.client.service.BMRequestService;
import com.dhn.client.service.FTRequestService;
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
import org.springframework.web.client.RestTemplate;

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
public class FTSendRequest implements ApplicationListener<ContextRefreshedEvent> {

    public static boolean isStart = false;
    private boolean isProc = false;
    private SQLParameter param = new SQLParameter();
    private String dhnServer;
    private String userid;
    private String preGroupNo = "";
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
        param.setMsg_type("F%");

        dhnServer = appContext.getEnvironment().getProperty("dhnclient.server");
        userid = appContext.getEnvironment().getProperty("dhnclient.userid");
        log_back = appContext.getEnvironment().getProperty("dhnclient.log_back","Y");
        log_table = appContext.getEnvironment().getProperty("dhnclient.log_table");


        if (param.getKakao_use() != null && param.getKakao_use().equalsIgnoreCase("Y")) {
            isStart = true;
            log.info("FT (친구톡) 초기화 완료");
        } else {
            posts.postProcessBeforeDestruction(this, null);
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
                String group_no = "FT" + now.format(formatter);

                if(!group_no.equals(preGroupNo)) {
                    try{
                        int cnt = ftRequestService.selectFTRequestCount(param);

                        if(cnt > 0){
                            param.setGroup_no(group_no);
                            ftRequestService.updateFTGroupNo(param);

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


            List<FTDataBean> _list = ftRequestService.selectFTRequests(sendParam);

            List<FTRequestBean> sendList = new ArrayList<>();
            List<String> invalidList = new ArrayList<>();

            for (FTDataBean ftDataBean  : _list ) {
                FTRequestBean sendBean = new FTRequestBean();
                sendBean.setMsgid(ftDataBean.getMsgid());
                sendBean.setAdflag(ftDataBean.getAdflag());
                sendBean.setMessagetype(ftDataBean.getMessagetype());
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
                sendBean.setTmplid(ftDataBean.getTmplid());
                sendBean.setCurrencytype(ftDataBean.getCurrencytype());
                sendBean.setHeader(ftDataBean.getHeader());
                sendBean.setKisacode(ftDataBean.getKisacode());
                sendBean.setKind(ftDataBean.getKind());
                sendBean.setSupplement(ftDataBean.getSupplement());
                sendBean.setAttitems(ftDataBean.getGrouptag());
                sendBean.setUserkey(ftDataBean.getUserkey());
                sendBean.setPushalarm(ftDataBean.getPushalarm());

                ObjectMapper mapper = new ObjectMapper();

                ObjectNode attNode = mapper.createObjectNode();

                JsonStatus stImg = isValidJson(ftDataBean.getAttimage());
                if (stImg == JsonStatus.VALID) {
                    attNode.set("image", mapper.readTree(ftDataBean.getAttimage()));
                } else if (stImg == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (image) msgid={}", ftDataBean.getMsgid());
                    invalidList.add(ftDataBean.getMsgid());
                    continue;
                }

                JsonStatus stBtn = isValidJson(ftDataBean.getAttbutton());
                if (stBtn == JsonStatus.VALID) {
                    attNode.set("button", mapper.readTree(ftDataBean.getAttbutton()));
                } else if (stBtn == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (button) msgid={}", ftDataBean.getMsgid());
                    invalidList.add(ftDataBean.getMsgid());
                    continue;
                }

                JsonStatus stItem = isValidJson(ftDataBean.getAttitem());
                if (stItem == JsonStatus.VALID) {
                    attNode.set("item", mapper.readTree(ftDataBean.getAttitem()));
                } else if (stItem == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (item) msgid={}", ftDataBean.getMsgid());
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

                JsonStatus stCommerce = isValidJson(ftDataBean.getAttcommerce());
                if (stCommerce == JsonStatus.VALID) {
                    attNode.set("commerce", mapper.readTree(ftDataBean.getAttcommerce()));
                } else if (stCommerce == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (commerce) msgid={}", ftDataBean.getMsgid());
                    invalidList.add(ftDataBean.getMsgid());
                    continue;
                }

                JsonStatus stVideo = isValidJson(ftDataBean.getAttvideo());
                if (stVideo == JsonStatus.VALID) {
                    attNode.set("video", mapper.readTree(ftDataBean.getAttvideo()));
                } else if (stVideo == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (video) msgid={}", ftDataBean.getMsgid());
                    invalidList.add(ftDataBean.getMsgid());
                    continue;
                }

                if (attNode.size() > 0) {
                    sendBean.setAttachments(mapper.writeValueAsString(attNode)); // String
                }

                // ===== carousel 조립 =====
                ObjectNode carNode = mapper.createObjectNode();

                JsonStatus stHead = isValidJson(ftDataBean.getCarhead());
                if (stHead == JsonStatus.VALID) {
                    carNode.set("head", mapper.readTree(ftDataBean.getCarhead()));
                } else if (stHead == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (carhead) msgid={}", ftDataBean.getMsgid());
                    invalidList.add(ftDataBean.getMsgid());
                    continue;
                }

                JsonStatus stList = isValidJson(ftDataBean.getCarlist());
                if (stList == JsonStatus.VALID) {
                    carNode.set("list", mapper.readTree(ftDataBean.getCarlist()));
                } else if (stList == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (carlist) msgid={}", ftDataBean.getMsgid());
                    invalidList.add(ftDataBean.getMsgid());
                    continue;
                }

                JsonStatus stTail = isValidJson(ftDataBean.getCartail());
                if (stTail == JsonStatus.VALID) {
                    carNode.set("tail", mapper.readTree(ftDataBean.getCartail()));
                } else if (stTail == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (cartail) msgid={}", ftDataBean.getMsgid());
                    invalidList.add(ftDataBean.getMsgid());
                    continue;
                }

                if (carNode.size() > 0) {
                    sendBean.setCarousel(mapper.writeValueAsString(carNode)); // String
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
                    log.info("FT Invalid 데이터 {}건 처리 완료", invalidList.size());
                } catch (Exception e) {
                    log.error("FT Invalid 데이터 처리 오류: {}", e.getMessage());
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
                        ftRequestService.updateFTSendComplete(sendParam);
                        log.info("FT 메세지 전송 완료 : " + response.getStatusCode() + " / " + group_no + " / " + sendList.size() + " 건");
                    }else {
                        log.error("({}) FT 메세지 전송오류 : {}",res.get("userid"), res.get("message"));
                        Thread.sleep(30000);
                        ftRequestService.updateFTSendInit(sendParam);
                    }
                } catch (Exception e) {
                    log.error("FT 메세지 전송 오류 : " + e.toString());
                    Thread.sleep(30000);
                    ftRequestService.updateFTSendInit(sendParam);
                }

            }
        }catch (Exception e){
            log.error("FT 메세지 전송 오류 : " + e.toString());
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
