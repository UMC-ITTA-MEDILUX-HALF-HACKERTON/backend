package com.allbutyou.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 후보 목록 조회 응답이자 WebSocket CANDIDATE_UPDATE 페이로드. 클라이언트는 그대로 교체하면 된다. */
@Schema(description = "후보 목록과 투표 현황")
public record CandidateListResponse(
        @Schema(description = "후보 목록 (등록 순)") List<CandidateResponse> candidates,
        @Schema(description = "모임 전체 인원", example = "4") int totalMembers,
        @Schema(description = "투표한 인원", example = "2") int votedCount,
        @Schema(description = "아직 투표하지 않은 참가자 (참여 순)") List<NotVotedMemberResponse> notVotedMembers,
        @Schema(description = "확정된 후보 ID (확정 전이면 null)", example = "null", nullable = true)
        Long decidedCandidateId
) {
}
