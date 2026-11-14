package com.allbutyou.game.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지각자와 지각비")
public record LateMemberResponse(
        @Schema(description = "멤버 ID", example = "3") Long memberId,
        @Schema(description = "닉네임", example = "철수") String nickname,
        @Schema(description = "현재 지각 분 (아직 미도착이면 null)", example = "12", nullable = true) Integer lateMinutes,
        @Schema(description = "제출 점수 (미제출이면 null)", example = "1234", nullable = true) Integer score,
        @Schema(description = "이 지각자의 지각비", example = "2000") int penalty
) {
}
