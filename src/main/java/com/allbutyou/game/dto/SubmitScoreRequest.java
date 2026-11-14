package com.allbutyou.game.dto;

import com.allbutyou.game.domain.GameParticipant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "게임 점수 제출 요청")
public record SubmitScoreRequest(
        @Schema(description = "점수 (0 이상 100000 이하)", example = "1234", minimum = "0",
                maximum = "" + GameParticipant.MAX_SCORE)
        @NotNull @Min(0) @Max(GameParticipant.MAX_SCORE)
        Integer score
) {
}
