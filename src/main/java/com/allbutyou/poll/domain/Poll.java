package com.allbutyou.poll.domain;

import com.allbutyou.room.domain.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** 방장이 만드는 투표 주제. 모임 하나에 여러 개 둘 수 있다. */
@Entity
@Table(name = "polls", indexes = @Index(name = "idx_poll_room", columnList = "room_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Poll {

    public static final int TITLE_MAX_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PollStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_option_id")
    private PollOption decidedOption;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Poll(Room room, String title) {
        this.room = Objects.requireNonNull(room);
        this.title = Objects.requireNonNull(title);
        this.status = PollStatus.OPEN;
    }

    public static Poll create(Room room, String title) {
        return new Poll(room, title);
    }

    /** 최종 결정. 다른 투표의 선택지인지는 호출하는 서비스가 먼저 검증한다. */
    public void decide(PollOption option) {
        this.decidedOption = Objects.requireNonNull(option);
        this.status = PollStatus.DECIDED;
    }

    public boolean isDecided() {
        return status == PollStatus.DECIDED;
    }

    public Long getRoomId() {
        return room.getId();
    }

    /** 프록시 초기화 없이 FK 값만 꺼낸다. */
    public Long getDecidedOptionId() {
        return decidedOption == null ? null : decidedOption.getId();
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
