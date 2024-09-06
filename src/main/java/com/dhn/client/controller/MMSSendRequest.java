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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.stereotype.Component;

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
		

		dhnServer = "http://" + appContext.getEnvironment().getProperty("dhnclient.server") + "/";
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

						mmsImageBean.setFile1(null);

						MultipartBody.Builder builder = new MultipartBody.Builder();
						builder.addFormDataPart("userid", userid);
						if(mmsImageBean.getFile1() != null && mmsImageBean.getFile1().length() > 0) {
							File file = new File(basepath + mmsImageBean.getFile1());
							builder.addFormDataPart("image1", mmsImageBean.getFile1(), RequestBody.create(MultipartBody.FORM,file));
						}
						if(mmsImageBean.getFile2() != null && mmsImageBean.getFile2().length() > 0) {
							File file = new File(basepath + mmsImageBean.getFile2());
							builder.addFormDataPart("image2", mmsImageBean.getFile2(), RequestBody.create(MultipartBody.FORM, file));
						}
						if(mmsImageBean.getFile3() != null && mmsImageBean.getFile3().length() > 0) {
							File file = new File(basepath + mmsImageBean.getFile3());
							builder.addFormDataPart("image3", mmsImageBean.getFile3(), RequestBody.create(MultipartBody.FORM, file));
						}

						builder.setType(MultipartBody.FORM);

						RequestBody reqbody = builder.build();

						Request request = new Request.Builder()
								.url(dhnServer + "mms/image")
								.post(reqbody)
								.build();

						try {
							OkHttpClient client = new OkHttpClient();
							Response response = client.newCall(request).execute();

							String responseBody = response.body().string(); // 응답 본문 저장

							ObjectMapper mapper = new ObjectMapper();
							Map<String, String> res = mapper.readValue(responseBody, Map.class);

							LocalDate now = LocalDate.now();
							DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
							String currentMonth = now.format(formatter);

							if(response.code() == 200) {
								log.info("MMS Image Key : " + res.toString());
								if(res.get("image_group") != null && res.get("image_group").length() > 0) {
									param.setMms_key(res.get("image_group"));
									msgRequestService.updateMMSImageGroup(param);
								}else{

									log.info("MMS 이미지 등록 실패 : "+res.toString());

									param.setMsg_log_table(msg_log_table+"_"+currentMonth);
									param.setMsg_image_code("9999");
									msgRequestService.updateMMSImageFail(param);
								}
							}else{
								log.info("MMS 이미지 등록 실패 : "+res.toString());
								param.setMsg_log_table(msg_log_table+"_"+currentMonth);
								param.setMsg_image_code(String.valueOf(response.code()));
								msgRequestService.updateMMSImageFail(param);
							}
							response.close();
						} catch (Exception e) {
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
