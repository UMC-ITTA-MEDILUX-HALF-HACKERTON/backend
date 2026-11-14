package com.allbutyou.game.domain;

import com.allbutyou.room.domain.Room;
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
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 모임당 1개. 방장이 약속 시각 이후에 시작한다. */
@Entity
@Table(name = "games", uniqueConstraints = @UniqueConstraint(name = "uk_game_room", columnNames = "room_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GameStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private Game(Room room, LocalDateTime startedAt) {
        this.room = Objects.requireNonNull(room);
        this.startedAt = Objects.requireNonNull(startedAt);
        this.status = GameStatus.PLAYING;
    }

    public static Game start(Room room, LocalDateTime startedAt) {
        return new Game(room, startedAt);
    }

    public void finish(LocalDateTime finishedAt) {
        if (isFinished()) {
            throw new IllegalStateException("이미 종료된 게임입니다.");
        }
        this.finishedAt = Objects.requireNonNull(finishedAt);
        this.status = GameStatus.FINISHED;
    }

    public boolean isFinished() {
        return status == GameStatus.FINISHED;
    }
}
