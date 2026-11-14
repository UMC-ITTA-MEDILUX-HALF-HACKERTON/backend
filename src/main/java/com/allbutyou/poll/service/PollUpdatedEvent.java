package com.allbutyou.poll.service;

import com.allbutyou.poll.dto.PollResponse;

/** 트랜잭션 커밋 후 POLL_UPDATE(생성·선택지 추가·투표)를 방송하기 위한 이벤트. */
public record PollUpdatedEvent(Long roomId, PollResponse poll) {
}
