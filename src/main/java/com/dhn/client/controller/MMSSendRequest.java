package com.dhn.client.controller;

import com.dhn.client.bean.ImageBean;
import com.dhn.client.bean.RequestBean;
import com.dhn.client.bean.SQLParameter;
import com.dhn.client.service.MSGRequestService;
import com.dhn.client.service.SendService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@Order(3)
public class MMSSendRequest implements ApplicationListener<ContextRefreshedEvent>{
	
	public static boolean isStart = false;
	private boolean isProc = false;
	private boolean isProcMms = false;
	private SQLParameter param = new SQLParameter();
	private String dhnServer;
	private String userid;
	private String basepath;
	private String preGroupNo = "";
	private String msg_log_table;

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
		param.setMms_use(appContext.getEnvironment().getProperty("dhnclient.mms_use"));
		param.setDatabase(appContext.getEnvironment().getProperty("dhnclient.database"));
		param.setSequence(appContext.getEnvironment().getProperty("dhnclient.msg_seq"));
		msg_log_table = appContext.getEnvironment().getProperty("dhnclient.msg_log_table");
		param.setMsg_type("M");
		

		dhnServer = appContext.getEnvironment().getProperty("dhnclient.server");
		userid = appContext.getEnvironment().getProperty("dhnclient.userid");
		
		// 풀 경로를 DB에 담는듯.
		basepath = appContext.getEnvironment().getProperty("dhnclient.file_base_path")==null?"":appContext.getEnvironment().getProperty("dhnclient.file_base_path");

		if (param.getMms_use() != null && param.getMms_use().equalsIgnoreCase("Y")) {
			try{
				msgRequestService.msgTableCheck(param);
				isStart = true;
				log.info("MMS 초기화 완료");
			}catch (Exception e){
				log.error("{}테이블 생성 오류 : ", param.getAt_table() + e.getMessage());
			}
		} else {
			posts.postProcessBeforeDestruction(this, null);
		}
	}

	@Scheduled(fixedDelay = 100)
	private void SendProcess() {
		if(isStart && !isProc && sendService.getActiveMMSThreads() < SendService.MAX_THREADS) {
			isProc = true;
			
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
			LocalDateTime now = LocalDateTime.now();
			String group_no = "4" + now.format(formatter);
			
			if(!group_no.equals(preGroupNo)) {
				
				try {
					
					int cnt = msgRequestService.selectMMSReqeustCount(param);
					
					if(cnt > 0) {
						
						param.setGroup_no(group_no);
						msgRequestService.updateMMSGroupNo(param);
						List<RequestBean> _list = msgRequestService.selectMMSRequests(param);

						SQLParameter paramCopy = param.toBuilder().build();
						sendService.MMSSendAsync(_list, paramCopy, group_no);

					}
				}catch (Exception e) {
					log.error("MMS 메세지 전송 오류 : " + e.toString());
				}
				preGroupNo = group_no;
			}
			isProc = false;
		}else if (sendService.getActiveMMSThreads() >= SendService.MAX_THREADS) {
			//log.info("SMS 스케줄러: 최대 활성화된 쓰레드 수에 도달했습니다. 다음 주기에 다시 시도합니다.");
		}
	}

	@Scheduled(fixedDelay = 100)
	private void GETImageKey() {
		if(isStart && !isProcMms) {
			isProcMms = true;

			try {

				int cnt = msgRequestService.selectMMSImageCount(param);

				if(cnt > 0){
					List<ImageBean> imgList = msgRequestService.selectMMSImage(param);

					for (ImageBean mmsImageBean : imgList) {
						param.setMsgid(mmsImageBean.getMsgid());

						// 헤더 설정
						HttpHeaders headers = new HttpHeaders();
						headers.setContentType(MediaType.MULTIPART_FORM_DATA);
						headers.set("userid", userid);

						// MultiValueMap을 사용해 파일 데이터 전송 준비
						MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
						body.add("userid", userid);

						if (mmsImageBean.getFile1() != null && mmsImageBean.getFile1().length() > 0) {
							File file = new File(basepath + mmsImageBean.getFile1());
							body.add("image1", new org.springframework.core.io.FileSystemResource(file));
						}
						if (mmsImageBean.getFile2() != null && mmsImageBean.getFile2().length() > 0) {
							File file = new File(basepath + mmsImageBean.getFile2());
							body.add("image2", new org.springframework.core.io.FileSystemResource(file));
						}
						if (mmsImageBean.getFile3() != null && mmsImageBean.getFile3().length() > 0) {
							File file = new File(basepath + mmsImageBean.getFile3());
							body.add("image3", new org.springframework.core.io.FileSystemResource(file));
						}

						// HttpEntity 생성
						HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

						RestTemplate restTemplate = new RestTemplate();

						try{
							ResponseEntity<String> response = restTemplate.exchange(dhnServer + "mms/image", HttpMethod.POST, requestEntity, String.class);

							LocalDate now = LocalDate.now();
							DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
							String currentMonth = now.format(formatter);

							if (response.getStatusCode() == HttpStatus.OK) {
								String responseBody = response.getBody();
								ObjectMapper mapper = new ObjectMapper();
								Map<String, String> res = mapper.readValue(responseBody, Map.class);

								log.info("MMS Image Key : " + res.toString());

								if (res.get("image_group") != null && res.get("image_group").length() > 0) {
									param.setMms_key(res.get("image_group"));
									msgRequestService.updateMMSImageGroup(param);
								} else {
									log.info("MMS 이미지 등록 실패 : " + res.toString());
									param.setMsg_log_table(msg_log_table + "_" + currentMonth);
									param.setMsg_image_code("9999");
									msgRequestService.updateMMSImageFail(param);
								}
							} else {
								log.info("MMS 이미지 등록 실패 : " + response.getBody());
								param.setMsg_log_table(msg_log_table + "_" + currentMonth);
								param.setMsg_image_code(String.valueOf(response.getStatusCodeValue()));
								msgRequestService.updateMMSImageFail(param);
							}
						}catch (Exception e){
							log.error("MMS Image Key 등록 오류 : ", e.getMessage());
						}
					}

				}

			} catch (Exception e) {
				log.error("MMS Image 등록 오류 : " + e.toString());
			}
		}
		isProcMms = false;
	}

}
