package com.allbutyou.candidate.service;

import com.allbutyou.candidate.domain.Candidate;
import com.allbutyou.candidate.dto.CandidateListResponse;
import com.allbutyou.candidate.dto.CandidateResponse;
import com.allbutyou.candidate.dto.CreateCandidateRequest;
import com.allbutyou.candidate.dto.NotVotedMemberResponse;
import com.allbutyou.candidate.repository.CandidateRepository;
import com.allbutyou.candidate.repository.CandidateVoteCount;
import com.allbutyou.candidate.repository.VoteRepository;
import com.allbutyou.common.exception.BusinessException;
import com.allbutyou.common.exception.ErrorCode;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.Room;
import com.allbutyou.room.service.RoomService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CandidateService {

    private final RoomService roomService;
    private final CandidateRepository candidateRepository;
    private final VoteRepository voteRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CandidateResponse register(Long roomId, Member requester, CreateCandidateRequest request) {
        roomService.validateParticipant(roomId, requester);
        Room room = roomService.getRoom(roomId);
        if (room.getDecidedCandidateId() != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 장소가 확정된 모임입니다.");
        }

        Candidate candidate = candidateRepository.save(Candidate.create(room, request.placeName().strip(),
                request.lat(), request.lng(), request.activity().strip(), requester));

        eventPublisher.publishEvent(new CandidateUpdatedEvent(roomId, buildList(room)));
        return CandidateResponse.of(candidate, 0);
    }

    public CandidateListResponse getCandidates(Long roomId, Member requester) {
        roomService.validateParticipant(roomId, requester);
        return buildList(roomService.getRoom(roomId));
    }

    /** 후보·득표 집계·미투표자를 쿼리 4번(후보+등록자, 득표 group by, 투표자 ID, 멤버)으로 조립한다. */
    private CandidateListResponse buildList(Room room) {
        Long roomId = room.getId();
        Map<Long, Long> voteCounts = voteRepository.countByCandidate(roomId).stream()
                .collect(Collectors.toMap(CandidateVoteCount::candidateId, CandidateVoteCount::voteCount));
        List<CandidateResponse> candidates = candidateRepository.findAllByRoomIdOrderByIdAsc(roomId).stream()
                .map(c -> CandidateResponse.of(c, voteCounts.getOrDefault(c.getId(), 0L)))
                .toList();

        Set<Long> voterIds = new HashSet<>(voteRepository.findVoterIds(roomId));
        List<Member> members = roomService.getMembers(roomId);
        List<NotVotedMemberResponse> notVoted = members.stream()
                .filter(m -> !voterIds.contains(m.getId()))
                .map(NotVotedMemberResponse::from)
                .toList();

        return new CandidateListResponse(candidates, members.size(), members.size() - notVoted.size(),
                notVoted, room.getDecidedCandidateId());
    }
}
