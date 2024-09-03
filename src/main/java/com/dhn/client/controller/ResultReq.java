package com.dhn.client.controller;

import com.dhn.client.bean.LMSTableBean;
import com.dhn.client.bean.Msg_Log;
import com.dhn.client.bean.SQLParameter;
import com.dhn.client.service.KAORequestService;
import com.dhn.client.service.MSGRequestService;
import com.dhn.client.service.RequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@Order(5)
public class ResultReq implements ApplicationListener<ContextRefreshedEvent>{
	
	public static boolean isStart = false;
	private boolean isProc = false;
	//private SQLParameter param = new SQLParameter();
	private String dhnServer;
	private String userid;
	private Map<String, String> _kaoCode = new HashMap<String,String>();
	private static int procCnt = 0;
	private String atTable = "";
	private String atLogTable = "";
	private String msgTable = "";
	private String msgLogTable = "";
	private String database = "";
	
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

		dhnServer = "http://" + appContext.getEnvironment().getProperty("dhnclient.server") + "/";
		userid = appContext.getEnvironment().getProperty("dhnclient.userid");

		try{
			if(appContext.getEnvironment().getProperty("dhnclient.kakao_use").equalsIgnoreCase("Y")){
				kaoRequestService.atLogTableCheck(atTable,atLogTable, database);
			}

			if(appContext.getEnvironment().getProperty("dhnclient.lms_use").equalsIgnoreCase("Y") ||
					appContext.getEnvironment().getProperty("dhnclient.sms_use").equalsIgnoreCase("Y")||
					appContext.getEnvironment().getProperty("dhnclient.mms_use").equalsIgnoreCase("Y")){
				msgRequestService.msgLogTableCheck(msgTable, msgLogTable, database);
			}
		}catch (Exception e){
			log.error("log 테이블 생성 오류 : "+e.getMessage());
		}
		
		isStart = true;
	}


	@Scheduled(fixedDelay = 100)
	private void SendProcess() {
		if(isStart && !isProc && procCnt < 10) {
			isProc = true;
			procCnt++;
			try {
				ObjectMapper om = new ObjectMapper();
				HttpHeaders header = new HttpHeaders();
				
				header.setContentType(MediaType.APPLICATION_JSON);
				header.set("userid", userid);
				
				RestTemplate rt = new RestTemplate();
				HttpEntity<String> entity = new HttpEntity<String>(null, header);
				
				try {
					ResponseEntity<String> response = rt.postForEntity(dhnServer + "result", entity, String.class);
											
					if(response.getStatusCode() ==  HttpStatus.OK)
					{
						JSONArray json = new JSONArray(response.getBody().toString());
						if(json.length()>0) {
							Thread res = new Thread(() ->ResultProc(json, procCnt) );
							res.start();
						} else {
							Thread.sleep(5000);
							procCnt--;
						}
					} else {
						procCnt--;
					}
				} catch(Exception ex) {
					log.error("결과 수신 오류 : " + ex.toString());
					procCnt--;
				}
				
			}catch (Exception e) {
				log.error("결과 수신 오류 : " + e.toString());
				procCnt--;
			}
			isProc = false;
		}
	}


	private void ResultProc(JSONArray json, int _pc) {
		for(int i=0; i<json.length(); i++) {
			JSONObject ent = json.getJSONObject(i);
			
			Msg_Log kao_ml = new Msg_Log();
			Msg_Log msg_ml = new Msg_Log();

			LocalDate now = LocalDate.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
			String currentMonth = now.format(formatter);

			if(ent.getString("message_type").equalsIgnoreCase("AT")){
				// 알림톡
				kao_ml.setMsgid(ent.getString("msgid"));
				kao_ml.setAt_table(atTable);
				kao_ml.setAt_log_table(atLogTable+"_"+currentMonth);
				kao_ml.setDatabase(database);

				kao_ml.setResult_dt(ent.getString("res_dt"));
				kao_ml.setS_code(ent.getString("s_code"));

				if(ent.getString("s_code").equals("0000")){
					kao_ml.setStatus("3");
				}else{
					kao_ml.setStatus("4");
				}

			}else if(ent.getString("message_type").equalsIgnoreCase("PH") && ent.has("s_code") && !ent.isNull("s_code") && ent.getString("s_code").length() > 1){
				// 알림톡 실패 문자
				kao_ml.setMsgid(ent.getString("msgid"));
				kao_ml.setAt_table(atTable);
				kao_ml.setAt_log_table(atLogTable+"_"+currentMonth);
				kao_ml.setDatabase(database);

				kao_ml.setS_code(ent.getString("s_code"));
				kao_ml.setCode(ent.getString("code"));
				kao_ml.setTelecom(ent.getString("remark1"));
				kao_ml.setResult_dt(ent.getString("remark2"));

				if(ent.getString("code").equals("0000")){
					kao_ml.setStatus("3");
				}else{
					kao_ml.setStatus("4");
				}
				kao_ml.setReal_send_type(ent.getString("sms_kind"));

			}else{
				// 문자
				msg_ml.setMsgid(ent.getString("msgid"));
				msg_ml.setMsg_table(msgTable);
				msg_ml.setMsg_log_table(msgLogTable+"_"+currentMonth);
				msg_ml.setDatabase(database);

				msg_ml.setCode(ent.getString("code"));
				msg_ml.setReal_send_type(ent.getString("sms_kind"));
				msg_ml.setTelecom(ent.getString("remark1"));
				msg_ml.setResult_dt(ent.getString("remark2"));
				if(ent.getString("code").equals("0000")){
					msg_ml.setStatus("3");
				}else{
					msg_ml.setStatus("4");
				}
			}

			try{
				if (msg_ml.getMsg_table() != null && msg_ml.getMsg_log_table() != null) {
					log.info("MSG 결과처리 : {}",msg_ml.toString());
					// MSG 처리 로직
				}else if (kao_ml.getAt_table() != null && kao_ml.getAt_log_table() != null) {
					log.info("KAO 결과처리 : {}",kao_ml.toString());
					kaoRequestService.kaoResultInsert(kao_ml);
				}
			}catch (Exception e){

			}

		}
		log.info("결과 수신 완료 : " + json.length() + " 건");		
		procCnt--;
		
	}

}
