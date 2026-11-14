package com.allbutyou.poll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "투표(또는 변경) 요청")
public record VoteRequest(
        @Schema(description = "투표할 선택지 ID", example = "1")
        @NotNull
        Long optionId
) {
}
