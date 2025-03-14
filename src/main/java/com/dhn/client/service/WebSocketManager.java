package com.dhn.client.service;

import com.dhn.client.bean.SendData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
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

                    // ✅ 응답에서 group_no 추출
                    String groupNo = (String) responseMap.get("sendgroup");

                    if (groupNo != null && pendingRequests.containsKey(groupNo)) {
                        pendingRequests.get(groupNo).complete(response);
                        pendingRequests.remove(groupNo);
                    } else {
                        log.warn("응답을 처리할 sendgroup가 없음. 응답: {}", response);
                    }
                } catch (Exception e) {
                    log.error("WebSocket 응답 처리 중 오류 발생: {}", e.getMessage());
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

        webSocket.send(jsonMessage);
        log.info("{} 데이터 전송 완료: {}", type, jsonMessage);

        try {
            // ✅ 최대 10초 동안 응답을 기다림
            return future.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pendingRequests.remove(groupNo);
            throw new Exception("WebSocket 응답 대기 중 타임아웃 발생");
        }
    }

    public boolean isConnected() {
        return isConnected;
    }
}
