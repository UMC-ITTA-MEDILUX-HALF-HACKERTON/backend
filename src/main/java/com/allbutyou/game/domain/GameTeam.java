package com.allbutyou.game.domain;

import com.allbutyou.room.domain.Member;
import java.time.LocalDateTime;

public enum GameTeam {
    /** 미도착이거나 arrivedAt > startAt */
    LATE,
    /** arrivedAt이 있고 arrivedAt <= startAt */
    ON_TIME;

    /**
     * 게임 시작 순간의 팀. lateMinutes(분 미만 버림)가 아니라 arrivedAt으로 판단하므로
     * 30초 늦은 멤버는 lateMinutes가 0이어도 LATE다.
     */
    public static GameTeam snapshot(Member member, LocalDateTime startAt) {
        LocalDateTime arrivedAt = member.getArrivedAt();
        return arrivedAt != null && !arrivedAt.isAfter(startAt) ? ON_TIME : LATE;
    }
}
