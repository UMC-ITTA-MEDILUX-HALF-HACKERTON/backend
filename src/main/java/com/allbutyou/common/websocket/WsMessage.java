package com.allbutyou.common.websocket;

import java.time.LocalDateTime;

public record WsMessage<T>(WsType type, T data, LocalDateTime sentAt) {

    public static <T> WsMessage<T> of(WsType type, T data) {
        return new WsMessage<>(type, data, LocalDateTime.now());
    }
}
