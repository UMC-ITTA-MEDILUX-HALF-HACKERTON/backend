package com.allbutyou.poll.service;

import com.allbutyou.poll.dto.PollResponse;

/** 트랜잭션 커밋 후 POLL_DECIDED를 방송하기 위한 이벤트. */
public record PollDecidedEvent(Long roomId, PollResponse poll) {
}
