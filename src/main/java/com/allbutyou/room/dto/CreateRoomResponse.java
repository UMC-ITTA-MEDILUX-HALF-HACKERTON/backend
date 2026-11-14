package com.allbutyou.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모임 생성 결과")
public record CreateRoomResponse(
        @Schema(description = "모임 ID", example = "1") Long roomId,
        @Schema(description = "참여 코드 (영대문자+숫자 6자리)", example = "A7K2QZ") String code,
        @Schema(description = "방장 멤버 ID", example = "1") Long memberId,
        @Schema(description = "인증 토큰. 이후 요청 헤더 X-Participant-Token에 담는다",
                example = "3f2b8c1e-7a4d-4e8b-9c2a-1d5e6f7a8b9c") String token
) {
}
