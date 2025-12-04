package com.dhn.client.controller;

import com.dhn.client.bean.*;
import com.dhn.client.service.BMRequestService;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
public class BMDSendRequest implements ApplicationListener<ContextRefreshedEvent> {

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
        param.setMsg_type("D%");

        dhnServer = appContext.getEnvironment().getProperty("dhnclient.server");
        userid = appContext.getEnvironment().getProperty("dhnclient.userid");
        log_back = appContext.getEnvironment().getProperty("dhnclient.log_back","Y");
        log_table = appContext.getEnvironment().getProperty("dhnclient.log_table");


        if (param.getBrand_use() != null && param.getBrand_use().equalsIgnoreCase("Y")) {
            isStart = true;
            log.info("브랜드메시지 동보형 초기화 완료");
        } else {
            posts.postProcessBeforeDestruction(this, null);
        }

    }

    @Scheduled(fixedDelay = 1000)
    public void SendProcess() {
        if(isStart && !isProc) {
            isProc = true;

            ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor) executorService;
            int activeThreads = poolExecutor.getActiveCount();

            if(activeThreads < 2){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
                LocalDateTime now = LocalDateTime.now();
                String group_no = "BD" + now.format(formatter);

                if(!group_no.equals(preGroupNo)) {
                    try{
                        int cnt = bmRequestService.selectBDRequestCount(param);

                        if(cnt > 0){
                            param.setGroup_no(group_no);
                            bmRequestService.updateBDGroupNo(param);

                            executorService.submit(() -> APIProcess(group_no));
                        }

                    }catch (Exception e){
                        log.error("BD (동보형) 메세지 전송 오류 : " + e.toString());
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


            List<BMRequestBean> _list = bmRequestService.selectBDRequests(sendParam);
            List<BMRequestBean> sendList = new ArrayList<>();
            List<String> invalidList = new ArrayList<>();
            List<String> retryList = new ArrayList<>();

            RestTemplate rt = new RestTemplate();
            ObjectMapper om = new ObjectMapper();

            for (BMRequestBean data : _list) {
                // 예상모수 조회 API 필요 일단 POST 방식 제작

                String senderKey   = data.getProfile();
                String friendGroup = data.getAttitems();
                String chatBubble  = "";

                switch (data.getMessagetype().toUpperCase()) {
                    case "D1":
                        chatBubble = "TEXT";
                        break;
                    case "D2":
                        chatBubble = "IMAGE";
                        break;
                    case "D3":
                        chatBubble = "WIDE";
                        break;
                    case "D4":
                        chatBubble = "WIDE_ITEM_LIST";
                        break;
                    case "D5":
                        chatBubble = "CAROUSEL_FEED";
                        break;
                    case "D6":
                        chatBubble = "PREMIUM_VIDEO";
                        break;
                    case "D7":
                        chatBubble = "COMMERCE";
                        break;
                    case "D8":
                        chatBubble = "CAROUSEL_COMMERCE";
                        break;
                    default:
                        chatBubble = "";
                        break;
                }

                if (senderKey == null || senderKey.isEmpty()) {
                    invalidList.add(data.getMsgid());
                    continue;
                }

                if (chatBubble.isEmpty()) {
                    invalidList.add(data.getMsgid());
                    continue;
                }

                UriComponentsBuilder builder = UriComponentsBuilder
                        .fromHttpUrl(dhnServer+"bm/broadcast/possible")
                        .queryParam("senderKey", senderKey)
                        .queryParam("chatBubbleType", chatBubble);

                if (friendGroup != null && !friendGroup.isEmpty()) {
                    builder.queryParam("friendGroupKey", friendGroup);
                }

                String url = builder.toUriString();

                try {

                    HttpHeaders headers = new HttpHeaders();
                    headers.set("userid", userid);

                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    ResponseEntity<String> response = rt.exchange(url, HttpMethod.GET, entity, String.class);

                    if (response.getStatusCode() != HttpStatus.OK) {
                        log.error("HTTP 통신 실패: {}", response.getStatusCode());
                        retryList.add(data.getMsgid());
                        continue;
                    }

                    Map<String, Object> result = om.readValue(response.getBody(), Map.class);
                    String code = result.get("code").toString();

                    if ("200".equals(code)) {
                        Map<String, Object> dataMap = (Map<String, Object>) result.get("data");
                        Object possibleObj = dataMap.get("possible");

                        int possible = 0;
                        if (possibleObj instanceof Number) {
                            possible = ((Number) possibleObj).intValue();
                        } else if (possibleObj != null) {
                            possible = Integer.parseInt(possibleObj.toString());
                        }

                        data.setExpectedbroadcastcnt(possible);
                        sendList.add(data);

                        log.info("BD (동보형) 예상 모수 : {}", possible);
                    } else {
                        log.error("BD (동보형) 예상 모수 조회 실패 : {} / {}", code, result.get("message"));
                        try {
                            Msg_Log ml = new Msg_Log();
                            ml.setMsg_table(param.getMsg_table());
                            ml.setDatabase(param.getDatabase());
                            ml.setMsgid(data.getMsgid());

                            if(log_back.equalsIgnoreCase("Y")){
                                LocalDate now = LocalDate.now();
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
                                String currentMonth = now.format(formatter);

                                ml.setLog_table(log_table+"_"+currentMonth);
                            }else{
                                ml.setLog_table(log_table);
                            }

                            ml.setStatus("4");
                            ml.setResult_message(result.get("message").toString());
                            ml.setCode(code);

                            bmRequestService.updateExpectedFail(ml);
                        } catch (Exception e) {
                            log.error("BD (동보형) 예상 모수 조회 실패 후처리 실패: {}", e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.error("BD (동보형) 예상 모수 조회 통신 실패 : {}", e.toString());
                    retryList.add(data.getMsgid());
                }
            }

            if (!retryList.isEmpty()) {
                try {
                    Msg_Log ml = new Msg_Log();
                    ml.setMsg_table(param.getMsg_table());
                    ml.setDatabase(param.getDatabase());

                    bmRequestService.retryBmData(retryList, ml);
                    log.info("BD (동보형) 모수조회 실패 재처리 {}건 처리 완료", retryList.size());
                } catch (Exception e) {
                    log.error("BD (동보형) 모수조회 실패 재처리 오류: {}", e.getMessage());
                    Thread.sleep(5000);
                }
            }

            if (!sendList.isEmpty()) {
                StringWriter sw = new StringWriter();
                om = new ObjectMapper();
                om.writeValue(sw, sendList);

                HttpHeaders header = new HttpHeaders();

                header.setContentType(MediaType.APPLICATION_JSON);
                header.set("userid", userid);

                rt = new RestTemplate();
                HttpEntity<String> entity = new HttpEntity<String>(sw.toString(), header);

                try {
                    ResponseEntity<String> response = rt.postForEntity(dhnServer + "req", entity, String.class);
                    Map<String, String> res = om.readValue(response.getBody().toString(), Map.class);
                    log.info(res.toString());
                    if (response.getStatusCode() == HttpStatus.OK) {
                        bmRequestService.updateBMSendComplete(sendParam);
                        log.info("BD (동보형) 메세지 전송 완료 : " + response.getStatusCode() + " / " + group_no + " / " + _list.size() + " 건");
                    }else {
                        log.error("({}) BD (동보형) 메세지 전송오류 : {}",res.get("userid"), res.get("message"));
                        bmRequestService.updateBMSendInit(sendParam);
                    }
                } catch (Exception e) {
                    log.error("BD (동보형) 메세지 전송 오류 : " + e.toString());
                    bmRequestService.updateBMSendInit(sendParam);
                }
            }

        }catch (Exception e){
            log.error("BD (동보형) 메세지 전송 오류 : " + e.toString());
        }
    }

}
