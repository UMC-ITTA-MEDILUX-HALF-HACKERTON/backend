package com.allbutyou.location.dto;

public record LocationUpdateRequest(
        double latitude,
        double longitude,
        double accuracyMeters
) {
}