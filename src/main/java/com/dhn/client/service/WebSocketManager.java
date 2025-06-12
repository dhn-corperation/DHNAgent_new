package com.dhn.client.service;

import com.dhn.client.bean.SendData;
import com.dhn.client.controller.ResultHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class WebSocketManager {
    private WebSocket webSocket;
    private boolean isConnected = false;
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConcurrentHashMap<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();

    @Autowired
    private ResultHandler resultHandler;

    @Value("${dhnclient.url}")
    private String url;

    public synchronized void connect() {
        if (isConnected) {
            System.out.println("서버와 이미 연결되어 있음.");
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Sec-WebSocket-Protocol", "json")
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                isConnected = true;
                log.info("연결 성공!");
            }

            @Override
            public void onMessage(WebSocket webSocket, String response) {
                log.info("WebSocket 응답 수신: {}", response);
                try {
                    Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);

                    // ✅ 1. 최종 결과 데이터인지 확인
                    if (responseMap.containsKey("data") && responseMap.get("data") instanceof Map) {
                        Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");

                        if (dataMap.containsKey("detail") && dataMap.get("detail") instanceof List) {
                            List<Map<String, Object>> resultList = (List<Map<String, Object>>) dataMap.get("detail");
                            log.info("서버 최종 결과 수신: {}건", resultList.size());

                            resultHandler.processFinalResult(resultList);  // 비동기 처리
                        } else {
                            log.warn("최종 결과 데이터 내 detail 필드가 존재하지 않음: {}", response);
                        }

                    } else if (responseMap.containsKey("sendgroup")) { // ✅ 2. 요청에 대한 응답인지 확인
                        String groupNo = (String) responseMap.get("sendgroup");

                        if (groupNo != null && pendingRequests.containsKey(groupNo)) {
                            pendingRequests.get(groupNo).complete(response);
                            pendingRequests.remove(groupNo);
                        } else {
                            log.warn("응답을 처리할 sendgroup가 없음. 응답: {}", response);
                        }
                    }
                    // ✅ 3. 기타 메시지
                    else {
                        log.warn("알 수 없는 WebSocket 응답 형식: {}", response);
                    }

                } catch (Exception e) {
                    log.error("WebSocket 응답 처리 중 오류 발생: {}", e.getMessage(), e);
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                isConnected = false;
                log.info("연결 종료: " + reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                isConnected = false;
                log.info("연결 오류 발생: " + t.getMessage());

                // ✅ 모든 대기 중 요청에 대해 강제로 예외 발생시킴
                for (Map.Entry<String, CompletableFuture<String>> entry : pendingRequests.entrySet()) {
                    entry.getValue().completeExceptionally(
                            new RuntimeException("WebSocket 연결 실패로 인한 요청 중단: " + t.getMessage())
                    );
                }

                pendingRequests.clear();
            }
        });
    }

    // ✅ 요청을 보낼 때 group_no를 기반으로 응답을 기다림
    public String sendBatchMessageSync(String groupNo, List<SendData> dataList, String type) throws Exception {
        if (!isConnected || webSocket == null) {
            throw new Exception("연결 끊김!");
        }

        StringWriter sw = new StringWriter();
        objectMapper.writeValue(sw, dataList);
        String jsonMessage = sw.toString();

        jsonMessage = new String(jsonMessage.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        // ✅ CompletableFuture를 생성하여 응답을 받을 때까지 대기
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(groupNo, future);

        boolean success = webSocket.send(jsonMessage);
        if (!success) {
            log.error("WebSocket 메시지 전송 실패: {}", groupNo);
            throw new Exception("WebSocket 데이터 전송 실패");
        }
        log.info("{} 데이터 전송 완료: {}", type, groupNo);

        try {
            // ✅ 최대 10초 동안 응답을 기다림
            return future.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new Exception("WebSocket 응답 대기 중 타임아웃 발생");
        } catch (Exception e){
            throw new Exception("WebSocket 처리 중 예외 발생", e);
        }finally {
            pendingRequests.remove(groupNo);  // ✅ 항상 정리
        }
    }

    public boolean isConnected() {
        return isConnected;
    }
}
