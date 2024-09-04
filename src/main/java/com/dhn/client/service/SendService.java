package com.dhn.client.service;

import com.dhn.client.bean.KAORequestBean;
import com.dhn.client.bean.RequestBean;
import com.dhn.client.bean.SQLParameter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class SendService {

    @Autowired
    private KAORequestService kaoRequestService;

    @Autowired
    private MSGRequestService msgRequestService;

    @Autowired
    private KAOService kaoService;

    private final String dhnServer;
    private final String userid;

    public SendService(ApplicationContext appContext) {
        this.dhnServer = "http://" + appContext.getEnvironment().getProperty("dhnclient.server") + "/";
        this.userid = appContext.getEnvironment().getProperty("dhnclient.userid");
    }

    private AtomicInteger activeKAOThreads = new AtomicInteger(0);
    private AtomicInteger activeSMSThreads = new AtomicInteger(0);
    private AtomicInteger activeLMSThreads = new AtomicInteger(0);
    private AtomicInteger activeMMSThreads = new AtomicInteger(0);
    public static final int MAX_THREADS = 5; // 최대 쓰레드 수

    public int getActiveKAOThreads() {
        return activeKAOThreads.get();
    }

    public int getActiveSMSThreads() {
        return activeSMSThreads.get();
    }

    public int getActiveLMSThreads() {
        return activeLMSThreads.get();
    }

    public int getActiveMMSThreads() {
        return activeMMSThreads.get();
    }

    @Async("kaoTaskExecutor") // 비동기 처리
    @Retryable(
            value = {Exception.class}, // 재시도할 예외 유형
            maxAttempts = 3, // 최대 시도 횟수
            backoff = @Backoff(delay = 2000) // 재시도 간의 대기 시간 (밀리초)
    )
    public void KAOSendAsync(List<KAORequestBean> _list, SQLParameter paramCopy, String group_no) throws Exception {
        if (activeKAOThreads.incrementAndGet() <= MAX_THREADS) {
            boolean apiCalled = false;
            List<String> json_err_msgid = new ArrayList<>(); // json 직렬화 -> 역직렬화시 기존 데이터 비교 후 다를때(문제있는 데이터) 상태값 별도로 update
            List<KAORequestBean> removeData = new ArrayList<>(); // 위의 json 직, 역직렬화시 다를때 API전송시 지울 데이터
            try{
                if(!apiCalled){
                    for (KAORequestBean kaoRequestBean : _list) {

                        StringWriter valSw = new StringWriter();
                        ObjectMapper valOm = new ObjectMapper();
                        valOm.writeValue(valSw, kaoRequestBean);
                        String jsonString = valSw.toString();

                        KAORequestBean newkao = valOm.readValue(jsonString, KAORequestBean.class);

                        boolean isEqual = kaoRequestBean.equals(newkao);

                        if (!isEqual) {
                            log.info("JSON 변환 작업에 이상이 있는 데이터 입니다. / 메세지 아이디 : "+kaoRequestBean.getMsgid());
                            json_err_msgid.add(kaoRequestBean.getMsgid());
                            removeData.add(kaoRequestBean);
                        }
                    }

                    _list.removeAll(removeData);

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
                            kaoRequestService.updateKAOSendComplete(paramCopy);
                            log.info("KAO 메세지 전송 완료 : " + response.getStatusCode() + " / " + group_no + " / " + _list.size() + " 건");
                        } else if(response.getStatusCode() == HttpStatus.BAD_REQUEST){ // 데이터 전달 시 데이터 손상 즉, json 깨질떄
                            kaoRequestService.updateKAOSendInit(paramCopy);
                            log.info("({}) KAO 메세지 전송오류 : {}",res.get("userid"), res.get("message"));
                        } else if(response.getStatusCode() == HttpStatus.NOT_ACCEPTABLE){ // 허가되지않은 계정 & IP
                            kaoRequestService.updateKAOAuthFail(paramCopy);
                            log.info("({}) KAO 허가되지않은 사용자 입니다. : {}",res.get("userid"), res.get("message"));
                        }else { // API 전송 실패시
                            log.info("({}) KAO 메세지 전송오류 : {}",res.get("userid"), res.get("message"));
                            kaoRequestService.updateKAOSendInit(paramCopy);
                        }
                        apiCalled = true;
                    } catch (Exception e) {
                        log.error("KAO 메세지 전송 오류 : " + e.toString());
                        kaoRequestService.updateKAOSendInit(paramCopy);
                        throw e;
                    }
                }

                if (apiCalled) {
                    if (!json_err_msgid.isEmpty()) {
                        kaoRequestService.kaoJsonErrMessage(paramCopy, json_err_msgid);
                    }
                }
            }catch (Exception e){
                log.error("KAO 비동기 작업 처리 중 오류 발생: " + e.toString());
                throw e;
            }finally {
                activeKAOThreads.decrementAndGet(); // 작업 완료 후 활성화된 쓰레드 수 감소
            }
        }else{
            //log.info("(내부)KAO 작업의 최대 활성화된 쓰레드 수에 도달했습니다.");
            activeKAOThreads.decrementAndGet(); // 실패한 경우에도 감소
        }
    }


    @Async("smsTaskExecutor")
    @Retryable(
            value = {Exception.class}, // 재시도할 예외 유형
            maxAttempts = 3, // 최대 시도 횟수
            backoff = @Backoff(delay = 2000) // 재시도 간의 대기 시간 (밀리초)
    )
    public void SMSSendAsync(List<RequestBean> _list, SQLParameter paramCopy, String group_no) throws Exception {
        if (activeSMSThreads.incrementAndGet() <= MAX_THREADS) {
            boolean apiCalled = false;
            List<String> json_err_msgid = new ArrayList<>();
            List<RequestBean> removeData = new ArrayList<>();
            try{
                if (!apiCalled) {
                    for (RequestBean requestBean : _list) {

                        StringWriter valSw = new StringWriter();
                        ObjectMapper valOm = new ObjectMapper();
                        valOm.writeValue(valSw, requestBean);
                        String jsonString = valSw.toString();

                        RequestBean newsms = valOm.readValue(jsonString, RequestBean.class);

                        boolean isEqual = requestBean.equals(newsms);

                        if (!isEqual) {
                            log.info("JSON 변환 작업에 이상이 있는 데이터 입니다. / 메세지 아이디 : "+requestBean.getMsgid());
                            json_err_msgid.add(requestBean.getMsgid());
                            removeData.add(requestBean);
                        }
                    }

                    _list.removeAll(removeData);

                    StringWriter sw = new StringWriter();
                    ObjectMapper om = new ObjectMapper();
                    om.writeValue(sw, _list);

                    HttpHeaders header = new HttpHeaders();

                    header.setContentType(MediaType.APPLICATION_JSON);
                    header.set("userid", userid);

                    RestTemplate rt = new RestTemplate();
                    HttpEntity<String> entity = new HttpEntity<String>(sw.toString(), header);

                    try {
                        ResponseEntity<String> response = rt.postForEntity(dhnServer + "req", entity, String.class);
                        Map<String, String> res = om.readValue(response.getBody().toString(), Map.class);
                        if(response.getStatusCode() ==  HttpStatus.OK)
                        {
                            msgRequestService.updateSMSSendComplete(paramCopy);
                            log.info("SMS 메세지 전송 완료 : " + group_no + " / " + _list.size() + " 건");
                        } else if(response.getStatusCode() == HttpStatus.BAD_REQUEST){
                            msgRequestService.updateSMSSendInit(paramCopy);
                            log.info("({}) SMS 메세지 전송오류 : {}",res.get("userid"), res.get("message"));
                        } else if(response.getStatusCode() == HttpStatus.NOT_ACCEPTABLE){ // 허가되지않은 계정 & IP
                            msgRequestService.updateMSGAuthFail(paramCopy);
                            log.info("({}) KAO 허가되지않은 사용자 입니다. : {}",res.get("userid"), res.get("message"));
                        } else {
                            log.info("({}) SMS 메세지 전송오류 : {}",res.get("userid"), res.get("message"));
                            msgRequestService.updateSMSSendInit(paramCopy);
                        }
                        apiCalled = true;
                    }catch (Exception e) {
                        log.error("SMS 메세지 전송 오류 : " + e.toString());
                        msgRequestService.updateSMSSendInit(paramCopy);
                        throw e;
                    }
                }

                if (apiCalled) {
                    if (!json_err_msgid.isEmpty()) {
                        msgRequestService.jsonErrMessage(paramCopy, json_err_msgid);
                    }
                }
            }catch (Exception e){
                log.error("SMS 비동기 작업 처리 중 오류 발생: " + e.toString());
                throw e;
            }finally {
                activeSMSThreads.decrementAndGet(); // 작업 완료 후 활성화된 쓰레드 수 감소
            }
        }else{
            //log.info("(내부)KAO 작업의 최대 활성화된 쓰레드 수에 도달했습니다.");
            activeSMSThreads.decrementAndGet(); // 실패한 경우에도 감소
        }
    }

    @Async("lmsTaskExecutor")
    @Retryable(
            value = {Exception.class}, // 재시도할 예외 유형
            maxAttempts = 3, // 최대 시도 횟수
            backoff = @Backoff(delay = 2000) // 재시도 간의 대기 시간 (밀리초)
    )
    public void LMSSendAsync(List<RequestBean> _list, SQLParameter paramCopy, String group_no) throws Exception {
        if (activeLMSThreads.incrementAndGet() <= MAX_THREADS) {
            boolean apiCalled = false;
            List<String> json_err_msgid = new ArrayList<>();
            List<RequestBean> removeData = new ArrayList<>();
            try{
                if (!apiCalled) {

                    for (RequestBean requestBean : _list) {

                        StringWriter valSw = new StringWriter();
                        ObjectMapper valOm = new ObjectMapper();
                        valOm.writeValue(valSw, requestBean);
                        String jsonString = valSw.toString();

                        RequestBean newlms = valOm.readValue(jsonString, RequestBean.class);

                        boolean isEqual = requestBean.equals(newlms);

                        if (!isEqual) {
                            log.info("JSON 변환 작업에 이상이 있는 데이터 입니다. / 메세지 아이디 : "+requestBean.getMsgid());
                            json_err_msgid.add(requestBean.getMsgid());
                            removeData.add(requestBean);
                        }
                    }

                    _list.removeAll(removeData);

                    StringWriter sw = new StringWriter();
                    ObjectMapper om = new ObjectMapper();
                    om.writeValue(sw, _list);

                    HttpHeaders header = new HttpHeaders();

                    header.setContentType(MediaType.APPLICATION_JSON);
                    header.set("userid", userid);

                    RestTemplate rt = new RestTemplate();
                    HttpEntity<String> entity = new HttpEntity<String>(sw.toString(), header);

                    try {
                        ResponseEntity<String> response = rt.postForEntity(dhnServer + "req", entity, String.class);
                        Map<String, String> res = om.readValue(response.getBody().toString(), Map.class);
                        if(response.getStatusCode() ==  HttpStatus.OK)
                        {
                            msgRequestService.updateSMSSendComplete(paramCopy);
                            log.info("LMS 메세지 전송 완료 : " + group_no + " / " + _list.size() + " 건");
                        } else if(response.getStatusCode() == HttpStatus.BAD_REQUEST){
                            msgRequestService.updateSMSSendInit(paramCopy);
                            log.info("({}) LMS 메세지 전송오류 : {}",res.get("userid"), res.get("message"));
                        } else if(response.getStatusCode() == HttpStatus.NOT_ACCEPTABLE){ // 허가되지않은 계정 & IP
                            msgRequestService.updateMSGAuthFail(paramCopy);
                            log.info("({}) KAO 허가되지않은 사용자 입니다. : {}",res.get("userid"), res.get("message"));
                        } else {
                            log.info("({}) LMS 메세지 전송오류 : {}",res.get("userid"), res.get("message"));
                            msgRequestService.updateSMSSendInit(paramCopy);
                        }
                        apiCalled = true;
                    }catch (Exception e) {
                        log.error("LMS 메세지 전송 오류 : " + e.toString());
                        msgRequestService.updateSMSSendInit(paramCopy);
                        throw e;
                    }
                }

                if (apiCalled) {
                    if (!json_err_msgid.isEmpty()) {
                        msgRequestService.jsonErrMessage(paramCopy, json_err_msgid);
                    }
                }
            }catch (Exception e){
                log.error("LMS 비동기 작업 처리 중 오류 발생: " + e.toString());
                throw e;
            }finally {
                activeLMSThreads.decrementAndGet(); // 작업 완료 후 활성화된 쓰레드 수 감소
            }
        }else{
            //log.info("(내부)KAO 작업의 최대 활성화된 쓰레드 수에 도달했습니다.");
            activeLMSThreads.decrementAndGet(); // 실패한 경우에도 감소
        }
    }

    @Async("mmsTaskExecutor")
    @Retryable(
            value = {Exception.class}, // 재시도할 예외 유형
            maxAttempts = 3, // 최대 시도 횟수
            backoff = @Backoff(delay = 2000) // 재시도 간의 대기 시간 (밀리초)
    )
    public void MMSSendAsync(List<RequestBean> _list, SQLParameter paramCopy, String group_no) throws Exception {
        if (activeMMSThreads.incrementAndGet() <= MAX_THREADS) {
            boolean apiCalled = false;
            List<String> json_err_msgid = new ArrayList<>();
            List<RequestBean> removeData = new ArrayList<>();
            try{
                if (!apiCalled) {

                    for (RequestBean requestBean : _list) {

                        StringWriter valSw = new StringWriter();
                        ObjectMapper valOm = new ObjectMapper();
                        valOm.writeValue(valSw, requestBean);
                        String jsonString = valSw.toString();

                        RequestBean newlms = valOm.readValue(jsonString, RequestBean.class);

                        boolean isEqual = requestBean.equals(newlms);

                        if (!isEqual) {
                            log.info("JSON 변환 작업에 이상이 있는 데이터 입니다. / 메세지 아이디 : "+requestBean.getMsgid());
                            json_err_msgid.add(requestBean.getMsgid());
                            removeData.add(requestBean);
                        }
                    }

                    _list.removeAll(removeData);

                    StringWriter sw = new StringWriter();
                    ObjectMapper om = new ObjectMapper();
                    om.writeValue(sw, _list);

                    HttpHeaders header = new HttpHeaders();

                    header.setContentType(MediaType.APPLICATION_JSON);
                    header.set("userid", userid);

                    RestTemplate rt = new RestTemplate();
                    HttpEntity<String> entity = new HttpEntity<String>(sw.toString(), header);

                    try {
                        ResponseEntity<String> response = rt.postForEntity(dhnServer + "req", entity, String.class);
                        Map<String, String> res = om.readValue(response.getBody().toString(), Map.class);
                        if(response.getStatusCode() ==  HttpStatus.OK)
                        {
                            msgRequestService.updateSMSSendComplete(paramCopy);
                            log.info("MMS 메세지 전송 완료 : " + group_no + " / " + _list.size() + " 건");
                        } else if(response.getStatusCode() == HttpStatus.BAD_REQUEST){
                            msgRequestService.updateSMSSendInit(paramCopy);
                            log.info("({}) MMS 메세지 전송오류 : {}",res.get("userid"), res.get("message"));
                        } else if(response.getStatusCode() == HttpStatus.NOT_ACCEPTABLE){ // 허가되지않은 계정 & IP
                            msgRequestService.updateMSGAuthFail(paramCopy);
                            log.info("({}) KAO 허가되지않은 사용자 입니다. : {}",res.get("userid"), res.get("message"));
                        } else {
                            log.info("({}) MMS 메세지 전송오류 : {}",res.get("userid"), res.get("message"));
                            msgRequestService.updateSMSSendInit(paramCopy);
                        }
                        apiCalled = true;
                    }catch (Exception e) {
                        log.error("MMS 메세지 전송 오류 : " + e.toString());
                        msgRequestService.updateSMSSendInit(paramCopy);
                        throw e;
                    }
                }

                if (apiCalled) {
                    if (!json_err_msgid.isEmpty()) {
                        msgRequestService.jsonErrMessage(paramCopy, json_err_msgid);
                    }
                }
            }catch (Exception e){
                log.error("MMS 비동기 작업 처리 중 오류 발생: " + e.toString());
                throw e;
            }finally {
                activeMMSThreads.decrementAndGet(); // 작업 완료 후 활성화된 쓰레드 수 감소
            }
        }else{
            //log.info("(내부)KAO 작업의 최대 활성화된 쓰레드 수에 도달했습니다.");
            activeMMSThreads.decrementAndGet(); // 실패한 경우에도 감소
        }
    }

}
