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
public class BMSendRequest implements ApplicationListener<ContextRefreshedEvent> {

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
        param.setMsg_type("B%");

        dhnServer = appContext.getEnvironment().getProperty("dhnclient.server");
        userid = appContext.getEnvironment().getProperty("dhnclient.userid");
        log_back = appContext.getEnvironment().getProperty("dhnclient.log_back","Y");
        log_table = appContext.getEnvironment().getProperty("dhnclient.log_table");


        if (param.getBrand_use() != null && param.getBrand_use().equalsIgnoreCase("Y")) {
            isStart = true;
            log.info("BM 초기화 완료");
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
                String group_no = "BM" + now.format(formatter);

                if(!group_no.equals(preGroupNo)) {
                    try{
                        int cnt = bmRequestService.selectBMRequestCount(param);

                        if(cnt > 0){
                            param.setGroup_no(group_no);
                            bmRequestService.updateBMGroupNo(param);

                            executorService.submit(() -> APIProcess(group_no));
                        }

                    }catch (Exception e){
                        log.error("BM 메세지 전송 오류 : " + e.toString());
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


            List<BMDataBean> _list = bmRequestService.selectBMRequests(sendParam);

            List<BMRequestBean> sendList = new ArrayList<>();
            List<String> invalidList = new ArrayList<>();

            for (BMDataBean bmDataBean  : _list ) {
                BMRequestBean sendBean = new BMRequestBean();
                sendBean.setMsgid(bmDataBean.getMsgid());
                sendBean.setAdflag(bmDataBean.getAdflag());
                sendBean.setMessagetype(bmDataBean.getMessagetype());
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

                JsonStatus stImg = isValidJson(bmDataBean.getAttimage());
                if (stImg == JsonStatus.VALID) {
                    attNode.set("image", mapper.readTree(bmDataBean.getAttimage()));
                } else if (stImg == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (image) msgid={}", bmDataBean.getMsgid());
                    invalidList.add(bmDataBean.getMsgid());
                    continue;
                }

                JsonStatus stBtn = isValidJson(bmDataBean.getAttbutton());
                if (stBtn == JsonStatus.VALID) {
                    attNode.set("button", mapper.readTree(bmDataBean.getAttbutton()));
                } else if (stBtn == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (button) msgid={}", bmDataBean.getMsgid());
                    invalidList.add(bmDataBean.getMsgid());
                    continue;
                }

                JsonStatus stItem = isValidJson(bmDataBean.getAttitem());
                if (stItem == JsonStatus.VALID) {
                    attNode.set("item", mapper.readTree(bmDataBean.getAttitem()));
                } else if (stItem == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (item) msgid={}", bmDataBean.getMsgid());
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

                JsonStatus stCommerce = isValidJson(bmDataBean.getAttcommerce());
                if (stCommerce == JsonStatus.VALID) {
                    attNode.set("commerce", mapper.readTree(bmDataBean.getAttcommerce()));
                } else if (stCommerce == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (commerce) msgid={}", bmDataBean.getMsgid());
                    invalidList.add(bmDataBean.getMsgid());
                    continue;
                }

                JsonStatus stVideo = isValidJson(bmDataBean.getAttvideo());
                if (stVideo == JsonStatus.VALID) {
                    attNode.set("video", mapper.readTree(bmDataBean.getAttvideo()));
                } else if (stVideo == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (video) msgid={}", bmDataBean.getMsgid());
                    invalidList.add(bmDataBean.getMsgid());
                    continue;
                }

                if (attNode.size() > 0) {
                    sendBean.setAttachments(mapper.writeValueAsString(attNode)); // String
                }

                // ===== carousel 조립 =====
                ObjectNode carNode = mapper.createObjectNode();

                JsonStatus stHead = isValidJson(bmDataBean.getCarhead());
                if (stHead == JsonStatus.VALID) {
                    carNode.set("head", mapper.readTree(bmDataBean.getCarhead()));
                } else if (stHead == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (carhead) msgid={}", bmDataBean.getMsgid());
                    invalidList.add(bmDataBean.getMsgid());
                    continue;
                }

                JsonStatus stList = isValidJson(bmDataBean.getCarlist());
                if (stList == JsonStatus.VALID) {
                    carNode.set("list", mapper.readTree(bmDataBean.getCarlist()));
                } else if (stList == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (carlist) msgid={}", bmDataBean.getMsgid());
                    invalidList.add(bmDataBean.getMsgid());
                    continue;
                }

                JsonStatus stTail = isValidJson(bmDataBean.getCartail());
                if (stTail == JsonStatus.VALID) {
                    carNode.set("tail", mapper.readTree(bmDataBean.getCartail()));
                } else if (stTail == JsonStatus.INVALID) {
                    log.error("Invalid JSON/ARRAY (cartail) msgid={}", bmDataBean.getMsgid());
                    invalidList.add(bmDataBean.getMsgid());
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

                    bmRequestService.updateInvalidData(invalidList, ml);
                    log.info("BM Invalid 데이터 {}건 처리 완료", invalidList.size());
                } catch (Exception e) {
                    log.error("BM Invalid 데이터 처리 오류: {}", e.getMessage());
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
                        log.info("BM 메세지 전송 완료 : " + response.getStatusCode() + " / " + group_no + " / " + sendList.size() + " 건");
                    }else {
                        log.error("({}) BM 메세지 전송오류 : {}",res.get("userid"), res.get("message"));
                        bmRequestService.updateBMSendInit(sendParam);
                    }
                } catch (Exception e) {
                    log.error("BM 메세지 전송 오류 : " + e.toString());
                    bmRequestService.updateBMSendInit(sendParam);
                }

            }
        }catch (Exception e){
            log.error("BM 메세지 전송 오류 : " + e.toString());
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
