
package com.allbutyou.location.domain;

import com.allbutyou.room.domain.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_locations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 참가자 1명당 위치 정보 1개
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    private Double latitude;
    private Double longitude;
    private Double accuracyMeters;
    private Double distanceMeters;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GpsStatus gpsStatus = GpsStatus.NOT_SHARING;

    private LocalDateTime locationUpdatedAt;

    public MemberLocation(Member member) {
        this.member = member;
    }

    public void updateLocation(
            double latitude,
            double longitude,
            double accuracyMeters,
            double distanceMeters,
            GpsStatus gpsStatus
    ) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
        this.distanceMeters = distanceMeters;
        this.gpsStatus = gpsStatus;
        this.locationUpdatedAt = LocalDateTime.now();
    }
}
