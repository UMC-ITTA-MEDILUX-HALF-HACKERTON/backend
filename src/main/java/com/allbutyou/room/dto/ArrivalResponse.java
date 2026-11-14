package com.allbutyou.room.dto;

import com.allbutyou.room.domain.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 도착 처리 응답이자 WebSocket ARRIVED 페이로드. */
@Schema(description = "도착 처리 결과")
public record ArrivalResponse(
        @Schema(description = "멤버 ID", example = "2") Long memberId,
        @Schema(description = "닉네임", example = "지영") String nickname,
        @Schema(description = "도착 시각", example = "2026-11-14T13:10:00") LocalDateTime arrivedAt,
        @Schema(description = "지각 분 = max(0, 도착 시각 - 약속 시각), 분 미만 버림", example = "10") int lateMinutes
) {

    public static ArrivalResponse from(Member member) {
        return new ArrivalResponse(member.getId(), member.getNickname(), member.getArrivedAt(), member.getLateMinutes());
    }
}
