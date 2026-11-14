package com.allbutyou.room.service;

import com.allbutyou.room.dto.MemberUpdateMessage;

/** 트랜잭션 커밋 후 MEMBER_UPDATE를 방송하기 위한 이벤트. */
public record MemberJoinedEvent(MemberUpdateMessage message) {
}
