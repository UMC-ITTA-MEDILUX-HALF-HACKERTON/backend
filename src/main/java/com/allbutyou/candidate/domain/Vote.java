package com.allbutyou.candidate.domain;

import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 멤버당 모임 내 1표. 투표 API는 #7에서 구현한다. */
@Entity
@Table(name = "votes", uniqueConstraints =
        @UniqueConstraint(name = "uk_vote_room_member", columnNames = {"room_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Vote(Candidate candidate, Member member) {
        this.candidate = Objects.requireNonNull(candidate);
        this.room = candidate.getRoom();
        this.member = Objects.requireNonNull(member);
    }

    /** room은 후보의 room을 따른다. 멤버와 후보의 room 일치 검증은 호출하는 서비스 책임. */
    public static Vote of(Candidate candidate, Member member) {
        return new Vote(candidate, member);
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
