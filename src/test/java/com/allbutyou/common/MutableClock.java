package com.allbutyou.common;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** 테스트에서 현재 시각을 고정하고 필요할 때 옮길 수 있는 Clock. */
public class MutableClock extends Clock {

    private final ZoneId zone;
    private Instant instant;

    public MutableClock(LocalDateTime now, ZoneId zone) {
        this.zone = zone;
        this.instant = now.atZone(zone).toInstant();
    }

    public void setNow(LocalDateTime now) {
        this.instant = now.atZone(zone).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(LocalDateTime.ofInstant(instant, this.zone), zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
