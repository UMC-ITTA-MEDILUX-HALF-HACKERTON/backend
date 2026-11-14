package com.allbutyou.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.allbutyou.common.exception.BusinessException;
import com.allbutyou.common.exception.ErrorCode;
import com.allbutyou.room.RoomFixtures;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.MemberRole;
import com.allbutyou.room.domain.Room;
import com.allbutyou.room.dto.CreateRoomRequest;
import com.allbutyou.room.dto.CreateRoomResponse;
import com.allbutyou.room.dto.JoinRoomRequest;
import com.allbutyou.room.dto.JoinRoomResponse;
import com.allbutyou.room.dto.MemberResponse;
import com.allbutyou.room.dto.RoomDetailResponse;
import com.allbutyou.room.repository.MemberRepository;
import com.allbutyou.room.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@DataJpaTest
@Import({RoomService.class, RoomCodeGenerator.class})
@RecordApplicationEvents
class RoomServiceTest {

    @Autowired RoomService roomService;
    @Autowired RoomRepository roomRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired ApplicationEvents events;

    private CreateRoomResponse createRoom(String hostNickname) {
        return roomService.createRoom(new CreateRoomRequest(
                " 금요일 저녁 모임 ", "강남역", 37.4979, 127.0276, RoomFixtures.START_AT, 1000, hostNickname));
    }

    @Test
    void 모임_생성시_코드를_발급하고_생성자를_HOST로_등록한다() {
        CreateRoomResponse res = createRoom("민수");

        assertThat(res.code()).matches("^[A-Z0-9]{6}$");
        Room room = roomService.getRoom(res.roomId());
        assertThat(room.getTitle()).isEqualTo("금요일 저녁 모임");
        assertThat(room.getCreatedAt()).isNotNull();

        Member host = roomService.getMember(res.memberId());
        assertThat(host.getRole()).isEqualTo(MemberRole.HOST);
        assertThat(host.getToken()).isEqualTo(res.token());
        assertThat(memberRepository.findByToken(res.token())).isPresent();
    }

    @Test
    void 참여하면_MEMBER로_등록되고_참가자_목록_이벤트를_발행한다() {
        CreateRoomResponse room = createRoom("민수");

        JoinRoomResponse res = roomService.join(new JoinRoomRequest(room.code().toLowerCase(), " 지영 "));

        assertThat(res.roomId()).isEqualTo(room.roomId());
        Member member = roomService.getMember(res.memberId());
        assertThat(member.getRole()).isEqualTo(MemberRole.MEMBER);
        assertThat(member.getNickname()).isEqualTo("지영");

        assertThat(events.stream(MemberJoinedEvent.class)).singleElement().satisfies(e -> {
            assertThat(e.message().roomId()).isEqualTo(room.roomId());
            assertThat(e.message().members()).extracting(MemberResponse::nickname).containsExactly("민수", "지영");
        });
    }

    @Test
    void 같은_모임에서_닉네임이_중복되면_DUPLICATE() {
        CreateRoomResponse room = createRoom("민수");

        assertThatThrownBy(() -> roomService.join(new JoinRoomRequest(room.code(), "민수")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE);
        assertThat(events.stream(MemberJoinedEvent.class)).isEmpty();
    }

    @Test
    void 다른_모임이면_같은_닉네임을_쓸_수_있다() {
        createRoom("민수");
        CreateRoomResponse other = createRoom("지영");

        JoinRoomResponse res = roomService.join(new JoinRoomRequest(other.code(), "민수"));
        assertThat(res.roomId()).isEqualTo(other.roomId());
    }

    @Test
    void 없는_코드로_참여하면_NOT_FOUND() {
        assertThatThrownBy(() -> roomService.join(new JoinRoomRequest("ZZZZZZ", "지영")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void UK_room_id_nickname이_DB에서도_적용된다() {
        Room room = roomRepository.save(RoomFixtures.room("ABC123"));
        memberRepository.saveAndFlush(Member.host(room, "민수"));

        assertThatThrownBy(() -> memberRepository.saveAndFlush(Member.member(room, "민수")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 모임_상세는_참가자_목록을_참여순으로_돌려준다() {
        CreateRoomResponse room = createRoom("민수");
        roomService.join(new JoinRoomRequest(room.code(), "지영"));
        Member host = memberRepository.findByToken(room.token()).orElseThrow();

        RoomDetailResponse detail = roomService.getRoomDetail(room.roomId(), host);

        assertThat(detail.code()).isEqualTo(room.code());
        assertThat(detail.startAt()).isEqualTo(RoomFixtures.START_AT);
        assertThat(detail.members()).extracting(MemberResponse::nickname, MemberResponse::role)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("민수", MemberRole.HOST),
                        org.assertj.core.groups.Tuple.tuple("지영", MemberRole.MEMBER));
    }

    @Test
    void 다른_모임_참가자는_상세를_볼_수_없다() {
        CreateRoomResponse roomA = createRoom("민수");
        CreateRoomResponse roomB = createRoom("지영");
        Member memberOfB = memberRepository.findByToken(roomB.token()).orElseThrow();

        assertThatThrownBy(() -> roomService.getRoomDetail(roomA.roomId(), memberOfB))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void 조회_메서드는_없으면_NOT_FOUND() {
        assertThatThrownBy(() -> roomService.getRoom(999L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> roomService.getMember(999L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> roomService.getMembers(999L)).isInstanceOf(BusinessException.class);
    }
}
