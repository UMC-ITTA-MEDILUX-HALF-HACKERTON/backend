package com.allbutyou.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "모임 생성 요청")
public record CreateRoomRequest(
        @Schema(description = "모임 이름", example = "금요일 저녁 모임")
        @NotBlank @Size(max = 50)
        String title,

        @Schema(description = "약속 장소 이름", example = "강남역 11번 출구")
        @NotBlank @Size(max = 100)
        String placeName,

        @Schema(description = "약속 장소 위도", example = "37.4979")
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        Double lat,

        @Schema(description = "약속 장소 경도", example = "127.0276")
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        Double lng,

        @Schema(description = "약속 시각 (Asia/Seoul, ISO-8601)", example = "2026-11-20T19:00:00")
        @NotNull
        LocalDateTime startAt,

        @Schema(description = "지각비 (원/분 등 정책은 game 파트에서 결정)", example = "1000")
        @NotNull @PositiveOrZero
        Integer lateFee,

        @Schema(description = "방장 닉네임", example = "민수")
        @NotBlank @Size(max = 20)
        String nickname
) {
}
