package com.allbutyou.room.dto;

import java.util.List;

/** WebSocket MEMBER_UPDATE 페이로드. 전체 참가자 목록을 보내 클라이언트가 그대로 교체하도록 한다. */
public record MemberUpdateMessage(Long roomId, List<MemberResponse> members) {
}
