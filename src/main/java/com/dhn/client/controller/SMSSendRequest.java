package com.dhn.client.controller;

import com.dhn.client.bean.RequestBean;
import com.dhn.client.bean.SQLParameter;
import com.dhn.client.service.MSGRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@Order(4)
public class SMSSendRequest implements ApplicationListener<ContextRefreshedEvent>{
	
	public static boolean isStart = false;
	private boolean isProc = false;
	private SQLParameter param = new SQLParameter();
	private String dhnServer;
	private String userid;
	private String preGroupNo = "";

	@Autowired
	private MSGRequestService msgRequestService;
	
	@Autowired
	private ApplicationContext appContext;

	@Autowired
	private ScheduledAnnotationBeanPostProcessor posts;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		param.setMsg_table(appContext.getEnvironment().getProperty("dhnclient.msg_table"));
		param.setMsg_use(appContext.getEnvironment().getProperty("dhnclient.msg_use"));
		param.setDatabase(appContext.getEnvironment().getProperty("dhnclient.database"));
		param.setSequence(appContext.getEnvironment().getProperty("dhnclient.msg_seq"));
		param.setMsg_type("S");
		
		dhnServer = appContext.getEnvironment().getProperty("dhnclient.server");
		userid = appContext.getEnvironment().getProperty("dhnclient.userid");

		if (param.getMsg_use() != null && param.getMsg_use().equalsIgnoreCase("Y")) {
			isStart = true;
			log.info("SMS 초기화 완료");
		} else {
			posts.postProcessBeforeDestruction(this, null);
		}
	}
	

	@Scheduled(fixedDelay = 100)
	private void SendProcess() {
		if(isStart && !isProc) {
			isProc = true;
			
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
			LocalDateTime now = LocalDateTime.now();
			String group_no = "2" + now.format(formatter);
			
			if(!group_no.equals(preGroupNo)) {
				try {
					int cnt = msgRequestService.selectSMSReqeustCount(param);
					
					if(cnt > 0) {
						param.setGroup_no(group_no);
						msgRequestService.updateSMSGroupNo(param);
						
						List<RequestBean> _list = msgRequestService.selectSMSRequests(param);

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
								msgRequestService.updateSMSSendComplete(param);
								log.info("SMS 메세지 전송 완료 : " + group_no + " / " + _list.size() + " 건");
							} else {
								log.info("({}) SMS 메세지 전송오류 : {}",res.get("userid"), res.get("message"));
								msgRequestService.updateSMSSendInit(param);
							}
						}catch (Exception e) {
							log.error("SMS 메세지 전송 오류 : " + e.toString());
							msgRequestService.updateSMSSendInit(param);
						}
					}
				}catch (Exception e) {
					log.error("SMS 메세지 전송 오류 : " + e.toString());
				}
				preGroupNo = group_no;
			}
			isProc = false;
		}
	}

}
