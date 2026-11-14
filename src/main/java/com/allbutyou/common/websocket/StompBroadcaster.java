package com.allbutyou.common.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompBroadcaster implements Broadcaster {

    public static final String DESTINATION_PREFIX = "/topic/rooms/";

    private final SimpMessagingTemplate messagingTemplate;

    public StompBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void send(Long roomId, WsType type, Object data) {
        messagingTemplate.convertAndSend(DESTINATION_PREFIX + roomId, WsMessage.of(type, data));
    }
}
