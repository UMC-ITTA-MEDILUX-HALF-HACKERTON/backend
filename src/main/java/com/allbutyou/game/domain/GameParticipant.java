package com.allbutyou.game.domain;

import com.allbutyou.room.domain.Member;
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

/** 게임 시작 시점의 참가자. 팀은 시작 순간에 고정되고, 점수는 1회만 제출한다. */
@Entity
@Table(name = "game_participants", uniqueConstraints =
        @UniqueConstraint(name = "uk_game_participant_game_member", columnNames = {"game_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameParticipant {

    public static final int MAX_SCORE = 100_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GameTeam team;

    /** 미제출이면 null. */
    private Integer score;

    private LocalDateTime submittedAt;

    private GameParticipant(Game game, Member member, GameTeam team) {
        this.game = Objects.requireNonNull(game);
        this.member = Objects.requireNonNull(member);
        this.team = Objects.requireNonNull(team);
    }

    public static GameParticipant of(Game game, Member member, GameTeam team) {
        return new GameParticipant(game, member, team);
    }

    /** 이미 제출했는지는 호출하는 서비스가 먼저 확인한다(409 응답용). */
    public void submit(int score, LocalDateTime submittedAt) {
        if (isSubmitted()) {
            throw new IllegalStateException("이미 점수를 제출했습니다.");
        }
        if (score < 0 || score > MAX_SCORE) {
            throw new IllegalArgumentException("점수는 0 이상 " + MAX_SCORE + " 이하여야 합니다.");
        }
        this.score = score;
        this.submittedAt = Objects.requireNonNull(submittedAt);
    }

    public boolean isSubmitted() {
        return score != null;
    }
}
