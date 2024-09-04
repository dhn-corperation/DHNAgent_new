package com.dhn.client.controller;

import com.dhn.client.service.KAORequestService;
import com.dhn.client.service.MSGRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Order(6)
public class LogTableCreate implements ApplicationListener<ContextRefreshedEvent> {

    public static boolean isStart = false;
    private boolean isProc = false;
    private String atTable = "";
    private String atLogTable = "";
    private String msgTable = "";
    private String msgLogTable = "";
    private String database = "";
    private String kakao_use = "";
    private String sms_use = "";
    private String lms_use = "";
    private String mms_use = "";

    @Autowired
    private KAORequestService kaoRequestService;

    @Autowired
    private MSGRequestService msgRequestService;

    @Autowired
    private ApplicationContext appContext;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        atTable = appContext.getEnvironment().getProperty("dhnclient.at_table");
        atLogTable = appContext.getEnvironment().getProperty("dhnclient.at_log_table");
        msgTable = appContext.getEnvironment().getProperty("dhnclient.msg_table");
        msgLogTable = appContext.getEnvironment().getProperty("dhnclient.msg_log_table");
        database = appContext.getEnvironment().getProperty("dhnclient.database");
        kakao_use = appContext.getEnvironment().getProperty("dhnclient.kakao_use");
        sms_use = appContext.getEnvironment().getProperty("dhnclient.sms_use");
        lms_use = appContext.getEnvironment().getProperty("dhnclient.lms_use");
        mms_use = appContext.getEnvironment().getProperty("dhnclient.mms_use");

        log.info("LOG테이블 자동생성 초기화 완료");

        isStart = true;
    }

    @Scheduled(cron = "0 0 1 L * ?")
    public void createTable() {
        log.info("로그 테이블 로그테이블 재확인 및 생성");
        if(isStart && !isProc){
            isProc = true;
            try{
                if(kakao_use.equalsIgnoreCase("Y")){
                    kaoRequestService.atLogTableCheck(atTable,atLogTable, database);
                }

                if(sms_use.equalsIgnoreCase("Y") || lms_use.equalsIgnoreCase("Y") || mms_use.equalsIgnoreCase("Y")){
                    msgRequestService.msgLogTableCheck(msgTable, msgLogTable, database);
                }
            }catch (Exception e){
                log.error("log 테이블 생성 오류 : "+e.getMessage());
            }
            isProc = false;
        }
    }

}
