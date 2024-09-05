package com.dhn.client.controller;

import com.dhn.client.bean.KAORequestBean;
import com.dhn.client.bean.SQLParameter;
import com.dhn.client.service.KAORequestService;
import com.dhn.client.service.KAOService;
import com.dhn.client.service.SendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
@Order(6)
public class FTSendRequest implements ApplicationListener<ContextRefreshedEvent> {
    public static boolean isStart = false;
    private boolean isProc = false;
    private SQLParameter param = new SQLParameter();
    private String dhnServer;
    private String userid;
    private String preGroupNo = "";

    @Autowired
    private KAORequestService kaoRequestService;

    @Autowired
    private KAOService kaoService;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private SendService sendService;

    @Autowired
    private ScheduledAnnotationBeanPostProcessor posts;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        param.setAt_table(appContext.getEnvironment().getProperty("dhnclient.at_table"));
        param.setFtkao_use(appContext.getEnvironment().getProperty("dhnclient.ftkao_use"));
        param.setDatabase(appContext.getEnvironment().getProperty("dhnclient.database"));
        param.setSequence(appContext.getEnvironment().getProperty("dhnclient.at_seq"));
        param.setMsg_type("FT");

        dhnServer = "http://" + appContext.getEnvironment().getProperty("dhnclient.server") + "/";
        userid = appContext.getEnvironment().getProperty("dhnclient.userid");

        if (param.getFtkao_use() != null && param.getFtkao_use().equalsIgnoreCase("Y")) {
            try{
                kaoRequestService.atTableCheck(param);
                isStart = true;
                log.info("FT 초기화 완료");
            }catch (Exception e){
                log.error("{}테이블 생성 오류 : ", param.getAt_table() + e.getMessage());
            }
        } else {
            posts.postProcessBeforeDestruction(this, null);
        }
    }

    @Scheduled(fixedDelay = 100)
    public void SendProcess() {
        if(isStart && !isProc && sendService.getActiveFTThreads() < SendService.MAX_THREADS) {
            isProc = true;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
            LocalDateTime now = LocalDateTime.now();
            String group_no = "5" + now.format(formatter);

            if(!group_no.equals(preGroupNo)) {

                try {
                    int cnt = kaoRequestService.selectFTRequestCount(param);

                    if(cnt > 0) {
                        param.setGroup_no(group_no);
                        kaoRequestService.updateFTGroupNo(param);
                        List<KAORequestBean> _list = kaoRequestService.selectFTRequests(param);

                        SQLParameter paramCopy = param.toBuilder().build();

                        sendService.FTSendAsync(_list, paramCopy, group_no);

                    }

                }catch (Exception e) {
                    log.error("FT 메세지 전송 오류 : " + e.toString());
                }
                preGroupNo = group_no;
            }
            isProc = false;
        } else if (sendService.getActiveFTThreads() >= SendService.MAX_THREADS) {
            //log.info("KAO 스케줄러: 최대 활성화된 쓰레드 수에 도달했습니다. 다음 주기에 다시 시도합니다.");
        }
    }
}
