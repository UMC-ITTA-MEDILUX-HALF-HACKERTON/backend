package com.allbutyou.room.controller;

import com.allbutyou.common.auth.CurrentMember;
import com.allbutyou.common.response.ApiResponse;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.dto.CreateRoomRequest;
import com.allbutyou.room.dto.CreateRoomResponse;
import com.allbutyou.room.dto.JoinRoomRequest;
import com.allbutyou.room.dto.JoinRoomResponse;
import com.allbutyou.room.dto.RoomDetailResponse;
import com.allbutyou.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Room", description = "모임 생성·참여·조회")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @Operation(summary = "모임 생성",
            description = "모임을 만들고 참여 코드를 발급한다. 생성자는 HOST로 등록되고 인증 토큰을 받는다.")
    @SecurityRequirements
    @PostMapping
    public ApiResponse<CreateRoomResponse> create(@Valid @RequestBody CreateRoomRequest request) {
        return ApiResponse.ok(roomService.createRoom(request));
    }

    @Operation(summary = "모임 참여",
            description = "참여 코드와 닉네임으로 모임에 참여한다. 닉네임이 중복이면 DUPLICATE(409). "
                    + "성공 시 /topic/rooms/{roomId}로 MEMBER_UPDATE를 방송한다.")
    @SecurityRequirements
    @PostMapping("/join")
    public ApiResponse<JoinRoomResponse> join(@Valid @RequestBody JoinRoomRequest request) {
        return ApiResponse.ok(roomService.join(request));
    }

    @Operation(summary = "모임 조회",
            description = "모임 정보, 약속 시간, 참가자 목록(닉네임·역할·도착 시각·지각 분)을 조회한다. 해당 모임 참가자만 조회 가능.")
    @GetMapping("/{roomId}")
    public ApiResponse<RoomDetailResponse> get(@PathVariable Long roomId,
                                               @Parameter(hidden = true) @CurrentMember Member member) {
        return ApiResponse.ok(roomService.getRoomDetail(roomId, member));
    }
}
