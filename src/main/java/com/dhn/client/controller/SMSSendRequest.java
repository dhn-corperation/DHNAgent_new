package com.dhn.client.controller;

import com.dhn.client.bean.RequestBean;
import com.dhn.client.bean.SQLParameter;
import com.dhn.client.service.MSGRequestService;
import com.dhn.client.service.RequestService;
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
@Order(4)
public class SMSSendRequest implements ApplicationListener<ContextRefreshedEvent>{
	
	public static boolean isStart = false;
	private boolean isProc = false;
	private SQLParameter param = new SQLParameter();
	private String dhnServer;
	private String userid;
	private String preGroupNo = "";
	
	@Autowired
	private RequestService requestService;

	@Autowired
	private MSGRequestService msgRequestService;
	
	@Autowired
	private ApplicationContext appContext;

	@Autowired
	private SendService sendService;

	@Autowired
	private ScheduledAnnotationBeanPostProcessor posts;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		param.setMsg_table(appContext.getEnvironment().getProperty("dhnclient.msg_table"));
		param.setSms_use(appContext.getEnvironment().getProperty("dhnclient.sms_use"));
		param.setDatabase(appContext.getEnvironment().getProperty("dhnclient.database"));
		param.setSequence(appContext.getEnvironment().getProperty("dhnclient.msg_seq"));
		param.setMsg_type("S");
		
		dhnServer = "http://" + appContext.getEnvironment().getProperty("dhnclient.server") + "/";
		userid = appContext.getEnvironment().getProperty("dhnclient.userid");

		if (param.getSms_use() != null && param.getSms_use().equalsIgnoreCase("Y")) {
			try{
				msgRequestService.msgTableCheck(param);
				isStart = true;
				log.info("SMS 초기화 완료");
			}catch (Exception e){
				log.error("{}테이블 생성 오류 : ", param.getAt_table() + e.getMessage());
			}
		} else {
			posts.postProcessBeforeDestruction(this, null);
		}
	}
	
	/*
	@Scheduled(fixedDelay = 100)
	private void SendProcess() {
		if(isStart && !isProc && sendService.getActiveSMSThreads() < SendService.MAX_THREADS) {
			isProc = true;
			
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
			LocalDateTime now = LocalDateTime.now();
			String group_no = "2" + now.format(formatter);
			
			if(!group_no.equals(preGroupNo)) {
				try {
					int cnt = requestService.selectSMSReqeustCount(param);
					
					if(cnt > 0) {
						param.setGroup_no(group_no);
						requestService.updateSMSGroupNo(param);
						
						List<RequestBean> _list = requestService.selectSMSRequests(param);

						SQLParameter paramCopy = param.toBuilder().build();
						sendService.SMSSendAsync(_list, paramCopy, group_no);
					}
				}catch (Exception e) {
					log.error("SMS 메세지 전송 오류 : " + e.toString());
				}
				preGroupNo = group_no;
			}
			isProc = false;
		}else if (sendService.getActiveSMSThreads() >= SendService.MAX_THREADS) {
			//log.info("SMS 스케줄러: 최대 활성화된 쓰레드 수에 도달했습니다. 다음 주기에 다시 시도합니다.");
		}
	}

	 */
}
