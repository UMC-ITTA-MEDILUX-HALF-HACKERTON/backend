package com.allbutyou.candidate.repository;

/** 후보별 득표 수 집계 결과. */
public record CandidateVoteCount(Long candidateId, long voteCount) {
}
