package com.allbutyou.room.service;

import com.allbutyou.room.dto.ArrivalResponse;

/** 트랜잭션 커밋 후 ARRIVED를 방송하기 위한 이벤트. */
public record MemberArrivedEvent(Long roomId, ArrivalResponse arrival) {
}
