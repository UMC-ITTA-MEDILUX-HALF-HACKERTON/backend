package com.allbutyou.candidate.service;

import com.allbutyou.candidate.dto.CandidateListResponse;

/** 트랜잭션 커밋 후 CANDIDATE_UPDATE를 방송하기 위한 이벤트. */
public record CandidateUpdatedEvent(Long roomId, CandidateListResponse candidates) {
}
