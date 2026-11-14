package com.allbutyou.poll.repository;

/** 투표별로 누가 어떤 선택지에 투표했는지. 미응답자와 myOptionId 계산용. */
public record VoterChoice(Long pollId, Long memberId, Long optionId) {
}
