package com.allbutyou.candidate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.allbutyou.candidate.domain.Candidate;
import com.allbutyou.candidate.domain.Vote;
import com.allbutyou.candidate.dto.CandidateListResponse;
import com.allbutyou.candidate.dto.CandidateResponse;
import com.allbutyou.candidate.dto.CreateCandidateRequest;
import com.allbutyou.candidate.dto.NotVotedMemberResponse;
import com.allbutyou.candidate.repository.CandidateRepository;
import com.allbutyou.candidate.repository.VoteRepository;
import com.allbutyou.common.exception.BusinessException;
import com.allbutyou.common.exception.ErrorCode;
import com.allbutyou.room.RoomFixtures;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.Room;
import com.allbutyou.room.repository.MemberRepository;
import com.allbutyou.room.repository.RoomRepository;
import com.allbutyou.room.service.RoomCodeGenerator;
import com.allbutyou.room.service.RoomService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import({CandidateService.class, RoomService.class, RoomCodeGenerator.class})
@RecordApplicationEvents
class CandidateServiceTest {

    @Autowired CandidateService candidateService;
    @Autowired RoomRepository roomRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired CandidateRepository candidateRepository;
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

    @Test
    void 참가자가_후보를_등록하면_저장하고_CANDIDATE_UPDATE_이벤트를_발행한다() {
        CandidateResponse res = candidateService.register(room.getId(), member,
                new CreateCandidateRequest("  성수 볼링장 ", 37.5446, 127.0559, " 볼링 "));

        assertThat(res.candidateId()).isNotNull();
        assertThat(res.placeName()).isEqualTo("성수 볼링장");
        assertThat(res.activity()).isEqualTo("볼링");
        assertThat(res.createdByNickname()).isEqualTo("지영");
        assertThat(res.voteCount()).isZero();

        Candidate saved = candidateRepository.findById(res.candidateId()).orElseThrow();
        assertThat(saved.getRoom().getId()).isEqualTo(room.getId());
        assertThat(saved.getLat()).isEqualTo(37.5446);
        assertThat(saved.getCreatedAt()).isNotNull();

        assertThat(events.stream(CandidateUpdatedEvent.class)).singleElement().satisfies(e -> {
            assertThat(e.roomId()).isEqualTo(room.getId());
            assertThat(e.candidates().candidates()).extracting(CandidateResponse::candidateId)
                    .containsExactly(res.candidateId());
        });
    }

    @Test
    void 좌표_없이도_등록할_수_있다() {
        CandidateResponse res = candidateService.register(room.getId(), host,
                new CreateCandidateRequest("아무 데나", null, null, "산책"));

        assertThat(res.lat()).isNull();
        assertThat(res.lng()).isNull();
    }

    @Test
    void 다른_모임_참가자가_등록하면_FORBIDDEN() {
        Room other = roomRepository.save(RoomFixtures.room("XYZ789"));
        Member outsider = memberRepository.save(Member.host(other, "철수"));

        assertError(() -> candidateService.register(room.getId(), outsider,
                new CreateCandidateRequest("성수 볼링장", null, null, "볼링")), ErrorCode.FORBIDDEN);
        assertThat(candidateRepository.count()).isZero();
        assertThat(events.stream(CandidateUpdatedEvent.class)).isEmpty();
    }

    @Test
    void 다른_모임_참가자가_조회하면_FORBIDDEN() {
        Room other = roomRepository.save(RoomFixtures.room("XYZ789"));
        Member outsider = memberRepository.save(Member.host(other, "철수"));

        assertError(() -> candidateService.getCandidates(room.getId(), outsider), ErrorCode.FORBIDDEN);
    }

    @Test
    void 확정된_모임이면_등록할_수_없다() {
        Candidate decided = candidateRepository.save(Candidate.create(room, "확정 장소", null, null, "밥", host));
        ReflectionTestUtils.setField(room, "decidedCandidateId", decided.getId());

        assertError(() -> candidateService.register(room.getId(), member,
                new CreateCandidateRequest("새 장소", null, null, "볼링")), ErrorCode.INVALID_INPUT);
    }

    @Test
    void 득표_수와_미투표_인원을_계산한다() {
        Member third = memberRepository.save(Member.member(room, "철수"));
        Member fourth = memberRepository.save(Member.member(room, "영희"));
        Candidate bowling = candidateRepository.save(Candidate.create(room, "성수 볼링장", 37.5, 127.0, "볼링", host));
        Candidate cafe = candidateRepository.save(Candidate.create(room, "연남 카페", null, null, "수다", member));
        Candidate empty = candidateRepository.save(Candidate.create(room, "한강", null, null, "산책", member));
        voteRepository.save(Vote.of(bowling, host));
        voteRepository.save(Vote.of(bowling, third));
        voteRepository.save(Vote.of(cafe, member));

        // 다른 모임의 표는 집계에 섞이지 않아야 한다.
        Room other = roomRepository.save(RoomFixtures.room("XYZ789"));
        Member outsider = memberRepository.save(Member.host(other, "외부인"));
        Candidate otherCandidate = candidateRepository.save(Candidate.create(other, "다른 곳", null, null, "밥", outsider));
        voteRepository.save(Vote.of(otherCandidate, outsider));

        CandidateListResponse res = candidateService.getCandidates(room.getId(), member);

        assertThat(res.candidates())
                .extracting(CandidateResponse::candidateId, CandidateResponse::createdByNickname,
                        CandidateResponse::voteCount)
                .containsExactly(
                        tuple(bowling.getId(), "민수", 2L),
                        tuple(cafe.getId(), "지영", 1L),
                        tuple(empty.getId(), "지영", 0L));
        assertThat(res.totalMembers()).isEqualTo(4);
        assertThat(res.votedCount()).isEqualTo(3);
        assertThat(res.notVotedMembers()).extracting(NotVotedMemberResponse::memberId, NotVotedMemberResponse::nickname)
                .containsExactly(tuple(fourth.getId(), "영희"));
        assertThat(res.decidedCandidateId()).isNull();
    }

    @Test
    void 아무도_투표하지_않았으면_전원이_미투표자다() {
        CandidateListResponse res = candidateService.getCandidates(room.getId(), host);

        assertThat(res.candidates()).isEmpty();
        assertThat(res.totalMembers()).isEqualTo(2);
        assertThat(res.votedCount()).isZero();
        assertThat(res.notVotedMembers()).extracting(NotVotedMemberResponse::memberId)
                .containsExactly(host.getId(), member.getId());
    }

    private void assertError(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(code);
    }
}
