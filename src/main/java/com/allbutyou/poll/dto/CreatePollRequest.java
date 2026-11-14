package com.allbutyou.poll.dto;

import com.allbutyou.poll.domain.Poll;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "투표 생성 요청")
public record CreatePollRequest(
        @Schema(description = "투표 주제", example = "1차 메뉴", maxLength = Poll.TITLE_MAX_LENGTH)
        @NotBlank @Size(max = Poll.TITLE_MAX_LENGTH)
        String title,

        @ArraySchema(arraySchema = @Schema(description = "선택지 (최소 2개, 입력 순서대로 저장)"), minItems = 2)
        @NotNull @Size(min = 2, message = "선택지는 최소 2개여야 합니다.")
        List<@NotNull @Valid PollOptionRequest> options
) {
}
