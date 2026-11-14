package com.allbutyou.room.domain;

import static com.allbutyou.room.RoomFixtures.START_AT;
import static org.assertj.core.api.Assertions.assertThat;

import com.allbutyou.room.RoomFixtures;
import org.junit.jupiter.api.Test;

class MemberTest {

    private final Room room = RoomFixtures.room("ABC123");

    @Test
    void 약속시간_이후_도착하면_지각_분을_계산한다() {
        Member member = Member.member(room, "지영");
        member.arrive(START_AT.plusMinutes(12));

        assertThat(member.getArrivedAt()).isEqualTo(START_AT.plusMinutes(12));
        assertThat(member.getLateMinutes()).isEqualTo(12);
        assertThat(member.isArrived()).isTrue();
    }

    @Test
    void 약속시간보다_5분_늦으면_5분_지각이다() {
        Member member = Member.member(room, "지영");
        member.arrive(START_AT.plusMinutes(5));
        assertThat(member.getLateMinutes()).isEqualTo(5);
    }

    @Test
    void 이미_도착했어도_다시_호출하면_시각과_지각분이_갱신된다() {
        Member member = Member.member(room, "지영");
        member.arrive(START_AT.plusMinutes(20));
        member.arrive(START_AT.plusMinutes(3));

        assertThat(member.getArrivedAt()).isEqualTo(START_AT.plusMinutes(3));
        assertThat(member.getLateMinutes()).isEqualTo(3);
    }

    @Test
    void 일찍_도착하면_지각은_0분이다() {
        Member member = Member.member(room, "지영");
        member.arrive(START_AT.minusMinutes(30));
        assertThat(member.getLateMinutes()).isZero();
    }

    @Test
    void 정각_도착은_0분이다() {
        Member member = Member.member(room, "지영");
        member.arrive(START_AT);
        assertThat(member.getLateMinutes()).isZero();
    }

    @Test
    void 분_미만은_버린다() {
        Member member = Member.member(room, "지영");
        member.arrive(START_AT.plusMinutes(4).plusSeconds(59));
        assertThat(member.getLateMinutes()).isEqualTo(4);
    }

    @Test
    void 도착_전에는_도착시각과_지각분이_null이다() {
        Member member = Member.member(room, "지영");
        assertThat(member.getArrivedAt()).isNull();
        assertThat(member.getLateMinutes()).isNull();
        assertThat(member.isArrived()).isFalse();
    }

    @Test
    void 역할과_토큰() {
        Member host = Member.host(room, "민수");
        Member member = Member.member(room, "지영");

        assertThat(host.isHost()).isTrue();
        assertThat(member.isHost()).isFalse();
        assertThat(host.getToken()).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        assertThat(host.getToken()).isNotEqualTo(member.getToken());
    }
}
