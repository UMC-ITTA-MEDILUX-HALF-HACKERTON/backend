
package com.allbutyou.location.service;

import com.allbutyou.location.domain.GpsStatus;
import com.allbutyou.location.domain.MemberLocation;
import com.allbutyou.location.dto.LocationResponse;
import com.allbutyou.location.dto.LocationUpdateRequest;
import com.allbutyou.location.dto.MemberLocationResponse;
import com.allbutyou.location.repository.MemberLocationRepository;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.Room;
import com.allbutyou.room.repository.MemberRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private final MemberRepository memberRepository;
    private final MemberLocationRepository locationRepository;

    public LocationService(
            MemberRepository memberRepository,
            MemberLocationRepository locationRepository
    ) {
        this.memberRepository = memberRepository;
        this.locationRepository = locationRepository;
    }

    // GPS 위치 갱신
    @Transactional
    public LocationResponse updateLocation(
            Long roomId,
            Long memberId,
            String token,
            LocationUpdateRequest request
    ) {
        // 1. 참가자 토큰 인증
        Member member = memberRepository.findByToken(token)
                .orElseThrow(() ->
                        new IllegalArgumentException("유효하지 않은 참가자 토큰입니다.")
                );

        // 2. 본인 확인
        if (!member.getId().equals(memberId)) {
            throw new IllegalArgumentException(
                    "본인의 위치만 갱신할 수 있습니다."
            );
        }

        // 3. 모임 소속 확인
        if (!member.belongsTo(roomId)) {
            throw new IllegalArgumentException(
                    "해당 모임의 참가자가 아닙니다."
            );
        }

        // 4. GPS 입력값 검증
        double latitude = request.latitude();
        double longitude = request.longitude();
        double accuracy = request.accuracyMeters();

        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180
                || !Double.isFinite(accuracy) || accuracy < 0) {
            throw new IllegalArgumentException(
                    "유효하지 않은 GPS 좌표 또는 정확도입니다."
            );
        }

        // 5. DB에서 약속 장소 좌표 조회
        Room room = member.getRoom();

        // 6. 약속 장소까지 거리 계산
        double distance = DistanceCalculator.calculate(
                latitude,
                longitude,
                room.getLat(),
                room.getLng()
        );

        // 7. 100m 이내 여부 판정
        GpsStatus status = distance <= 100
                ? GpsStatus.NEARBY_GPS_OFF
                : GpsStatus.SHARING;

        // 8. 기존 GPS 정보 조회 또는 새로 생성
        MemberLocation location = locationRepository
                .findByMemberId(memberId)
                .orElseGet(() -> new MemberLocation(member));

        // 9. GPS 정보 갱신
        location.updateLocation(
                latitude,
                longitude,
                accuracy,
                distance,
                status
        );

        // 10. DB 저장
        locationRepository.save(location);

        // 11. 결과 반환
        return new LocationResponse(
                memberId,
                distance,
                status,
                OffsetDateTime.now()
        );
    }

    // 모임 참가자 전체 GPS 현황 조회
    @Transactional(readOnly = true)
    public List<MemberLocationResponse> getRoomLocations(
            Long roomId,
            String token
    ) {
        // 1. 요청한 참가자 인증
        Member requester = memberRepository.findByToken(token)
                .orElseThrow(() ->
                        new IllegalArgumentException("유효하지 않은 참가자 토큰입니다.")
                );

        // 2. 해당 모임 참가자인지 확인
        if (!requester.belongsTo(roomId)) {
            throw new IllegalArgumentException(
                    "해당 모임의 참가자가 아닙니다."
            );
        }

        // 3. 모임 참가자 전체 조회
        List<Member> members =
                memberRepository.findAllByRoomIdOrderByIdAsc(roomId);

        // 4. 저장된 GPS 정보 조회
        Map<Long, MemberLocation> locationMap =
                locationRepository.findAllByMemberRoomId(roomId)
                        .stream()
                        .collect(Collectors.toMap(
                                location -> location.getMember().getId(),
                                Function.identity()
                        ));

        // 5. 참가자별 GPS 현황 응답 생성
        return members.stream()
                .map(member -> {
                    MemberLocation location =
                            locationMap.get(member.getId());

                    // 위치 공유 기록이 없는 참가자
                    if (location == null) {
                        return new MemberLocationResponse(
                                member.getId(),
                                member.getNickname(),
                                null,
                                GpsStatus.NOT_SHARING,
                                null
                        );
                    }

                    // 위치 공유 기록이 있는 참가자
                    return new MemberLocationResponse(
                            member.getId(),
                            member.getNickname(),
                            location.getDistanceMeters(),
                            location.getGpsStatus(),
                            location.getLocationUpdatedAt()
                    );
                })
                .toList();
    }
}
