package com.allbutyou.location.dto;

import com.allbutyou.location.domain.GpsStatus;
import java.time.OffsetDateTime;

public record LocationResponse(
        Long memberId,
        double distanceMeters,
        GpsStatus gpsStatus,
        OffsetDateTime locationUpdatedAt
) {
}