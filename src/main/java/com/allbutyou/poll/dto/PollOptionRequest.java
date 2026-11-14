package com.allbutyou.poll.dto;

import com.allbutyou.poll.domain.PollOption;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "투표 선택지 입력")
public record PollOptionRequest(
        @Schema(description = "선택지 이름 (메뉴·키워드 등)", example = "볼링", maxLength = PollOption.LABEL_MAX_LENGTH)
        @NotBlank @Size(max = PollOption.LABEL_MAX_LENGTH)
        String label,

        @Schema(description = "장소 이름 (선택)", example = "성수 볼링장", nullable = true,
                maxLength = PollOption.PLACE_NAME_MAX_LENGTH)
        @Size(max = PollOption.PLACE_NAME_MAX_LENGTH)
        String placeName,

        @Schema(description = "장소 위도 (lng와 함께 보내거나 둘 다 생략)", example = "37.5446", nullable = true)
        @DecimalMin("-90.0") @DecimalMax("90.0")
        Double lat,

        @Schema(description = "장소 경도 (lat와 함께 보내거나 둘 다 생략)", example = "127.0559", nullable = true)
        @DecimalMin("-180.0") @DecimalMax("180.0")
        Double lng
) {

    @Schema(hidden = true)
    @AssertTrue(message = "lat과 lng는 함께 입력해야 합니다.")
    public boolean isCoordinatePaired() {
        return (lat == null) == (lng == null);
    }
}
