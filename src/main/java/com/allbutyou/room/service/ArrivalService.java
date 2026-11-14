package com.allbutyou.room.service;

import com.allbutyou.room.domain.Member;
import com.allbutyou.room.dto.ArrivalResponse;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArrivalService {

    private final RoomService roomService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 주최자가 참가자의 도착을 기록한다. 이미 도착한 참가자도 시각을 다시 지정할 수 있다.
     *
     * @param arrivedAt null이면 현재 시각
     */
    @Transactional
    public ArrivalResponse markArrived(Long roomId, Long memberId, Member requester, LocalDateTime arrivedAt) {
        roomService.validateHost(roomId, requester);
        Member member = roomService.getMemberInRoom(roomId, memberId);

        member.arrive(arrivedAt != null ? arrivedAt : LocalDateTime.now());

        ArrivalResponse response = ArrivalResponse.from(member);
        eventPublisher.publishEvent(new MemberArrivedEvent(roomId, response));
        return response;
    }
}
