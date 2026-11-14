package com.allbutyou.candidate.domain;

import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "candidates", indexes = @Index(name = "idx_candidate_room", columnList = "room_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false, length = 100)
    private String placeName;

    /** 좌표 없이 장소 이름만 등록할 수도 있어 nullable. */
    private Double lat;

    private Double lng;

    @Column(nullable = false, length = 50)
    private String activity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Member createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Candidate(Room room, String placeName, Double lat, Double lng, String activity, Member createdBy) {
        this.room = Objects.requireNonNull(room);
        this.placeName = Objects.requireNonNull(placeName);
        this.lat = lat;
        this.lng = lng;
        this.activity = Objects.requireNonNull(activity);
        this.createdBy = Objects.requireNonNull(createdBy);
    }

    public static Candidate create(Room room, String placeName, Double lat, Double lng, String activity,
                                   Member createdBy) {
        return new Candidate(room, placeName, lat, lng, activity, createdBy);
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
