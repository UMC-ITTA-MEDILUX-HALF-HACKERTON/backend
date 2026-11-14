package com.allbutyou.room.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(name = "uk_room_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room {

    public static final int CODE_LENGTH = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = CODE_LENGTH)
    private String code;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, length = 100)
    private String placeName;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private int lateFee;

    private Long decidedCandidateId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Room(String code, String title, String placeName, double lat, double lng,
                 LocalDateTime startAt, int lateFee) {
        this.code = code;
        this.title = title;
        this.placeName = placeName;
        this.lat = lat;
        this.lng = lng;
        this.startAt = startAt;
        this.lateFee = lateFee;
    }

    public static Room create(String code, String title, String placeName, double lat, double lng,
                              LocalDateTime startAt, int lateFee) {
        return new Room(code, title, placeName, lat, lng, startAt, lateFee);
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
