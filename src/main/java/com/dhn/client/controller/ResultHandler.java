package com.dhn.client.controller;

import com.dhn.client.bean.Msg_Log;
import com.dhn.client.service.RequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class ResultHandler implements ApplicationListener<ContextRefreshedEvent> {

    public static boolean isStart = false;
    private boolean isProc = false;
    //private SQLParameter param = new SQLParameter();
    private String dhnServer;
    private String userid;
    private Map<String, String> _kaoCode = new HashMap<>();
    private static int procCnt = 0;
    private String msg_table = "";
    private String log_table = "";
//    private String database = "";
//    private String log_back = "";

    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Autowired
    private RequestService requestService;

    @Autowired
    private ApplicationContext appContext;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        msg_table = appContext.getEnvironment().getProperty("dhnclient.msg_table");
        log_table = appContext.getEnvironment().getProperty("dhnclient.log_table");
//        database = appContext.getEnvironment().getProperty("dhnclient.database");
//        log_back = appContext.getEnvironment().getProperty("dhnclient.log_back","Y");

        dhnServer = appContext.getEnvironment().getProperty("dhnclient.server");
        userid = appContext.getEnvironment().getProperty("dhnclient.userid");

        isStart = true;
    }

    public void processFinalResult(List<Map<String, Object>> detailList) {
        executorService.submit(() -> doProcessFinalResult(detailList));
    }

    private void doProcessFinalResult(List<Map<String, Object>> detailList) {

        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));


        for (Map<String, Object> ent : detailList) {
            try {
                Msg_Log kao_ml = new Msg_Log();
                Msg_Log msg_ml = new Msg_Log();

                String messageType = (String) ent.get("message_type");
                String resultMessage = ((String) ent.getOrDefault("message", "")).trim();
                String msgid = (String) ent.get("msgid");

                if (messageType != null &&
                        (messageType.equalsIgnoreCase("AT") ||
                                messageType.equalsIgnoreCase("AI") ||
                                messageType.equalsIgnoreCase("FI") ||
                                messageType.equalsIgnoreCase("FT"))) {

                    // 알림톡
                    kao_ml.setMsgid(msgid);
                    kao_ml.setMsg_table(msg_table);
//                    kao_ml.setDatabase(database);
//                    kao_ml.setLog_table(log_back.equalsIgnoreCase("Y") ? log_table + "_" + currentMonth : log_table);
                    kao_ml.setLog_table(log_table + "_" + currentMonth);

                    kao_ml.setResult_dt((String) ent.get("res_dt"));
                    kao_ml.setS_code((String) ent.get("s_code"));
                    kao_ml.setResult_message(resultMessage);

                    if ("0000".equals(ent.get("s_code"))) {
                        kao_ml.setStatus("3");
                    } else {
                        kao_ml.setStatus("4");
                    }

                } else if ("PH".equalsIgnoreCase(messageType)
                        && ent.containsKey("s_code")
                        && ent.get("s_code") != null
                        && ((String) ent.get("s_code")).length() > 1) {

                    // 알림톡 실패 문자
                    kao_ml.setMsgid(msgid);
                    kao_ml.setMsg_table(msg_table);
//                    kao_ml.setDatabase(database);
//                    kao_ml.setLog_table(log_back.equalsIgnoreCase("Y") ? log_table + "_" + currentMonth : log_table);
                    kao_ml.setLog_table(log_table + "_" + currentMonth);

                    kao_ml.setS_code((String) ent.get("s_code"));
                    kao_ml.setCode((String) ent.get("code"));
                    kao_ml.setResult_dt((String) ent.get("remark2"));
                    kao_ml.setResult_message(resultMessage);
                    kao_ml.setReal_send_type((String) ent.get("sms_kind"));

                    String remark1 = (String) ent.get("remark1");
                    if ("LGT".equalsIgnoreCase(remark1) || "019".equals(remark1)) {
                        kao_ml.setTelecom("LGT");
                    } else if ("SKT".equalsIgnoreCase(remark1) || "011".equals(remark1)) {
                        kao_ml.setTelecom("SKT");
                    } else if ("KTF".equalsIgnoreCase(remark1) || "KT".equalsIgnoreCase(remark1) || "016".equals(remark1)) {
                        kao_ml.setTelecom("KTF");
                    } else {
                        kao_ml.setTelecom("ETC");
                    }

                    if ("0000".equals(ent.get("code"))) {
                        kao_ml.setStatus("3");
                    } else {
                        kao_ml.setStatus("4");
                    }

                } else {
                    // 문자
                    msg_ml.setMsgid(msgid);
                    msg_ml.setMsg_table(msg_table);
//                    msg_ml.setDatabase(database);
//                    kao_ml.setLog_table(log_back.equalsIgnoreCase("Y") ? log_table + "_" + currentMonth : log_table);
                    kao_ml.setLog_table(log_table + "_" + currentMonth);

                    msg_ml.setCode((String) ent.get("code"));
                    msg_ml.setReal_send_type((String) ent.get("sms_kind"));
                    msg_ml.setResult_dt((String) ent.get("remark2"));
                    msg_ml.setResult_message(resultMessage);

                    String remark1 = (String) ent.get("remark1");
                    if ("LGT".equalsIgnoreCase(remark1) || "019".equals(remark1)) {
                        msg_ml.setTelecom("LGT");
                    } else if ("SKT".equalsIgnoreCase(remark1) || "011".equals(remark1)) {
                        msg_ml.setTelecom("SKT");
                    } else if ("KTF".equalsIgnoreCase(remark1) || "KT".equalsIgnoreCase(remark1) || "016".equals(remark1)) {
                        msg_ml.setTelecom("KTF");
                    } else {
                        msg_ml.setTelecom("ETC");
                    }

                    if ("0000".equals(ent.get("code"))) {
                        msg_ml.setStatus("3");
                    } else {
                        msg_ml.setStatus("4");
                    }
                }

                if (msg_ml.getMsg_table() != null && msg_ml.getLog_table() != null) {
                    requestService.msgResultInsert(msg_ml);
                } else if (kao_ml.getMsg_table() != null && kao_ml.getLog_table() != null) {
                    requestService.kaoResultInsert(kao_ml);
                }

            } catch (Exception e) {
                log.error("WebSocket 결과 처리 오류: {}", e.getMessage(), e);
            }
        }

        log.info("WebSocket 결과 처리 완료: {}건", detailList.size());
    }


}
