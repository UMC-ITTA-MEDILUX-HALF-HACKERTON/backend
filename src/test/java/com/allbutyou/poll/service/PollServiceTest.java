package com.allbutyou.poll.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.allbutyou.common.exception.BusinessException;
import com.allbutyou.common.exception.ErrorCode;
import com.allbutyou.poll.domain.Poll;
import com.allbutyou.poll.domain.PollOption;
import com.allbutyou.poll.domain.PollStatus;
import com.allbutyou.poll.domain.Vote;
import com.allbutyou.poll.dto.CreatePollRequest;
import com.allbutyou.poll.dto.NotVotedMemberResponse;
import com.allbutyou.poll.dto.PollOptionRequest;
import com.allbutyou.poll.dto.PollOptionResponse;
import com.allbutyou.poll.dto.PollResponse;
import com.allbutyou.poll.repository.PollOptionRepository;
import com.allbutyou.poll.repository.PollRepository;
import com.allbutyou.poll.repository.VoteRepository;
import com.allbutyou.room.RoomFixtures;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.Room;
import com.allbutyou.room.repository.MemberRepository;
import com.allbutyou.room.repository.RoomRepository;
import com.allbutyou.room.service.RoomCodeGenerator;
import com.allbutyou.room.service.RoomService;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@DataJpaTest
@Import({PollService.class, RoomService.class, RoomCodeGenerator.class})
@RecordApplicationEvents
class PollServiceTest {

    @Autowired PollService pollService;
    @Autowired RoomRepository roomRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired PollRepository pollRepository;
    @Autowired PollOptionRepository optionRepository;
    @Autowired VoteRepository voteRepository;
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

    // ---- 생성 ----

    @Test
    void HOST가_투표를_만들면_선택지와_함께_저장하고_POLL_UPDATE를_발행한다() {
        PollResponse res = pollService.create(room.getId(), host, new CreatePollRequest(" 1차 메뉴 ", List.of(
                option(" 삼겹살 "),
                new PollOptionRequest("볼링", " 성수 볼링장 ", 37.5, 127.0))));

        assertThat(res.pollId()).isNotNull();
        assertThat(res.title()).isEqualTo("1차 메뉴");
        assertThat(res.status()).isEqualTo(PollStatus.OPEN);
        assertThat(res.decidedOptionId()).isNull();
        assertThat(res.options())
                .extracting(PollOptionResponse::label, PollOptionResponse::placeName, PollOptionResponse::lat,
                        PollOptionResponse::voteCount)
                .containsExactly(tuple("삼겹살", null, null, 0L), tuple("볼링", "성수 볼링장", 37.5, 0L));
        assertThat(res.votedCount()).isZero();
        assertThat(res.notVotedMembers()).extracting(NotVotedMemberResponse::memberId)
                .containsExactly(host.getId(), member.getId());

        Poll saved = pollRepository.findById(res.pollId()).orElseThrow();
        assertThat(saved.getRoomId()).isEqualTo(room.getId());
        assertThat(saved.getCreatedAt()).isNotNull();

        assertThat(events.stream(PollUpdatedEvent.class)).singleElement().satisfies(e -> {
            assertThat(e.roomId()).isEqualTo(room.getId());
            assertThat(e.poll().pollId()).isEqualTo(res.pollId());
            assertThat(e.poll().myOptionId()).isNull();
        });
    }

    @Test
    void HOST가_아닌_참가자가_투표를_만들면_FORBIDDEN() {
        assertError(() -> pollService.create(room.getId(), member, menuPollRequest()), ErrorCode.FORBIDDEN);

        assertThat(pollRepository.count()).isZero();
        assertThat(events.stream(PollUpdatedEvent.class)).isEmpty();
    }

    @Test
    void 다른_모임의_HOST가_투표를_만들면_FORBIDDEN() {
        Member outsiderHost = outsider();

        assertError(() -> pollService.create(room.getId(), outsiderHost, menuPollRequest()), ErrorCode.FORBIDDEN);
    }

    // ---- 선택지 추가 ----

    @Test
    void HOST가_선택지를_추가한다() {
        Poll poll = poll("1차 메뉴", "삼겹살", "치킨");

        PollResponse res = pollService.addOption(poll.getId(), host, new PollOptionRequest("피자", "  ", null, null));

        assertThat(res.options()).extracting(PollOptionResponse::label).containsExactly("삼겹살", "치킨", "피자");
        assertThat(res.options().get(2).placeName()).isNull();
        assertThat(events.stream(PollUpdatedEvent.class)).hasSize(1);
    }

    @Test
    void HOST가_아니면_선택지를_추가할_수_없다() {
        Poll poll = poll("1차 메뉴", "삼겹살", "치킨");

        assertError(() -> pollService.addOption(poll.getId(), member, option("피자")), ErrorCode.FORBIDDEN);
    }

    @Test
    void 결정된_투표에는_선택지를_추가할_수_없다() {
        Poll poll = poll("1차 메뉴", "삼겹살", "치킨");
        pollService.decide(poll.getId(), host, firstOption(poll).getId());

        assertError(() -> pollService.addOption(poll.getId(), host, option("피자")), ErrorCode.INVALID_INPUT);
    }

    // ---- 투표 ----

    @Test
    void 처음_투표하면_생성하고_다시_투표하면_변경한다() {
        Poll poll = poll("1차 메뉴", "삼겹살", "치킨");
        List<PollOption> options = optionRepository.findAllByPollIds(List.of(poll.getId()));
        Long pork = options.get(0).getId();
        Long chicken = options.get(1).getId();

        PollResponse first = pollService.vote(poll.getId(), member, pork);

        assertThat(first.myOptionId()).isEqualTo(pork);
        assertThat(first.votedCount()).isEqualTo(1);
        assertThat(first.options()).extracting(PollOptionResponse::voteCount).containsExactly(1L, 0L);

        PollResponse changed = pollService.vote(poll.getId(), member, chicken);

        assertThat(changed.myOptionId()).isEqualTo(chicken);
        assertThat(changed.votedCount()).isEqualTo(1);
        assertThat(changed.options()).extracting(PollOptionResponse::voteCount).containsExactly(0L, 1L);
        assertThat(voteRepository.count()).isEqualTo(1);
        assertThat(voteRepository.findByPollIdAndMemberId(poll.getId(), member.getId()))
                .get().extracting(v -> v.getOption().getId()).isEqualTo(chicken);

        assertThat(events.stream(PollUpdatedEvent.class)).hasSize(2)
                .allSatisfy(e -> assertThat(e.poll().myOptionId()).isNull());
    }

    @Test
    void 다른_투표의_선택지로_투표하면_INVALID_INPUT() {
        Poll menu = poll("1차 메뉴", "삼겹살", "치킨");
        Poll activity = poll("2차", "볼링", "노래방");

        assertError(() -> pollService.vote(menu.getId(), member, firstOption(activity).getId()),
                ErrorCode.INVALID_INPUT);
        assertThat(voteRepository.count()).isZero();
        assertThat(events.stream(PollUpdatedEvent.class)).isEmpty();
    }

    @Test
    void 이미_투표한_뒤_다른_투표의_선택지로_바꾸려_해도_INVALID_INPUT() {
        Poll menu = poll("1차 메뉴", "삼겹살", "치킨");
        Poll activity = poll("2차", "볼링", "노래방");
        pollService.vote(menu.getId(), member, firstOption(menu).getId());

        assertError(() -> pollService.vote(menu.getId(), member, firstOption(activity).getId()),
                ErrorCode.INVALID_INPUT);
    }

    @Test
    void 결정된_투표에는_투표할_수_없다() {
        Poll poll = poll("1차 메뉴", "삼겹살", "치킨");
        Long pork = firstOption(poll).getId();
        pollService.decide(poll.getId(), host, pork);

        assertError(() -> pollService.vote(poll.getId(), member, pork), ErrorCode.INVALID_INPUT);
        assertThat(voteRepository.count()).isZero();
    }

    @Test
    void 다른_모임_참가자가_투표하면_FORBIDDEN() {
        Poll poll = poll("1차 메뉴", "삼겹살", "치킨");
        Member outsider = outsider();

        assertError(() -> pollService.vote(poll.getId(), outsider, firstOption(poll).getId()), ErrorCode.FORBIDDEN);
    }

    @Test
    void 없는_투표면_NOT_FOUND() {
        assertError(() -> pollService.vote(999L, member, 1L), ErrorCode.NOT_FOUND);
    }

    // ---- 결정 ----

    @Test
    void HOST가_결정하면_DECIDED가_되고_POLL_DECIDED를_발행한다() {
        Poll poll = poll("1차 메뉴", "삼겹살", "치킨");
        Long pork = firstOption(poll).getId();
        pollService.vote(poll.getId(), host, pork);

        PollResponse res = pollService.decide(poll.getId(), host, pork);

        assertThat(res.status()).isEqualTo(PollStatus.DECIDED);
        assertThat(res.decidedOptionId()).isEqualTo(pork);
        assertThat(res.myOptionId()).isEqualTo(pork);
        Poll saved = pollRepository.findById(poll.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(PollStatus.DECIDED);
        assertThat(saved.getDecidedOptionId()).isEqualTo(pork);

        assertThat(events.stream(PollDecidedEvent.class)).singleElement().satisfies(e -> {
            assertThat(e.roomId()).isEqualTo(room.getId());
            assertThat(e.poll().decidedOptionId()).isEqualTo(pork);
            assertThat(e.poll().myOptionId()).isNull();
        });
    }

    @Test
    void HOST가_아니면_결정할_수_없다() {
        Poll poll = poll("1차 메뉴", "삼겹살", "치킨");

        assertError(() -> pollService.decide(poll.getId(), member, firstOption(poll).getId()), ErrorCode.FORBIDDEN);
        assertThat(pollRepository.findById(poll.getId()).orElseThrow().isDecided()).isFalse();
    }

    @Test
    void 다른_투표의_선택지로_결정하면_INVALID_INPUT() {
        Poll menu = poll("1차 메뉴", "삼겹살", "치킨");
        Poll activity = poll("2차", "볼링", "노래방");

        assertError(() -> pollService.decide(menu.getId(), host, firstOption(activity).getId()),
                ErrorCode.INVALID_INPUT);
    }

    // ---- 조회 ----

    @Test
    void 투표별로_득표_수와_미응답_인원과_내_선택을_계산한다() {
        Member third = memberRepository.save(Member.member(room, "철수"));
        Member fourth = memberRepository.save(Member.member(room, "영희"));
        Poll menu = poll("1차 메뉴", "삼겹살", "치킨", "피자");
        Poll activity = poll("2차", "볼링", "노래방");
        List<PollOption> menuOptions = optionRepository.findAllByPollIds(List.of(menu.getId()));
        PollOption pork = menuOptions.get(0);
        PollOption chicken = menuOptions.get(1);
        PollOption pizza = menuOptions.get(2);
        PollOption bowling = firstOption(activity);

        voteRepository.save(Vote.of(pork, host));
        voteRepository.save(Vote.of(pork, third));
        voteRepository.save(Vote.of(chicken, member));
        voteRepository.save(Vote.of(bowling, fourth));

        // 다른 모임의 투표는 섞이지 않아야 한다.
        Room other = roomRepository.save(RoomFixtures.room("XYZ789"));
        Member otherHost = memberRepository.save(Member.host(other, "외부인"));
        Poll otherPoll = pollRepository.save(Poll.create(other, "다른 투표"));
        PollOption otherOption = optionRepository.save(PollOption.create(otherPoll, "밥", null, null, null));
        voteRepository.save(Vote.of(otherOption, otherHost));

        List<PollResponse> res = pollService.getPolls(room.getId(), member);

        assertThat(res).extracting(PollResponse::pollId).containsExactly(menu.getId(), activity.getId());

        PollResponse menuRes = res.get(0);
        assertThat(menuRes.options()).extracting(PollOptionResponse::optionId, PollOptionResponse::voteCount)
                .containsExactly(tuple(pork.getId(), 2L), tuple(chicken.getId(), 1L), tuple(pizza.getId(), 0L));
        assertThat(menuRes.votedCount()).isEqualTo(3);
        assertThat(menuRes.notVotedMembers())
                .extracting(NotVotedMemberResponse::memberId, NotVotedMemberResponse::nickname)
                .containsExactly(tuple(fourth.getId(), "영희"));
        assertThat(menuRes.myOptionId()).isEqualTo(chicken.getId());

        PollResponse activityRes = res.get(1);
        assertThat(activityRes.votedCount()).isEqualTo(1);
        assertThat(activityRes.notVotedMembers()).extracting(NotVotedMemberResponse::memberId)
                .containsExactly(host.getId(), member.getId(), third.getId());
        assertThat(activityRes.myOptionId()).isNull();
    }

    @Test
    void 투표가_없으면_빈_목록() {
        assertThat(pollService.getPolls(room.getId(), member)).isEmpty();
    }

    @Test
    void 다른_모임_참가자가_조회하면_FORBIDDEN() {
        Member outsider = outsider();

        assertError(() -> pollService.getPolls(room.getId(), outsider), ErrorCode.FORBIDDEN);
    }

    // ---- helpers ----

    private Poll poll(String title, String... labels) {
        Poll poll = pollRepository.save(Poll.create(room, title));
        for (String label : labels) {
            optionRepository.save(PollOption.create(poll, label, null, null, null));
        }
        return poll;
    }

    private PollOption firstOption(Poll poll) {
        return optionRepository.findAllByPollIds(List.of(poll.getId())).get(0);
    }

    private Member outsider() {
        Room other = roomRepository.save(RoomFixtures.room("XYZ789"));
        return memberRepository.save(Member.host(other, "철수"));
    }

    private static PollOptionRequest option(String label) {
        return new PollOptionRequest(label, null, null, null);
    }

    private static CreatePollRequest menuPollRequest() {
        return new CreatePollRequest("1차 메뉴", List.of(option("삼겹살"), option("치킨")));
    }

    private void assertError(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(code);
    }
}
