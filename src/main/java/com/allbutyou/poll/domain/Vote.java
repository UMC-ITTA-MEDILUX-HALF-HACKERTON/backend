package com.allbutyou.poll.domain;

import com.allbutyou.room.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투표당 멤버 1표. 바꾸면 행을 새로 만들지 않고 option만 바꾼다.
 * 테이블명은 #5의 votes(room_id·candidate_id NOT NULL)와 겹치지 않게 poll_votes로 둔다(ddl-auto=update 환경 대비).
 */
@Entity
@Table(name = "poll_votes", uniqueConstraints =
        @UniqueConstraint(name = "uk_poll_vote_poll_member", columnNames = {"poll_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private PollOption option;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private Vote(PollOption option, Member member) {
        this.option = Objects.requireNonNull(option);
        this.poll = option.getPoll();
        this.member = Objects.requireNonNull(member);
    }

    /** poll은 선택지의 poll을 따른다. 멤버가 해당 모임 소속인지는 호출하는 서비스 책임. */
    public static Vote of(PollOption option, Member member) {
        return new Vote(option, member);
    }

    /** 같은 투표 안의 다른 선택지로 바꾼다. */
    public void changeOption(PollOption option) {
        if (!option.belongsTo(poll)) {
            throw new IllegalArgumentException("다른 투표의 선택지로 바꿀 수 없습니다.");
        }
        this.option = option;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
