package com.allbutyou.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모임 참여 결과")
public record JoinRoomResponse(
        @Schema(description = "모임 ID", example = "1") Long roomId,
        @Schema(description = "멤버 ID", example = "2") Long memberId,
        @Schema(description = "인증 토큰. 이후 요청 헤더 X-Participant-Token에 담는다",
                example = "9a8b7c6d-5e4f-4a3b-2c1d-0e9f8a7b6c5d") String token
) {
}
