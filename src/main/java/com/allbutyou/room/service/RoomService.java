package com.allbutyou.room.service;

import com.allbutyou.common.exception.BusinessException;
import com.allbutyou.common.exception.ErrorCode;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.Room;
import com.allbutyou.room.dto.CreateRoomRequest;
import com.allbutyou.room.dto.CreateRoomResponse;
import com.allbutyou.room.dto.JoinRoomRequest;
import com.allbutyou.room.dto.JoinRoomResponse;
import com.allbutyou.room.dto.MemberResponse;
import com.allbutyou.room.dto.MemberUpdateMessage;
import com.allbutyou.room.dto.RoomDetailResponse;
import com.allbutyou.room.repository.MemberRepository;
import com.allbutyou.room.repository.RoomRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    static final int MAX_CODE_ATTEMPTS = 10;

    private final RoomRepository roomRepository;
    private final MemberRepository memberRepository;
    private final RoomCodeGenerator codeGenerator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CreateRoomResponse createRoom(CreateRoomRequest request) {
        Room room = roomRepository.save(Room.create(issueCode(), request.title().strip(),
                request.placeName().strip(), request.lat(), request.lng(), request.startAt(), request.lateFee()));
        Member host = memberRepository.save(Member.host(room, request.nickname().strip()));
        return new CreateRoomResponse(room.getId(), room.getCode(), host.getId(), host.getToken());
    }

    @Transactional
    public JoinRoomResponse join(JoinRoomRequest request) {
        String code = request.code().strip().toUpperCase(Locale.ROOT);
        Room room = roomRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "존재하지 않는 참여 코드입니다."));

        String nickname = request.nickname().strip();
        if (memberRepository.existsByRoomIdAndNickname(room.getId(), nickname)) {
            throw duplicateNickname();
        }
        Member member;
        try {
            member = memberRepository.saveAndFlush(Member.member(room, nickname));
        } catch (DataIntegrityViolationException e) {
            // 동시 참여로 exists 검사를 통과한 경우 UK(room_id, nickname)가 막는다.
            throw duplicateNickname();
        }

        eventPublisher.publishEvent(new MemberJoinedEvent(new MemberUpdateMessage(room.getId(), memberResponses(room.getId()))));
        return new JoinRoomResponse(room.getId(), member.getId(), member.getToken());
    }

    public RoomDetailResponse getRoomDetail(Long roomId, Member requester) {
        validateParticipant(roomId, requester);
        return RoomDetailResponse.of(getRoom(roomId), memberResponses(roomId));
    }

    // ---- 권한 검증 (다른 파트에서도 사용) ----

    /** 토큰 주인이 해당 모임 참가자가 아니면 FORBIDDEN. */
    public void validateParticipant(Long roomId, Member requester) {
        if (!requester.belongsTo(roomId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "참여하지 않은 모임입니다.");
        }
    }

    /** 토큰 주인이 해당 모임의 HOST가 아니면 FORBIDDEN. */
    public void validateHost(Long roomId, Member requester) {
        validateParticipant(roomId, requester);
        if (!requester.isHost()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "주최자만 할 수 있습니다.");
        }
    }

    // ---- 다른 파트(location, poll, game)용 조회 메서드 ----

    public Room getRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "모임을 찾을 수 없습니다."));
    }

    public Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "참가자를 찾을 수 없습니다."));
    }

    /** 해당 모임 소속 참가자. 없거나 다른 모임 소속이면 NOT_FOUND. */
    public Member getMemberInRoom(Long roomId, Long memberId) {
        return memberRepository.findById(memberId)
                .filter(m -> m.belongsTo(roomId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "모임에서 참가자를 찾을 수 없습니다."));
    }

    /** 참여 순(id 오름차순). 모임이 없으면 NOT_FOUND. */
    public List<Member> getMembers(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "모임을 찾을 수 없습니다.");
        }
        return memberRepository.findAllByRoomIdOrderByIdAsc(roomId);
    }

    private List<MemberResponse> memberResponses(Long roomId) {
        return getMembers(roomId).stream().map(MemberResponse::from).toList();
    }

    private String issueCode() {
        for (int i = 0; i < MAX_CODE_ATTEMPTS; i++) {
            String code = codeGenerator.generate();
            if (!roomRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL, "참여 코드 발급에 실패했습니다.");
    }

    private BusinessException duplicateNickname() {
        return new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 닉네임입니다.");
    }
}
