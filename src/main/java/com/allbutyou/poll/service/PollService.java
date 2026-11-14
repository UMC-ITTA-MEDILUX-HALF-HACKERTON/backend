package com.allbutyou.poll.service;

import com.allbutyou.common.exception.BusinessException;
import com.allbutyou.common.exception.ErrorCode;
import com.allbutyou.poll.domain.Poll;
import com.allbutyou.poll.domain.PollOption;
import com.allbutyou.poll.domain.Vote;
import com.allbutyou.poll.dto.CreatePollRequest;
import com.allbutyou.poll.dto.NotVotedMemberResponse;
import com.allbutyou.poll.dto.PollOptionRequest;
import com.allbutyou.poll.dto.PollOptionResponse;
import com.allbutyou.poll.dto.PollResponse;
import com.allbutyou.poll.repository.OptionVoteCount;
import com.allbutyou.poll.repository.PollOptionRepository;
import com.allbutyou.poll.repository.PollRepository;
import com.allbutyou.poll.repository.VoteRepository;
import com.allbutyou.poll.repository.VoterChoice;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.Room;
import com.allbutyou.room.service.RoomService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Transactional(readOnly = true)
public class PollService {

    private final RoomService roomService;
    private final PollRepository pollRepository;
    private final PollOptionRepository optionRepository;
    private final VoteRepository voteRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public PollService(RoomService roomService, PollRepository pollRepository, PollOptionRepository optionRepository,
                       VoteRepository voteRepository, ApplicationEventPublisher eventPublisher,
                       PlatformTransactionManager transactionManager) {
        this.roomService = roomService;
        this.pollRepository = pollRepository;
        this.optionRepository = optionRepository;
        this.voteRepository = voteRepository;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public PollResponse create(Long roomId, Member requester, CreatePollRequest request) {
        roomService.validateHost(roomId, requester);
        Room room = roomService.getRoom(roomId);

        Poll poll = pollRepository.save(Poll.create(room, request.title().strip()));
        optionRepository.saveAll(request.options().stream().map(o -> toOption(poll, o)).toList());

        return publishUpdated(poll, requester);
    }

    @Transactional
    public PollResponse addOption(Long pollId, Member requester, PollOptionRequest request) {
        Poll poll = getPoll(pollId);
        roomService.validateHost(poll.getRoomId(), requester);
        validateOpen(poll);

        optionRepository.save(toOption(poll, request));
        return publishUpdated(poll, requester);
    }

    public List<PollResponse> getPolls(Long roomId, Member requester) {
        roomService.validateParticipant(roomId, requester);
        return buildResponses(roomId, pollRepository.findAllByRoomId(roomId), requester.getId());
    }

    /**
     * 첫 투표면 생성, 이미 있으면 변경.
     * 같은 멤버의 첫 투표가 동시에 들어오면 한쪽이 UK(poll_id, member_id)에 걸린다. 실패한 트랜잭션은
     * rollback-only라 그 안에서 재시도할 수 없으므로, 새 트랜잭션에서 한 번 더 실행해 변경으로 처리한다.
     * 바깥 트랜잭션이 있으면(테스트 등) 그대로 참여한다.
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public PollResponse vote(Long pollId, Member requester, Long optionId) {
        try {
            return transactionTemplate.execute(status -> castVote(pollId, requester, optionId));
        } catch (DataIntegrityViolationException e) {
            return transactionTemplate.execute(status -> castVote(pollId, requester, optionId));
        }
    }

    @Transactional
    public PollResponse decide(Long pollId, Member requester, Long optionId) {
        Poll poll = getPoll(pollId);
        roomService.validateHost(poll.getRoomId(), requester);
        PollOption option = getOptionOf(poll, optionId);

        poll.decide(option);

        PollResponse response = buildResponse(poll, requester.getId());
        eventPublisher.publishEvent(new PollDecidedEvent(poll.getRoomId(), response.forBroadcast()));
        return response;
    }

    private PollResponse castVote(Long pollId, Member requester, Long optionId) {
        Poll poll = getPoll(pollId);
        roomService.validateParticipant(poll.getRoomId(), requester);
        validateOpen(poll);
        PollOption option = getOptionOf(poll, optionId);

        voteRepository.findByPollIdAndMemberId(pollId, requester.getId()).ifPresentOrElse(
                vote -> vote.changeOption(option),
                // flush해서 UK 충돌을 이 트랜잭션 안에서 DataIntegrityViolationException으로 받는다.
                () -> voteRepository.saveAndFlush(Vote.of(option, requester)));

        return publishUpdated(poll, requester);
    }

    private Poll getPoll(Long pollId) {
        return pollRepository.findById(pollId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "투표를 찾을 수 없습니다."));
    }

    /** 선택지가 없거나 다른 투표의 것이면 INVALID_INPUT. */
    private PollOption getOptionOf(Poll poll, Long optionId) {
        return optionRepository.findById(optionId)
                .filter(o -> o.belongsTo(poll))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "이 투표의 선택지가 아닙니다."));
    }

    private void validateOpen(Poll poll) {
        if (poll.isDecided()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 결정된 투표입니다.");
        }
    }

    private PollOption toOption(Poll poll, PollOptionRequest request) {
        String placeName = request.placeName() == null || request.placeName().isBlank()
                ? null : request.placeName().strip();
        return PollOption.create(poll, request.label().strip(), placeName, request.lat(), request.lng());
    }

    /** 요청자 기준 상태를 만들고, myOptionId를 비운 사본을 POLL_UPDATE 이벤트로 발행한다. */
    private PollResponse publishUpdated(Poll poll, Member requester) {
        PollResponse response = buildResponse(poll, requester.getId());
        eventPublisher.publishEvent(new PollUpdatedEvent(poll.getRoomId(), response.forBroadcast()));
        return response;
    }

    private PollResponse buildResponse(Poll poll, Long requesterId) {
        return buildResponses(poll.getRoomId(), List.of(poll), requesterId).get(0);
    }

    /**
     * 투표 수와 관계없이 쿼리 4번(선택지, 득표 group by, 투표자 선택, 멤버)으로 조립한다. 투표 목록은 호출자가 넘긴다.
     * votedCount는 (poll, member) UK 덕분에 투표자 행 수와 같다.
     */
    private List<PollResponse> buildResponses(Long roomId, List<Poll> polls, Long requesterId) {
        if (polls.isEmpty()) {
            return List.of();
        }
        List<Long> pollIds = polls.stream().map(Poll::getId).toList();

        Map<Long, Long> voteCounts = voteRepository.countByOption(pollIds).stream()
                .collect(Collectors.toMap(OptionVoteCount::optionId, OptionVoteCount::voteCount));
        Map<Long, List<PollOption>> optionsByPoll = optionRepository.findAllByPollIds(pollIds).stream()
                .collect(Collectors.groupingBy(o -> o.getPoll().getId()));
        // pollId -> (memberId -> optionId)
        Map<Long, Map<Long, Long>> choicesByPoll = new HashMap<>();
        for (VoterChoice c : voteRepository.findVoterChoices(pollIds)) {
            choicesByPoll.computeIfAbsent(c.pollId(), k -> new HashMap<>()).put(c.memberId(), c.optionId());
        }
        List<Member> members = roomService.getMembers(roomId);

        return polls.stream().map(poll -> {
            List<PollOptionResponse> options = optionsByPoll.getOrDefault(poll.getId(), List.of()).stream()
                    .map(o -> PollOptionResponse.of(o, voteCounts.getOrDefault(o.getId(), 0L)))
                    .toList();
            Map<Long, Long> choices = choicesByPoll.getOrDefault(poll.getId(), Map.of());
            List<NotVotedMemberResponse> notVoted = members.stream()
                    .filter(m -> !choices.containsKey(m.getId()))
                    .map(NotVotedMemberResponse::from)
                    .toList();
            Long myOptionId = requesterId == null ? null : choices.get(requesterId);
            return new PollResponse(poll.getId(), poll.getTitle(), poll.getStatus(), poll.getDecidedOptionId(),
                    options, choices.size(), notVoted, myOptionId);
        }).toList();
    }
}
