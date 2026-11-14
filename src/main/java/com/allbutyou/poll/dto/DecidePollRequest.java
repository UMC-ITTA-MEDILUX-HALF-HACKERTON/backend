package com.allbutyou.poll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "최종 결정 요청")
public record DecidePollRequest(
        @Schema(description = "최종 결정할 선택지 ID", example = "1")
        @NotNull
        Long optionId
) {
}
