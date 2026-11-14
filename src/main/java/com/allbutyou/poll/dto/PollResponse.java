package com.allbutyou.poll.dto;

import com.allbutyou.poll.domain.PollStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 투표 하나의 전체 상태. REST 응답이자 WebSocket POLL_UPDATE·POLL_DECIDED 페이로드.
 * 방송은 모임 전체에 같은 내용이 가므로 myOptionId는 항상 null이다.
 */
@Schema(description = "투표 상태")
public record PollResponse(
        @Schema(description = "투표 ID", example = "1") Long pollId,
        @Schema(description = "투표 주제", example = "1차 메뉴") String title,
        @Schema(description = "상태", example = "OPEN") PollStatus status,
        @Schema(description = "최종 결정된 선택지 ID (결정 전이면 null)", example = "null", nullable = true)
        Long decidedOptionId,
        @Schema(description = "선택지 (등록 순, 득표 수 포함)") List<PollOptionResponse> options,
        @Schema(description = "투표한 인원", example = "2") int votedCount,
        @Schema(description = "아직 투표하지 않은 참가자 (참여 순)") List<NotVotedMemberResponse> notVotedMembers,
        @Schema(description = "요청자가 투표한 선택지 ID (미투표 또는 WebSocket 방송이면 null)",
                example = "1", nullable = true)
        Long myOptionId
) {

    /** 모임 전체 방송용 사본. 개인 정보인 myOptionId를 비운다. */
    public PollResponse forBroadcast() {
        return new PollResponse(pollId, title, status, decidedOptionId, options, votedCount, notVotedMembers, null);
    }
}
