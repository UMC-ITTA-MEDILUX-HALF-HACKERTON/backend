package com.allbutyou.common.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompBroadcaster implements Broadcaster {

    private static final String DESTINATION_PREFIX = "/topic/gatherings/";

    private final SimpMessagingTemplate messagingTemplate;

    public StompBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void send(String gatheringCode, WsType type, Object data) {
        messagingTemplate.convertAndSend(DESTINATION_PREFIX + gatheringCode, WsMessage.of(type, data));
    }
}
