package com.dhn.client.controller;

import com.dhn.client.bean.KAORequestBean;
import com.dhn.client.bean.SQLParameter;
import com.dhn.client.service.KAORequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class KAOSendRequest implements ApplicationListener<ContextRefreshedEvent> {

	public static boolean isStart = false;
	private boolean isProc = false;
	private SQLParameter param = new SQLParameter();
	private String dhnServer;
	private String userid;
	private String preGroupNo = "";

    @Autowired
	private KAORequestService kaoRequestService;

	@Autowired
	private ApplicationContext appContext;

	@Autowired
	private ScheduledAnnotationBeanPostProcessor posts;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		param.setMsg_table(appContext.getEnvironment().getProperty("dhnclient.msg_table"));
		param.setKakao_use(appContext.getEnvironment().getProperty("dhnclient.kakao_use"));
		param.setDatabase(appContext.getEnvironment().getProperty("dhnclient.database"));
		param.setSequence(appContext.getEnvironment().getProperty("dhnclient.at_seq"));
		param.setMsg_type("AT");

		dhnServer = appContext.getEnvironment().getProperty("dhnclient.server");
		userid = appContext.getEnvironment().getProperty("dhnclient.userid");

		if (param.getKakao_use() != null && param.getKakao_use().equalsIgnoreCase("Y")) {
			isStart = true;
			log.info("KAO 초기화 완료");
		} else {
			posts.postProcessBeforeDestruction(this, null);
		}

		
	}

	@Scheduled(fixedDelay = 100)
	public void SendProcess() {
		if(isStart && !isProc) {
			isProc = true;
			
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
			LocalDateTime now = LocalDateTime.now();
			String group_no = "1" + now.format(formatter);
			
			if(!group_no.equals(preGroupNo)) {

				try {
					int cnt = kaoRequestService.selectKAORequestCount(param);

					if(cnt > 0) {
						param.setGroup_no(group_no);
						kaoRequestService.updateKAOGroupNo(param);
						List<KAORequestBean> _list = kaoRequestService.selectKAORequests(param);

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
								kaoRequestService.updateKAOSendComplete(param);
								log.info("KAO 메세지 전송 완료 : " + response.getStatusCode() + " / " + group_no + " / " + _list.size() + " 건");
							}else { // API 전송 실패시
								log.info("({}) KAO 메세지 전송오류 : {}",res.get("userid"), res.get("message"));
								kaoRequestService.updateKAOSendInit(param);
							}
						} catch (Exception e) {
							log.error("KAO 메세지 전송 오류 : " + e.toString());
							kaoRequestService.updateKAOSendInit(param);
						}
					}

				}catch (Exception e) {
					log.error("KAO 메세지 전송 오류 : " + e.toString());
				}
				preGroupNo = group_no;
			}
			isProc = false;
		}
	}

}
