package com.allbutyou.room.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_room_nickname", columnNames = {"room_id", "nickname"}),
        @UniqueConstraint(name = "uk_member_token", columnNames = "token")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MemberRole role;

    @Column(nullable = false, length = 36)
    private String token;

    private LocalDateTime arrivedAt;

    private Integer lateMinutes;

    private Member(Room room, String nickname, MemberRole role) {
        this.room = Objects.requireNonNull(room);
        this.nickname = Objects.requireNonNull(nickname);
        this.role = role;
        this.token = UUID.randomUUID().toString();
    }

    public static Member host(Room room, String nickname) {
        return new Member(room, nickname, MemberRole.HOST);
    }

    public static Member member(Room room, String nickname) {
        return new Member(room, nickname, MemberRole.MEMBER);
    }

    /** 도착 처리. 지각 분 = max(0, 도착 시각 - 약속 시각), 분 단위 버림. */
    public void arrive(LocalDateTime arrivedAt) {
        this.arrivedAt = Objects.requireNonNull(arrivedAt);
        long minutes = Duration.between(room.getStartAt(), arrivedAt).toMinutes();
        this.lateMinutes = (int) Math.max(0, minutes);
    }

    public boolean isHost() {
        return role == MemberRole.HOST;
    }

    public boolean isArrived() {
        return arrivedAt != null;
    }

    public boolean belongsTo(Long roomId) {
        return room.getId() != null && room.getId().equals(roomId);
    }
}
