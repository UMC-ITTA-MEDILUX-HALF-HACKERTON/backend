package com.allbutyou.room.dto;

import com.allbutyou.room.domain.Room;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "모임 상세")
public record RoomDetailResponse(
        @Schema(description = "모임 ID", example = "1") Long roomId,
        @Schema(description = "참여 코드", example = "A7K2QZ") String code,
        @Schema(description = "모임 이름", example = "금요일 저녁 모임") String title,
        @Schema(description = "약속 장소 이름", example = "강남역 11번 출구") String placeName,
        @Schema(description = "약속 장소 위도", example = "37.4979") double lat,
        @Schema(description = "약속 장소 경도", example = "127.0276") double lng,
        @Schema(description = "약속 시각", example = "2026-11-20T19:00:00") LocalDateTime startAt,
        @Schema(description = "지각비", example = "1000") int lateFee,
        @Schema(description = "모임 생성 시각", example = "2026-11-14T12:00:00") LocalDateTime createdAt,
        @Schema(description = "참가자 목록 (참여 순)") List<MemberResponse> members
) {

    public static RoomDetailResponse of(Room room, List<MemberResponse> members) {
        return new RoomDetailResponse(room.getId(), room.getCode(), room.getTitle(), room.getPlaceName(),
                room.getLat(), room.getLng(), room.getStartAt(), room.getLateFee(),
                room.getCreatedAt(), members);
    }
}
