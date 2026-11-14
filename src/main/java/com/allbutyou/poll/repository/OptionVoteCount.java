package com.allbutyou.poll.repository;

/** 선택지별 득표 수 집계 결과. */
public record OptionVoteCount(Long pollId, Long optionId, long voteCount) {
}
