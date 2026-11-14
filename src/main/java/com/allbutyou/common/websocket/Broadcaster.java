package com.allbutyou.common.websocket;

public interface Broadcaster {

    /** /topic/rooms/{roomId} 구독자에게 WsMessage로 감싸 보낸다. */
    void send(Long roomId, WsType type, Object data);
}
