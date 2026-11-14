package com.allbutyou.room.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.RepeatedTest;

class RoomCodeGeneratorTest {

    private final RoomCodeGenerator generator = new RoomCodeGenerator();

    @RepeatedTest(50)
    void 영대문자와_숫자_6자리() {
        assertThat(generator.generate()).matches("^[A-Z0-9]{6}$");
    }
}
