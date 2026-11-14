
package com.allbutyou.location.dto;

import com.allbutyou.location.domain.GpsStatus;
import java.time.LocalDateTime;

public record MemberLocationResponse(
        Long memberId,
        String nickname,
        Double distanceMeters,
        GpsStatus gpsStatus,
        LocalDateTime locationUpdatedAt
) {
}
