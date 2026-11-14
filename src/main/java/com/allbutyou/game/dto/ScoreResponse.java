package com.allbutyou.game.dto;

import com.allbutyou.game.domain.GameTeam;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "제출된 점수")
public record ScoreResponse(
        @Schema(description = "멤버 ID", example = "2") Long memberId,
        @Schema(description = "닉네임", example = "지영") String nickname,
        @Schema(description = "게임 시작 시 고정된 팀", example = "ON_TIME") GameTeam team,
        @Schema(description = "점수", example = "1500") int score
) {
}
