package com.dhn.client.controller;

import com.dhn.client.bean.ImageBean;
import com.dhn.client.bean.SQLParameter;
import com.dhn.client.service.MSGRequestService;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class MMSImageUpload implements ApplicationListener<ContextRefreshedEvent> {

    public static boolean isStart = false;
    private boolean isProc = false;
    private boolean isProcMms = false;
    private SQLParameter param = new SQLParameter();
    private String dhnServer;
    private String userid;
    private String basepath;
    private String preGroupNo = "";
    private String log_table;

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
        param.setLog_back(appContext.getEnvironment().getProperty("dhnclient.log_back","Y"));
        log_table = appContext.getEnvironment().getProperty("dhnclient.log_table");
        param.setMsg_type("PH");
        param.setSms_kind("M");


        dhnServer = appContext.getEnvironment().getProperty("dhnclient.server");
        userid = appContext.getEnvironment().getProperty("dhnclient.userid");

        // 풀 경로를 DB에 담는듯.
        basepath = appContext.getEnvironment().getProperty("dhnclient.file_base_path")==null?"":appContext.getEnvironment().getProperty("dhnclient.file_base_path");

        if (param.getMsg_use() != null && param.getMsg_use().equalsIgnoreCase("Y")) {
            isStart = true;
            log.info("MMS Image Upload 초기화 완료");
        } else {
            posts.postProcessBeforeDestruction(this, null);
        }
    }

    @Scheduled(fixedDelay = 100)
    private void GETImageKey() {
        if(isStart && !isProcMms) {
            isProcMms = true;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
            LocalDateTime now = LocalDateTime.now();
            String group_no = "MI" + now.format(formatter);

            if(!group_no.equals(preGroupNo)) {

                try {

                    int cnt = msgRequestService.selectMMSImageCount(param);

                    if(cnt > 0){
                        param.setGroup_no(group_no);
                        msgRequestService.updateMMSImageGroupNo(param);

                        List<ImageBean> imgList = msgRequestService.selectMMSImage(param);

                        Map<String, List<ImageBean>> groupedImages = new HashMap<>();
                        for (ImageBean bean : imgList) {
                            String key = String.format("%s|%s|%s",
                                    (bean.getFile1() != null ? bean.getFile1() : ""),
                                    (bean.getFile2() != null ? bean.getFile2() : ""),
                                    (bean.getFile3() != null ? bean.getFile3() : ""));
                            groupedImages.computeIfAbsent(key, k -> new ArrayList<>()).add(bean);
                        }

                        for (Map.Entry<String, List<ImageBean>> entry : groupedImages.entrySet()) {
                            List<ImageBean> group = entry.getValue();
                            ImageBean representative = group.get(0);

                            HttpHeaders headers = new HttpHeaders();
                            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                            headers.set("userid", userid);

                            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                            body.add("userid", userid);

                            boolean fileError = false;
                            int fileCount = 0;

                            // 각 파일 필드를 검사 (경로가 있는데 파일이 없으면 에러로 간주)
                            if (isFileIncomplete(representative.getFile1(), body, "image1")) fileError = true;
                            else if (representative.getFile1() != null && !representative.getFile1().isEmpty()) fileCount++;

                            if (isFileIncomplete(representative.getFile2(), body, "image2")) fileError = true;
                            else if (representative.getFile2() != null && !representative.getFile2().isEmpty()) fileCount++;

                            if (isFileIncomplete(representative.getFile3(), body, "image3")) fileError = true;
                            else if (representative.getFile3() != null && !representative.getFile3().isEmpty()) fileCount++;

                            if (!fileError && fileCount > 0) {
                                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                                RestTemplate restTemplate = new RestTemplate();

                                try {
                                    ResponseEntity<String> response = restTemplate.exchange(dhnServer + "mms/image", HttpMethod.POST, requestEntity, String.class);

                                    if (response.getStatusCode() == HttpStatus.OK) {
                                        ObjectMapper mapper = new ObjectMapper();
                                        Map<String, String> res = mapper.readValue(response.getBody(), Map.class);
                                        String imageGroupKey = res.get("image_group");

                                        if (imageGroupKey != null && imageGroupKey.length() > 0) {
                                            param.setMms_key(imageGroupKey);
                                            param.setFile1(representative.getFile1());
                                            param.setFile2(representative.getFile2());
                                            param.setFile3(representative.getFile3());

                                            msgRequestService.updateMMSImageGroup(param);
                                            log.info("MMS Image Batch Success: Key={}, Count={}", imageGroupKey, group.size());
                                        } else {
                                            handleImageFail(group, "9999", res.toString(), param);
                                        }
                                    } else {
                                        handleImageFail(group, String.valueOf(response.getStatusCodeValue()), response.getBody(), param);
                                    }
                                } catch (Exception e) {
                                    log.error("MMS Image API Call Error", e);
                                    handleImageFail(group, "9999", e.getMessage(), param);
                                }
                            }else {
                                log.error("MMS 이미지 파일이 경로에 존재하지 않습니다. (대표 msgid: {})", representative.getMsgid());

                                handleImageFail(group, "9999", "파일 없음: " + representative.getFile1(), param);
                            }
                        }

                    }

                } catch (Exception e) {
                    log.error("MMS Image 등록 오류 : " + e.toString());
                }

                preGroupNo = group_no;
            }
        }
        isProcMms = false;
    }

    private void handleImageFail(List<ImageBean> group, String errCode, String errMsg, SQLParameter globalParam) {
        log.error("MMS 이미지 등록 실패 처리 시작 (사유: {}, 대상: {}건)", errMsg, group.size());

        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String baseLogTable = this.log_table;

        for(ImageBean bean : group) {
            try {
                SQLParameter failParam = new SQLParameter();

                failParam.setDatabase(globalParam.getDatabase());
                failParam.setMsg_table(globalParam.getMsg_table());
                failParam.setMsg_type(globalParam.getMsg_type());
                failParam.setSms_kind(globalParam.getSms_kind());

                if("Y".equalsIgnoreCase(globalParam.getLog_back())) {
                    failParam.setLog_table(baseLogTable + "_" + currentMonth);
                } else {
                    failParam.setLog_table(baseLogTable);
                }

                failParam.setMsgid(bean.getMsgid());
                failParam.setMsg_image_code(errCode);

                msgRequestService.updateMMSImageFail(failParam);

            } catch (Exception e) {
                log.error("개별 메시지 실패 처리 중 오류 (msgid: {}) : {}", bean.getMsgid(), e.getMessage());
            }
        }
    }

    private boolean isFileIncomplete(String filePath, MultiValueMap<String, Object> body, String paramName) {
        if (filePath != null && !filePath.isEmpty()) {
            File file = new File(basepath + filePath);
            if (file.exists()) {
                body.add(paramName, new org.springframework.core.io.FileSystemResource(file));
                return false;
            } else {
                return true;
            }
        }
        return false;
    }
}
