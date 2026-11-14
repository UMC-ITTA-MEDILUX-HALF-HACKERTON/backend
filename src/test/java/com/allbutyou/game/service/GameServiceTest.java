package com.allbutyou.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.allbutyou.common.MutableClock;
import com.allbutyou.common.exception.BusinessException;
import com.allbutyou.common.exception.ErrorCode;
import com.allbutyou.game.domain.Game;
import com.allbutyou.game.domain.GameOutcome;
import com.allbutyou.game.domain.GameParticipant;
import com.allbutyou.game.domain.GameStatus;
import com.allbutyou.game.domain.GameTeam;
import com.allbutyou.game.dto.GameProgress;
import com.allbutyou.game.dto.GameResultResponse;
import com.allbutyou.game.dto.GameStartResponse;
import com.allbutyou.game.dto.LateMemberResponse;
import com.allbutyou.game.dto.ScoreResponse;
import com.allbutyou.game.dto.TeamMemberResponse;
import com.allbutyou.game.repository.GameParticipantRepository;
import com.allbutyou.game.repository.GameRepository;
import com.allbutyou.room.RoomFixtures;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.Room;
import com.allbutyou.room.repository.MemberRepository;
import com.allbutyou.room.repository.RoomRepository;
import com.allbutyou.room.service.RoomCodeGenerator;
import com.allbutyou.room.service.RoomService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** 약속 시각은 RoomFixtures.START_AT(2026-11-20 19:00), 기본 지각비는 1000원. 현재 시각은 MutableClock으로 고정한다. */
@DataJpaTest
@Import({GameService.class, RoomService.class, RoomCodeGenerator.class, GameServiceTest.ClockConfig.class})
@RecordApplicationEvents
class GameServiceTest {

    private static final int BASE_FEE = 1000;
    private static final LocalDateTime START_AT = RoomFixtures.START_AT;
    private static final LocalDateTime GAME_TIME = START_AT.plusMinutes(30);

    @TestConfiguration
    static class ClockConfig {
        @Bean
        MutableClock clock() {
            return new MutableClock(GAME_TIME, ZoneId.of("Asia/Seoul"));
        }
    }

    @Autowired GameService gameService;
    @Autowired MutableClock clock;
    @Autowired RoomRepository roomRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired GameRepository gameRepository;
    @Autowired GameParticipantRepository participantRepository;
    @Autowired ApplicationEvents events;

    private Room room;
    private Member host;      // 정시 도착
    private Member onTime;    // 약속 시각 정각 도착
    private Member late1;     // 10분 지각
    private Member late2;     // 25분 지각

    @BeforeEach
    void setUp() {
        clock.setNow(GAME_TIME);
        room = roomRepository.save(RoomFixtures.room("ABC123"));
        host = arrivedAt(Member.host(room, "민수"), START_AT.minusMinutes(5));
        onTime = arrivedAt(Member.member(room, "지영"), START_AT);
        late1 = arrivedAt(Member.member(room, "철수"), START_AT.plusMinutes(10));
        late2 = arrivedAt(Member.member(room, "영희"), START_AT.plusMinutes(25));
    }

    // ---- 시작 ----

    @Test
    void 약속_시각_전에는_시작할_수_없다() {
        clock.setNow(START_AT.minusSeconds(1));

        assertError(() -> gameService.start(room.getId(), host), ErrorCode.INVALID_INPUT);
        assertThat(gameRepository.count()).isZero();
        assertThat(events.stream(GameStartedEvent.class)).isEmpty();
    }

    @Test
    void 약속_시각_정각에는_시작할_수_있다() {
        clock.setNow(START_AT);

        assertThat(gameService.start(room.getId(), host).startedAt()).isEqualTo(START_AT);
    }

    @Test
    void HOST가_아니면_시작할_수_없다() {
        assertError(() -> gameService.start(room.getId(), onTime), ErrorCode.FORBIDDEN);
        assertThat(gameRepository.count()).isZero();
    }

    @Test
    void 이미_게임이_있으면_DUPLICATE() {
        gameService.start(room.getId(), host);

        assertError(() -> gameService.start(room.getId(), host), ErrorCode.DUPLICATE);
        assertThat(gameRepository.count()).isEqualTo(1);
    }

    @Test
    void 지각자가_없으면_시작할_수_없다() {
        Room punctual = roomRepository.save(RoomFixtures.room("XYZ789"));
        Member punctualHost = arrivedAt(Member.host(punctual, "민수"), START_AT);
        arrivedAt(Member.member(punctual, "지영"), START_AT.minusMinutes(1));

        assertError(() -> gameService.start(punctual.getId(), punctualHost), ErrorCode.INVALID_INPUT);
    }

    @Test
    void 시작_순간에_팀을_고정한다_정시_지각_미도착() {
        // 30초 지각: lateMinutes는 0이지만 arrivedAt > startAt 이라 LATE
        Member slightlyLate = arrivedAt(Member.member(room, "30초"), START_AT.plusSeconds(30));
        Member notArrived = memberRepository.save(Member.member(room, "미도착"));
        assertThat(slightlyLate.getLateMinutes()).isZero();

        GameStartResponse res = gameService.start(room.getId(), host);

        assertThat(res.gameId()).isNotNull();
        assertThat(res.startedAt()).isEqualTo(GAME_TIME);
        assertThat(res.onTimeTeam()).extracting(TeamMemberResponse::memberId)
                .containsExactly(host.getId(), onTime.getId());
        assertThat(res.lateTeam()).extracting(TeamMemberResponse::memberId)
                .containsExactly(late1.getId(), late2.getId(), slightlyLate.getId(), notArrived.getId());

        Game game = gameRepository.findById(res.gameId()).orElseThrow();
        assertThat(game.getStatus()).isEqualTo(GameStatus.PLAYING);
        assertThat(participantRepository.findAllWithMember(game.getId()))
                .extracting(p -> p.getMember().getId(), GameParticipant::getTeam, GameParticipant::getScore)
                .containsExactly(
                        tuple(host.getId(), GameTeam.ON_TIME, null),
                        tuple(onTime.getId(), GameTeam.ON_TIME, null),
                        tuple(late1.getId(), GameTeam.LATE, null),
                        tuple(late2.getId(), GameTeam.LATE, null),
                        tuple(slightlyLate.getId(), GameTeam.LATE, null),
                        tuple(notArrived.getId(), GameTeam.LATE, null));

        // 시작 후 도착해도 팀은 바뀌지 않는다.
        notArrived.arrive(START_AT.minusMinutes(1));
        assertThat(gameService.getResult(room.getId(), host).lateTotalCount()).isEqualTo(4);

        assertThat(events.stream(GameStartedEvent.class)).singleElement().satisfies(e -> {
            assertThat(e.roomId()).isEqualTo(room.getId());
            assertThat(e.game()).isEqualTo(res);
        });
    }

    // ---- 점수 제출과 결과 ----

    @Test
    void 지각자_평균이_높으면_WIN이고_지각비는_0원() {
        start();
        submit(host, 100);
        submit(onTime, 200);
        GameResultResponse res = submit(late1, 300);

        assertThat(res.result()).isEqualTo(GameOutcome.WIN);
        assertThat(res.penaltyPerPerson()).isZero();
        assertThat(res.lateMembers()).extracting(LateMemberResponse::penalty).containsOnly(0);
    }

    @Test
    void 지각자_평균이_낮으면_LOSE이고_기본_지각비의_2배() {
        start();
        submit(host, 1500);
        clock.setNow(GAME_TIME.plusMinutes(1));
        submit(onTime, 1000);
        clock.setNow(GAME_TIME.plusMinutes(2));
        GameResultResponse res = submit(late1, 1200);

        assertThat(res.status()).isEqualTo(GameProgress.PLAYING);
        assertThat(res.result()).isEqualTo(GameOutcome.LOSE);
        assertThat(res.penaltyPerPerson()).isEqualTo(BASE_FEE * 2);
        assertThat(res.lateAverage()).isEqualTo(1200.0);
        assertThat(res.onTimeAverage()).isEqualTo(1250.0);
        assertThat(res.lateSubmittedCount()).isEqualTo(1);
        assertThat(res.lateTotalCount()).isEqualTo(2);
        assertThat(res.onTimeSubmittedCount()).isEqualTo(2);
        assertThat(res.onTimeTotalCount()).isEqualTo(2);
        // 미제출 지각자도 목록에 나오고 지각비는 똑같이 적용된다.
        assertThat(res.lateMembers())
                .extracting(LateMemberResponse::memberId, LateMemberResponse::lateMinutes,
                        LateMemberResponse::score, LateMemberResponse::penalty)
                .containsExactly(
                        tuple(late1.getId(), 10, 1200, 2000),
                        tuple(late2.getId(), 25, null, 2000));
        assertThat(res.scores())
                .extracting(ScoreResponse::memberId, ScoreResponse::nickname, ScoreResponse::team, ScoreResponse::score)
                .containsExactly(
                        tuple(host.getId(), "민수", GameTeam.ON_TIME, 1500),
                        tuple(onTime.getId(), "지영", GameTeam.ON_TIME, 1000),
                        tuple(late1.getId(), "철수", GameTeam.LATE, 1200));
    }

    @Test
    void 평균이_같으면_DRAW이고_기본_지각비() {
        // 인원이 달라도 평균만 비교한다: 지각 (100+200)/2 = 150, 정시 150/1 = 150
        start();
        submit(late1, 100);
        submit(late2, 200);
        GameResultResponse res = submit(host, 150);

        assertThat(res.result()).isEqualTo(GameOutcome.DRAW);
        assertThat(res.penaltyPerPerson()).isEqualTo(BASE_FEE);
    }

    @Test
    void 평균이_나눠떨어지지_않아도_정확히_비교한다() {
        // 지각 (1+1+2)/3 = 1.333..., 정시 (1+1+2)/3 = 1.333... → 무한소수라도 교차 곱으로 정확히 DRAW
        Member late3 = arrivedAt(Member.member(room, "동수"), START_AT.plusMinutes(3));
        Member onTime3 = arrivedAt(Member.member(room, "수진"), START_AT);
        start();
        submit(late1, 1);
        submit(late2, 1);
        submit(late3, 2);
        submit(host, 1);
        submit(onTime, 1);
        GameResultResponse res = submit(onTime3, 2);

        assertThat(res.result()).isEqualTo(GameOutcome.DRAW);
        assertThat(res.lateAverage()).isEqualTo(1.33);
    }

    @Test
    void 한_팀이라도_제출자가_없으면_PENDING이고_기본_지각비로_표시() {
        start();
        GameResultResponse res = submit(host, 1000);

        assertThat(res.result()).isEqualTo(GameOutcome.PENDING);
        assertThat(res.penaltyPerPerson()).isEqualTo(BASE_FEE);
        assertThat(res.lateAverage()).isNull();
        assertThat(res.onTimeAverage()).isEqualTo(1000.0);
        assertThat(res.lateSubmittedCount()).isZero();
        assertThat(res.lateMembers()).extracting(LateMemberResponse::score).containsOnlyNulls();
        assertThat(res.lateMembers()).extracting(LateMemberResponse::penalty).containsOnly(BASE_FEE);
    }

    @Test
    void 전원이_제출하면_자동으로_FINISHED() {
        start();
        submit(host, 100);
        submit(onTime, 200);
        submit(late1, 300);
        clock.setNow(GAME_TIME.plusMinutes(3));
        GameResultResponse res = submit(late2, 400);

        assertThat(res.status()).isEqualTo(GameProgress.FINISHED);
        Game game = gameRepository.findByRoomId(room.getId()).orElseThrow();
        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(game.getFinishedAt()).isEqualTo(GAME_TIME.plusMinutes(3));
        assertThat(events.stream(GameResultEvent.class)).hasSize(4)
                .last().satisfies(e -> assertThat(e.result().status()).isEqualTo(GameProgress.FINISHED));
    }

    @Test
    void 제출하면_GAME_RESULT_이벤트를_발행한다() {
        start();
        GameResultResponse res = submit(host, 1000);

        assertThat(events.stream(GameResultEvent.class)).singleElement().satisfies(e -> {
            assertThat(e.roomId()).isEqualTo(room.getId());
            assertThat(e.result()).isEqualTo(res);
        });
    }

    @Test
    void 이미_제출했으면_DUPLICATE() {
        start();
        submit(late1, 100);

        assertError(() -> submit(late1, 200), ErrorCode.DUPLICATE);
        assertThat(events.stream(GameResultEvent.class)).hasSize(1);
        assertThat(gameService.getResult(room.getId(), late1).lateMembers())
                .extracting(LateMemberResponse::score).containsExactly(100, null);
    }

    @Test
    void 게임이_없으면_제출할_수_없다() {
        assertError(() -> submit(host, 100), ErrorCode.INVALID_INPUT);
    }

    @Test
    void 종료된_게임에는_제출할_수_없다() {
        start();
        gameService.finish(room.getId(), host);

        assertError(() -> submit(late1, 100), ErrorCode.INVALID_INPUT);
    }

    @Test
    void 게임_시작_후_들어온_멤버는_참가자가_아니라_FORBIDDEN() {
        start();
        Member newcomer = memberRepository.save(Member.member(room, "늦게온사람"));

        assertError(() -> submit(newcomer, 100), ErrorCode.FORBIDDEN);
    }

    @Test
    void 다른_모임_참가자가_제출하면_FORBIDDEN() {
        start();
        Member outsider = outsider();

        assertError(() -> gameService.submitScore(room.getId(), outsider, 100), ErrorCode.FORBIDDEN);
        assertThat(events.stream(GameResultEvent.class)).isEmpty();
    }

    @Test
    void 다른_모임_참가자가_조회하면_FORBIDDEN() {
        Member outsider = outsider();

        assertError(() -> gameService.getResult(room.getId(), outsider), ErrorCode.FORBIDDEN);
    }

    // ---- 종료 ----

    @Test
    void HOST가_종료하면_미제출자가_있어도_FINISHED이고_GAME_RESULT를_발행한다() {
        start();
        submit(host, 100);
        submit(late1, 50);
        clock.setNow(GAME_TIME.plusMinutes(10));

        GameResultResponse res = gameService.finish(room.getId(), host);

        assertThat(res.status()).isEqualTo(GameProgress.FINISHED);
        assertThat(res.result()).isEqualTo(GameOutcome.LOSE);
        assertThat(gameRepository.findByRoomId(room.getId()).orElseThrow().getFinishedAt())
                .isEqualTo(GAME_TIME.plusMinutes(10));
        assertThat(events.stream(GameResultEvent.class)).hasSize(3)
                .last().satisfies(e -> assertThat(e.result().status()).isEqualTo(GameProgress.FINISHED));
    }

    @Test
    void HOST가_아니면_종료할_수_없다() {
        start();

        assertError(() -> gameService.finish(room.getId(), late1), ErrorCode.FORBIDDEN);
    }

    @Test
    void 게임이_없거나_이미_종료됐으면_종료할_수_없다() {
        assertError(() -> gameService.finish(room.getId(), host), ErrorCode.INVALID_INPUT);

        start();
        gameService.finish(room.getId(), host);
        assertError(() -> gameService.finish(room.getId(), host), ErrorCode.INVALID_INPUT);
    }

    // ---- 조회 ----

    @Test
    void 시작_전이면_NOT_STARTED와_빈_집계() {
        GameResultResponse res = gameService.getResult(room.getId(), late1);

        assertThat(res.status()).isEqualTo(GameProgress.NOT_STARTED);
        assertThat(res.result()).isEqualTo(GameOutcome.PENDING);
        assertThat(res.penaltyPerPerson()).isEqualTo(BASE_FEE);
        assertThat(res.lateAverage()).isNull();
        assertThat(res.lateTotalCount()).isZero();
        assertThat(res.lateMembers()).isEmpty();
        assertThat(res.scores()).isEmpty();
    }

    @Test
    void 시작_직후_아무도_제출하지_않았으면_PLAYING과_PENDING() {
        start();

        GameResultResponse res = gameService.getResult(room.getId(), late1);

        assertThat(res.status()).isEqualTo(GameProgress.PLAYING);
        assertThat(res.result()).isEqualTo(GameOutcome.PENDING);
        assertThat(res.lateTotalCount()).isEqualTo(2);
        assertThat(res.onTimeTotalCount()).isEqualTo(2);
        assertThat(res.lateMembers()).hasSize(2);
    }

    // ---- helpers ----

    private Member arrivedAt(Member member, LocalDateTime arrivedAt) {
        member.arrive(arrivedAt);
        return memberRepository.save(member);
    }

    private void start() {
        gameService.start(room.getId(), host);
    }

    private GameResultResponse submit(Member member, int score) {
        return gameService.submitScore(room.getId(), member, score);
    }

    private Member outsider() {
        Room other = roomRepository.save(RoomFixtures.room("OUT999"));
        return arrivedAt(Member.host(other, "외부인"), START_AT);
    }

    private void assertError(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(code);
    }
}
