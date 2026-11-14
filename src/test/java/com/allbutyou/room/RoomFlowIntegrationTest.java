package com.allbutyou.room;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/** 실제 서버(랜덤 포트) + H2로 생성 → STOMP 구독 → 참여 → MEMBER_UPDATE 수신까지 확인한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
class RoomFlowIntegrationTest {

    @LocalServerPort int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void 참여하면_topic_rooms_roomId로_MEMBER_UPDATE가_방송된다() throws Exception {
        String created = postJson("/api/rooms", """
                {"title":"금요일 저녁 모임","placeName":"강남역","lat":37.4979,"lng":127.0276,
                 "startAt":"2026-11-20T19:00:00","lateFee":1000,"nickname":"민수"}
                """);
        long roomId = Long.parseLong(extract(created, "\"roomId\":(\\d+)"));
        String code = extract(created, "\"code\":\"([A-Z0-9]{6})\"");

        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());
        StompSession session = stompClient
                .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() { })
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<Map<?, ?>> received = new LinkedBlockingQueue<>();
        session.subscribe("/topic/rooms/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((Map<?, ?>) payload);
            }
        });
        Thread.sleep(500); // SUBSCRIBE 프레임이 브로커에 등록될 시간

        postJson("/api/rooms/join", "{\"code\":\"" + code + "\",\"nickname\":\"지영\"}");

        Map<?, ?> message = received.poll(5, TimeUnit.SECONDS);
        assertThat(message).as("MEMBER_UPDATE 수신").isNotNull();
        assertThat(message.get("type")).isEqualTo("MEMBER_UPDATE");
        assertThat(message.get("sentAt")).isNotNull();
        Map<?, ?> data = (Map<?, ?>) message.get("data");
        assertThat(((Number) data.get("roomId")).longValue()).isEqualTo(roomId);
        assertThat((java.util.List<?>) data.get("members")).hasSize(2);

        session.disconnect();
        stompClient.stop();
    }

    @Test
    void swagger_api_docs가_노출되고_자물쇠_설정이_반영된다() throws Exception {
        HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create(base() + "/v3/api-docs")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body())
                .contains("너빼고 다옴 API")
                .contains("X-Participant-Token")
                .contains("\"/api/rooms/{roomId}\"");
    }

    private String postJson(String path, String body) throws Exception {
        HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create(base() + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).as(res.body()).isEqualTo(200);
        return res.body();
    }

    private String base() {
        return "http://localhost:" + port;
    }

    private static String extract(String json, String regex) {
        Matcher m = Pattern.compile(regex).matcher(json);
        assertThat(m.find()).as(json).isTrue();
        return m.group(1);
    }
}
