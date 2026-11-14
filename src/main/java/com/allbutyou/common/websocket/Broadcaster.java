package com.allbutyou.common.websocket;

public interface Broadcaster {

    void send(String gatheringCode, WsType type, Object data);
}
