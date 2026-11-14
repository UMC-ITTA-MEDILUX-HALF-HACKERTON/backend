package com.allbutyou.game.service;

import com.allbutyou.common.exception.BusinessException;
import com.allbutyou.common.exception.ErrorCode;
import com.allbutyou.game.domain.Game;
import com.allbutyou.game.domain.GameOutcome;
import com.allbutyou.game.domain.GameParticipant;
import com.allbutyou.game.domain.GameTeam;
import com.allbutyou.game.dto.GameProgress;
import com.allbutyou.game.dto.GameResultResponse;
import com.allbutyou.game.dto.GameStartResponse;
import com.allbutyou.game.dto.LateMemberResponse;
import com.allbutyou.game.dto.ScoreResponse;
import com.allbutyou.game.dto.TeamMemberResponse;
import com.allbutyou.game.repository.GameParticipantRepository;
import com.allbutyou.game.repository.GameRepository;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.Room;
import com.allbutyou.room.service.RoomService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameService {

    private final RoomService roomService;
    private final GameRepository gameRepository;
    private final GameParticipantRepository participantRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * 방장이 약속 시각 이후에 게임을 시작한다. 시작 순간의 모임 멤버 전원이 참가자가 되고 팀이 고정된다.
     * 약속 시각 전이면 400, 이미 게임이 있으면 409, 지각자 팀이 0명이면 400.
     */
    @Transactional
    public GameStartResponse start(Long roomId, Member requester) {
        roomService.validateHost(roomId, requester);
        Room room = roomService.getRoom(roomId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(room.getStartAt())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "약속 시각 이후에 게임을 시작할 수 있습니다.");
        }
        if (gameRepository.findByRoomId(roomId).isPresent()) {
            throw alreadyStarted();
        }
        List<Member> members = roomService.getMembers(roomId);
        if (members.stream().noneMatch(m -> GameTeam.snapshot(m, room.getStartAt()) == GameTeam.LATE)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "지각자가 없어 게임을 시작할 수 없습니다.");
        }

        Game game;
        try {
            game = gameRepository.saveAndFlush(Game.start(room, now));
        } catch (DataIntegrityViolationException e) {
            // 동시 시작으로 조회를 통과한 경우 UK(room_id)가 막는다.
            throw alreadyStarted();
        }
        List<GameParticipant> participants = participantRepository.saveAll(members.stream()
                .map(m -> GameParticipant.of(game, m, GameTeam.snapshot(m, room.getStartAt())))
                .toList());

        GameStartResponse response = new GameStartResponse(game.getId(), game.getStartedAt(),
                teamMembers(participants, GameTeam.LATE), teamMembers(participants, GameTeam.ON_TIME));
        eventPublisher.publishEvent(new GameStartedEvent(roomId, response));
        return response;
    }

    /**
     * 게임 참가자가 점수를 1회 제출한다. 모임 멤버가 아니거나 게임 참가자가 아니면 403,
     * 게임이 없거나 종료됐으면 400, 이미 제출했으면 409. 전원이 제출하면 자동으로 종료한다.
     */
    @Transactional
    public GameResultResponse submitScore(Long roomId, Member requester, int score) {
        roomService.validateParticipant(roomId, requester);
        Game game = getPlayingGameForUpdate(roomId);
        List<GameParticipant> participants = participantRepository.findAllWithMember(game.getId());
        GameParticipant me = participants.stream()
                .filter(p -> p.getMember().getId().equals(requester.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "게임 참가자가 아닙니다."));
        if (me.isSubmitted()) {
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 점수를 제출했습니다.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        me.submit(score, now);
        if (participants.stream().allMatch(GameParticipant::isSubmitted)) {
            game.finish(now);
        }

        return publishResult(roomId, game, participants);
    }

    /** 방장이 게임을 종료한다. 미제출자가 있어도 종료한다. 게임이 없거나 이미 종료됐으면 400. */
    @Transactional
    public GameResultResponse finish(Long roomId, Member requester) {
        roomService.validateHost(roomId, requester);
        Game game = getPlayingGameForUpdate(roomId);

        game.finish(LocalDateTime.now(clock));

        return publishResult(roomId, game, participantRepository.findAllWithMember(game.getId()));
    }

    public GameResultResponse getResult(Long roomId, Member requester) {
        roomService.validateParticipant(roomId, requester);
        int baseFee = roomService.getRoom(roomId).getLateFee();
        return gameRepository.findByRoomId(roomId)
                .map(game -> buildResult(game, participantRepository.findAllWithMember(game.getId()), baseFee))
                .orElseGet(() -> notStarted(baseFee));
    }

    private Game getPlayingGameForUpdate(Long roomId) {
        Game game = gameRepository.findByRoomIdForUpdate(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "시작된 게임이 없습니다."));
        if (game.isFinished()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 종료된 게임입니다.");
        }
        return game;
    }

    private GameResultResponse publishResult(Long roomId, Game game, List<GameParticipant> participants) {
        GameResultResponse result = buildResult(game, participants, roomService.getRoom(roomId).getLateFee());
        eventPublisher.publishEvent(new GameResultEvent(roomId, result));
        return result;
    }

    /** 팀은 시작 시 고정된 값을 쓴다. 지각 분은 표시용으로 멤버의 현재 값을 보여준다. */
    private GameResultResponse buildResult(Game game, List<GameParticipant> participants, int baseFee) {
        TeamStats late = TeamStats.of(participants, GameTeam.LATE);
        TeamStats onTime = TeamStats.of(participants, GameTeam.ON_TIME);
        GameOutcome outcome = GameOutcome.decide(late.sum, late.submitted, onTime.sum, onTime.submitted);
        int penalty = outcome.penalty(baseFee);

        List<LateMemberResponse> lateMembers = participants.stream()
                .filter(p -> p.getTeam() == GameTeam.LATE)
                .map(p -> new LateMemberResponse(p.getMember().getId(), p.getMember().getNickname(),
                        p.getMember().getLateMinutes(), p.getScore(), penalty))
                .toList();
        List<ScoreResponse> scores = participants.stream()
                .filter(GameParticipant::isSubmitted)
                .sorted(Comparator.comparing(GameParticipant::getSubmittedAt).thenComparing(GameParticipant::getId))
                .map(p -> new ScoreResponse(p.getMember().getId(), p.getMember().getNickname(), p.getTeam(),
                        p.getScore()))
                .toList();

        return new GameResultResponse(GameProgress.of(game), late.average(), onTime.average(),
                late.submitted, late.total, onTime.submitted, onTime.total,
                outcome, penalty, lateMembers, scores);
    }

    private GameResultResponse notStarted(int baseFee) {
        return new GameResultResponse(GameProgress.NOT_STARTED, null, null, 0, 0, 0, 0,
                GameOutcome.PENDING, GameOutcome.PENDING.penalty(baseFee), List.of(), List.of());
    }

    private static List<TeamMemberResponse> teamMembers(List<GameParticipant> participants, GameTeam team) {
        return participants.stream()
                .filter(p -> p.getTeam() == team)
                .map(p -> TeamMemberResponse.from(p.getMember()))
                .toList();
    }

    private BusinessException alreadyStarted() {
        return new BusinessException(ErrorCode.DUPLICATE, "이미 게임이 시작된 모임입니다.");
    }

    private record TeamStats(int total, int submitted, long sum) {

        static TeamStats of(List<GameParticipant> participants, GameTeam team) {
            int total = 0;
            int submitted = 0;
            long sum = 0;
            for (GameParticipant p : participants) {
                if (p.getTeam() != team) {
                    continue;
                }
                total++;
                if (p.isSubmitted()) {
                    submitted++;
                    sum += p.getScore();
                }
            }
            return new TeamStats(total, submitted, sum);
        }

        /** 표시용(소수 둘째 자리 반올림). 결과 판정은 GameOutcome.decide가 합과 인원으로 정확히 한다. */
        Double average() {
            return submitted == 0 ? null : Math.round((double) sum / submitted * 100) / 100.0;
        }
    }
}
