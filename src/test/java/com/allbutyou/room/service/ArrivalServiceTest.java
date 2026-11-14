package com.allbutyou.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.allbutyou.common.exception.BusinessException;
import com.allbutyou.common.exception.ErrorCode;
import com.allbutyou.room.RoomFixtures;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.Room;
import com.allbutyou.room.dto.ArrivalResponse;
import com.allbutyou.room.repository.MemberRepository;
import com.allbutyou.room.repository.RoomRepository;
import java.time.LocalDateTime;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@DataJpaTest
@Import({ArrivalService.class, RoomService.class, RoomCodeGenerator.class})
@RecordApplicationEvents
class ArrivalServiceTest {

    @Autowired ArrivalService arrivalService;
    @Autowired RoomRepository roomRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired ApplicationEvents events;

    private Room room;
    private Member host;
    private Member member;

    @BeforeEach
    void setUp() {
        room = roomRepository.save(RoomFixtures.room("ABC123"));
        host = memberRepository.save(Member.host(room, "민수"));
        member = memberRepository.save(Member.member(room, "지영"));
    }

    @Test
    void 주최자가_도착_처리하면_지각분을_계산하고_ARRIVED_이벤트를_발행한다() {
        LocalDateTime arrivedAt = RoomFixtures.START_AT.plusMinutes(5);

        ArrivalResponse res = arrivalService.markArrived(room.getId(), member.getId(), host, arrivedAt);

        assertThat(res.memberId()).isEqualTo(member.getId());
        assertThat(res.nickname()).isEqualTo("지영");
        assertThat(res.arrivedAt()).isEqualTo(arrivedAt);
        assertThat(res.lateMinutes()).isEqualTo(5);
        assertThat(memberRepository.findById(member.getId()).orElseThrow().getLateMinutes()).isEqualTo(5);
        assertThat(events.stream(MemberArrivedEvent.class)).singleElement().satisfies(e -> {
            assertThat(e.roomId()).isEqualTo(room.getId());
            assertThat(e.arrival()).isEqualTo(res);
        });
    }

    @Test
    void arrivedAt이_null이면_현재_시각으로_처리한다() {
        LocalDateTime before = LocalDateTime.now();

        ArrivalResponse res = arrivalService.markArrived(room.getId(), member.getId(), host, null);

        assertThat(res.arrivedAt()).isBetween(before, LocalDateTime.now());
    }

    @Test
    void 이미_도착한_멤버도_시각을_수정할_수_있다() {
        arrivalService.markArrived(room.getId(), member.getId(), host, RoomFixtures.START_AT.plusMinutes(30));

        ArrivalResponse res = arrivalService.markArrived(room.getId(), member.getId(), host,
                RoomFixtures.START_AT.minusMinutes(1));

        assertThat(res.arrivedAt()).isEqualTo(RoomFixtures.START_AT.minusMinutes(1));
        assertThat(res.lateMinutes()).isZero();
    }

    @Test
    void 주최자가_아니면_FORBIDDEN() {
        assertError(() -> arrivalService.markArrived(room.getId(), host.getId(), member, null), ErrorCode.FORBIDDEN);
        assertThat(events.stream(MemberArrivedEvent.class)).isEmpty();
    }

    @Test
    void 다른_모임의_주최자면_FORBIDDEN() {
        Room other = roomRepository.save(RoomFixtures.room("XYZ789"));
        Member otherHost = memberRepository.save(Member.host(other, "철수"));

        assertError(() -> arrivalService.markArrived(room.getId(), member.getId(), otherHost, null), ErrorCode.FORBIDDEN);
    }

    @Test
    void 대상_멤버가_해당_모임_소속이_아니면_NOT_FOUND() {
        Room other = roomRepository.save(RoomFixtures.room("XYZ789"));
        Member outsider = memberRepository.save(Member.member(other, "철수"));

        assertError(() -> arrivalService.markArrived(room.getId(), outsider.getId(), host, null), ErrorCode.NOT_FOUND);
        assertError(() -> arrivalService.markArrived(room.getId(), 999L, host, null), ErrorCode.NOT_FOUND);
    }

    private void assertError(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(code);
    }
}
