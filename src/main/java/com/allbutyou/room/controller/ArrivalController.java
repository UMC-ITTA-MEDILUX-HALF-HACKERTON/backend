package com.allbutyou.room.controller;

import com.allbutyou.common.auth.CurrentMember;
import com.allbutyou.common.response.ApiResponse;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.dto.ArrivalRequest;
import com.allbutyou.room.dto.ArrivalResponse;
import com.allbutyou.room.service.ArrivalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Arrival", description = "도착 관리")
@RestController
@RequestMapping("/api/rooms/{roomId}/members/{memberId}")
@RequiredArgsConstructor
public class ArrivalController {

    private final ArrivalService arrivalService;

    @Operation(summary = "도착 처리",
            description = "주최자(HOST)가 참가자의 도착 시각을 기록한다. arrivedAt이 null이거나 본문이 없으면 서버 현재 시각. "
                    + "이미 도착한 참가자도 시각을 수정할 수 있다. 지각 분 = max(0, 도착 시각 - 약속 시각). "
                    + "HOST가 아니거나 다른 모임 토큰이면 FORBIDDEN(403), 참가자가 해당 모임 소속이 아니면 NOT_FOUND(404). "
                    + "성공 시 /topic/rooms/{roomId}로 ARRIVED를 방송한다.")
    @PatchMapping("/arrival")
    public ApiResponse<ArrivalResponse> arrive(@PathVariable Long roomId,
                                               @PathVariable Long memberId,
                                               @RequestBody(required = false) ArrivalRequest request,
                                               @Parameter(hidden = true) @CurrentMember Member requester) {
        return ApiResponse.ok(arrivalService.markArrived(roomId, memberId, requester,
                request == null ? null : request.arrivedAt()));
    }
}
