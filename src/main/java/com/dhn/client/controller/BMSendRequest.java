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
        param.setSequence(appContext.getEnvironment().getProperty("dhnclient.at_seq"));
        param.setMsg_type("B%");

        dhnServer = appContext.getEnvironment().getProperty("dhnclient.server");
        userid = appContext.getEnvironment().getProperty("dhnclient.userid");

        if (param.getBrand_use() != null && param.getBrand_use().equalsIgnoreCase("Y")) {
            isStart = true;
            log.info("BM 초기화 완료");
        } else {
            posts.postProcessBeforeDestruction(this, null);
        }

    }

    @Scheduled(fixedDelay = 2000)
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

                ObjectMapper mapper = new ObjectMapper();

                ObjectNode attNode = mapper.createObjectNode();

                if (isValidJson(bmDataBean.getAttimage())) {
                    attNode.set("image", mapper.readTree(bmDataBean.getAttimage()));
                }
                if (isValidJson(bmDataBean.getAttbutton())) {
                    attNode.set("button", mapper.readTree(bmDataBean.getAttbutton()));
                }
                if (isValidJson(bmDataBean.getAttitem())) {
                    attNode.set("item", mapper.readTree(bmDataBean.getAttitem()));
                }
                if (isValidJson(bmDataBean.getAttcoupon())) {
                    attNode.set("coupon", mapper.readTree(bmDataBean.getAttcoupon()));
                }
                if (isValidJson(bmDataBean.getAttcommerce())) {
                    attNode.set("commerce", mapper.readTree(bmDataBean.getAttcommerce()));
                }
                if (isValidJson(bmDataBean.getAttvideo())) {
                    attNode.set("video", mapper.readTree(bmDataBean.getAttvideo()));
                }

                if (attNode.size() > 0) {
                    Attachments attObj = mapper.treeToValue(attNode, Attachments.class);
                    sendBean.setAttachments(attObj);
                }

                // ===== carousel 조립 =====
                ObjectNode carNode = mapper.createObjectNode();

                if (isValidJson(bmDataBean.getCarhead())) {
                    carNode.set("head", mapper.readTree(bmDataBean.getCarhead()));
                }
                if (isValidJson(bmDataBean.getCarlist())) {
                    carNode.set("list", mapper.readTree(bmDataBean.getCarlist())); // 배열 []
                }
                if (isValidJson(bmDataBean.getCartail())) {
                    carNode.set("tail", mapper.readTree(bmDataBean.getCartail()));
                }

                if (carNode.size() > 0) {
                    Carousel carObj = mapper.treeToValue(carNode, Carousel.class);
                    sendBean.setCarousel(carObj);
                }

                sendList.add(sendBean);
            }



            /*
            StringWriter sw = new StringWriter();
            ObjectMapper om = new ObjectMapper();
            om.writeValue(sw, _list); // List를 Json화 하여 문자열 저장

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
                    bmRequestService.updateKAOSendComplete(sendParam);
                    log.info("BM 메세지 전송 완료 : " + response.getStatusCode() + " / " + group_no + " / " + _list.size() + " 건");
                }else { // API 전송 실패시
                    log.info("({}) BM 메세지 전송오류 : {}",res.get("userid"), res.get("message"));
                    bmRequestService.updateKAOSendInit(sendParam);
                }
            } catch (Exception e) {
                log.error("BM 메세지 전송 오류 : " + e.toString());
                bmRequestService.updateKAOSendInit(sendParam);
            }

             */
        }catch (Exception e){
            log.error("BM 메세지 전송 오류 : " + e.toString());
        }
    }

    private boolean isValidJson(String str) {
        if (str == null || str.trim().isEmpty()) return false;

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(str);

            if (node.isArray()) {
                return node.size() > 0;
            }

            if (node.isObject()) {
                return node.fieldNames().hasNext();
            }
            return false;

        } catch (Exception e) {
            return false;
        }
    }

}
