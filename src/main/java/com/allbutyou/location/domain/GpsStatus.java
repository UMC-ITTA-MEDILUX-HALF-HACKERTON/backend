package com.allbutyou.location.domain;

public enum GpsStatus {
    SHARING,          // 위치 공유 중
    NEARBY_GPS_OFF,   // 약속 장소 100m 이내
    NOT_SHARING       // 위치 미공유
}