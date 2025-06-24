package com.dhn.client.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class DuplicationChecker implements ApplicationListener<ContextRefreshedEvent> {

    private String dup_flag;
    private String dup_time_str;
    private int dup_time = 5;

    private final ConcurrentHashMap<String, Long> kaoMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> smsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lmsMap = new ConcurrentHashMap<>();
    private long EXPIRE_TIME_MS; // 5분

    @Autowired
    private ApplicationContext appContext;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        dup_flag = appContext.getEnvironment().getProperty("dhnclient.dup_flag","N");
        dup_time_str = appContext.getEnvironment().getProperty("dhnclient.dup_time","5");

        try{
            dup_time = Integer.parseInt(dup_time_str);
        }catch (Exception e){
            dup_time = 5;
        }

        EXPIRE_TIME_MS = (long) dup_time * 60 * 1000;

    }


    // 중복 체크 및 추가
    public synchronized boolean msgCheck(String type,String key) {
        long now = System.currentTimeMillis();

        if(type.equalsIgnoreCase("KAO")){
            Long lastTime = kaoMap.get(key);
            if (lastTime != null && now - lastTime < EXPIRE_TIME_MS) {
                return false;
            }
            kaoMap.put(key, now);
        } else if (type.equalsIgnoreCase("SMS")){
            Long lastTime = smsMap.get(key);
            if (lastTime != null && now - lastTime < EXPIRE_TIME_MS) {
                return false;
            }
            smsMap.put(key, now);
        } else if (type.equalsIgnoreCase("LMS")){
            Long lastTime = lmsMap.get(key);
            if (lastTime != null && now - lastTime < EXPIRE_TIME_MS) {
                return false;
            }
            lmsMap.put(key, now);
        }
        return true; // 신규 등록
    }

    // 1분마다 오래된 데이터 제거
    @Scheduled(fixedDelay = 60000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        kaoMap.entrySet().removeIf(entry -> now - entry.getValue() > EXPIRE_TIME_MS);
        smsMap.entrySet().removeIf(entry -> now - entry.getValue() > EXPIRE_TIME_MS);
        lmsMap.entrySet().removeIf(entry -> now - entry.getValue() > EXPIRE_TIME_MS);
    }

}
