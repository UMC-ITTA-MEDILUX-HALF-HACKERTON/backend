package com.allbutyou.poll.dto;

import com.allbutyou.poll.domain.PollOption;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "투표 선택지와 득표 수")
public record PollOptionResponse(
        @Schema(description = "선택지 ID", example = "1") Long optionId,
        @Schema(description = "선택지 이름", example = "볼링") String label,
        @Schema(description = "장소 이름 (없으면 null)", example = "성수 볼링장", nullable = true) String placeName,
        @Schema(description = "장소 위도 (없으면 null)", example = "37.5446", nullable = true) Double lat,
        @Schema(description = "장소 경도 (없으면 null)", example = "127.0559", nullable = true) Double lng,
        @Schema(description = "득표 수", example = "2") long voteCount
) {

    public static PollOptionResponse of(PollOption option, long voteCount) {
        return new PollOptionResponse(option.getId(), option.getLabel(), option.getPlaceName(),
                option.getLat(), option.getLng(), voteCount);
    }
}
