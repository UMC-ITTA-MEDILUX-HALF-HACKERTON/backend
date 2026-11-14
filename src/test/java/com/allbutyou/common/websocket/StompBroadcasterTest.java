package com.allbutyou.common.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class StompBroadcasterTest {

    @Test
    void topic_rooms_roomId로_WsMessage를_보낸다() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        new StompBroadcaster(template).send(7L, WsType.MEMBER_UPDATE, "payload");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(eq("/topic/rooms/7"), captor.capture());
        WsMessage<?> message = (WsMessage<?>) captor.getValue();
        assertThat(message.type()).isEqualTo(WsType.MEMBER_UPDATE);
        assertThat(message.data()).isEqualTo("payload");
        assertThat(message.sentAt()).isNotNull();
    }
}
