package com.allbutyou.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "도착 처리 요청")
public record ArrivalRequest(
        @Schema(description = "실제 도착 시각 (null이면 서버 현재 시각)", example = "2026-11-14T13:10:00", nullable = true)
        LocalDateTime arrivedAt
) {
}
