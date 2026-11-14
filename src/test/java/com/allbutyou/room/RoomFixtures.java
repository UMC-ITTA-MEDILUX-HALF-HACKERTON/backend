package com.allbutyou.room;

import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.Room;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

public final class RoomFixtures {

    public static final LocalDateTime START_AT = LocalDateTime.of(2026, 11, 20, 19, 0);

    private RoomFixtures() {
    }

    public static Room room(String code) {
        return Room.create(code, "금요일 저녁 모임", "강남역 11번 출구", 37.4979, 127.0276, START_AT, 1000);
    }

    /** 영속화 없이 id를 지정해야 하는 웹 슬라이스 테스트용. */
    public static <T> T withId(T entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Member hostOf(Room room, String nickname) {
        return Member.host(room, nickname);
    }
}
