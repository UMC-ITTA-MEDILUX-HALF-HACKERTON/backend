package com.allbutyou.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "모임 참여 요청")
public record JoinRoomRequest(
        @Schema(description = "참여 코드 (대소문자 무시)", example = "A7K2QZ")
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{6}$", message = "참여 코드는 영문/숫자 6자리입니다.")
        String code,

        @Schema(description = "닉네임 (모임 내 중복 불가)", example = "지영")
        @NotBlank @Size(max = 20)
        String nickname
) {
}
