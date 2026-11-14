package com.allbutyou.location.service;

import com.allbutyou.location.domain.GpsStatus;
import com.allbutyou.location.dto.LocationResponse;
import com.allbutyou.location.dto.LocationUpdateRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class LocationService {

    public LocationResponse updateLocation(
            Long memberId,
            LocationUpdateRequest request,
            double meetingLatitude,
            double meetingLongitude
    ) {
        // 1. 참가자 현재 위치와 약속 장소 사이의 거리 계산
        double distance = DistanceCalculator.calculate(
                request.latitude(),
                request.longitude(),
                meetingLatitude,
                meetingLongitude
        );

        // 2. 100m 이내인지 판정
        GpsStatus status = distance <= 100
                ? GpsStatus.NEARBY_GPS_OFF
                : GpsStatus.SHARING;

        // 3. 프론트에 전달할 응답 생성
        return new LocationResponse(
                memberId,
                distance,
                status,
                OffsetDateTime.now()
        );
    }
}