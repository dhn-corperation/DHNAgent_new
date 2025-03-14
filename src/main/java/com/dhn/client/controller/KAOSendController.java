package com.dhn.client.controller;

import com.dhn.client.bean.SQLParameter;
import com.dhn.client.bean.SendData;
import com.dhn.client.service.RequestService;
import com.dhn.client.service.WebSocketManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class KAOSendController implements ApplicationListener<ContextRefreshedEvent> {

    public static boolean isStart = false;
    private boolean isProc = false;
    private SQLParameter param = new SQLParameter();
    private String dhnServer;
    private String userid;
    private String preSendGroup = "";

    @Autowired
    private RequestService requestService;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    ScheduledAnnotationBeanPostProcessor posts;

    @Autowired
    WebSocketManager webSocketManager;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        param.setMsg_table(appContext.getEnvironment().getProperty("dhnclient.msg_table"));
        param.setLog_table(appContext.getEnvironment().getProperty("dhnclient.log_table"));
        param.setKakao_use(appContext.getEnvironment().getProperty("dhnclient.kakao_use"));
        userid = appContext.getEnvironment().getProperty("dhnclient.userid");
        param.setMsg_type("AT");

        if (param.getKakao_use() != null && param.getKakao_use().equalsIgnoreCase("Y")) {

            try{
                webSocketManager.connect();
                isStart = true;
                log.info("KAO 초기화 완료");
            }catch (Exception e){
                log.error("연결 실패 : {}",e.getMessage());
            }
        } else {
            posts.postProcessBeforeDestruction(this, null);
        }

    }

    @Scheduled(fixedDelay = 3000)
    public void SendProcess(){
        if (isStart && !isProc) {
            isProc = true;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
            LocalDateTime now = LocalDateTime.now();
            String send_group = "AT"+now.format(formatter);

            if(!send_group.equals(preSendGroup)) {
                if (!webSocketManager.isConnected()) {  // ✅ WebSocket 연결 확인
                    log.error("서버와 연결되지 않음. 메시지 전송 스킵.");
                } else {

                    try{
                        int cnt = requestService.kaoSendDataCount(param);

                        if(cnt > 0){
                            preSendGroup = send_group;
                            param.setSend_group(send_group);
                            requestService.kaoGroupUpdate(param);

                            List<SendData> dataList = requestService.kaoSendDataList(param);
                            log.info("KAO {} 건 발송 시작",dataList.size());

                            try {
                                // ✅ WebSocket을 통해 JSON 데이터 전송
                                String response = webSocketManager.sendBatchMessageSync(send_group, dataList, param.getMsg_type());

                                log.info("KAO 응답 도착: {}", response);

                                // ✅ 응답 처리
                                ObjectMapper objectMapper = new ObjectMapper();
                                Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);

                                // ✅ 해당 group_no의 데이터만 업데이트
//                                requestService.updateMessageStatus(group_no, status);
//                                log.info("KAO group_no {} 상태 업데이트 완료: {}", group_no, status);

                                if(responseMap.get("code").equals("00")){
                                    log.info("KAO 메세지 전송 완료( {} ) : {} 건 ( {} )",send_group, dataList.size(),responseMap.get("code"));
                                    requestService.kaoSendSuccess(param);
                                }else{
                                    log.error("KAO 메세지 전송 오류(Data ERR) : " + responseMap.get("code") + " / " + responseMap.get("message"));
                                    param.setMessage(responseMap.get("message").toString());
                                    requestService.kaoSendFail(param);
                                }

                            } catch (Exception e) {
                                log.error("KAO 메세지 전송 오류(Response) : " + e.getMessage());
                                requestService.kaoSendRetry(param);
                            }
                        }
                    }catch (Exception e){
                        log.error("KAO 메세지 전송 오류(Send) : " + e.toString());
                    }
                }
                preSendGroup = send_group;
            }
            isProc = false;
        }
    }




}
