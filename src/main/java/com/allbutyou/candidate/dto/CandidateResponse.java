package com.allbutyou.candidate.dto;

import com.allbutyou.candidate.domain.Candidate;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "후보")
public record CandidateResponse(
        @Schema(description = "후보 ID", example = "1") Long candidateId,
        @Schema(description = "장소 이름", example = "성수 볼링장") String placeName,
        @Schema(description = "장소 위도 (미입력이면 null)", example = "37.5446", nullable = true) Double lat,
        @Schema(description = "장소 경도 (미입력이면 null)", example = "127.0559", nullable = true) Double lng,
        @Schema(description = "할 활동", example = "볼링") String activity,
        @Schema(description = "등록한 참가자 닉네임", example = "민수") String createdByNickname,
        @Schema(description = "득표 수", example = "2") long voteCount
) {

    public static CandidateResponse of(Candidate candidate, long voteCount) {
        return new CandidateResponse(candidate.getId(), candidate.getPlaceName(), candidate.getLat(),
                candidate.getLng(), candidate.getActivity(), candidate.getCreatedBy().getNickname(), voteCount);
    }
}
