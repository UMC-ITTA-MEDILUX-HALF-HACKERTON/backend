package com.allbutyou.poll.controller;

import com.allbutyou.common.auth.CurrentMember;
import com.allbutyou.common.response.ApiResponse;
import com.allbutyou.poll.dto.CreatePollRequest;
import com.allbutyou.poll.dto.DecidePollRequest;
import com.allbutyou.poll.dto.PollOptionRequest;
import com.allbutyou.poll.dto.PollResponse;
import com.allbutyou.poll.dto.VoteRequest;
import com.allbutyou.poll.service.PollService;
import com.allbutyou.room.domain.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Poll", description = "투표 주제·선택지 생성, 투표, 최종 결정")
@RestController
@RequiredArgsConstructor
public class PollController {

    private static final String BROADCAST_UPDATE =
            " 성공 시 커밋 후 /topic/rooms/{roomId}로 POLL_UPDATE(이 응답과 같은 형태, myOptionId=null)를 방송한다.";
    private static final String BROADCAST_DECIDED =
            " 성공 시 커밋 후 /topic/rooms/{roomId}로 POLL_DECIDED(이 응답과 같은 형태, myOptionId=null)를 방송한다.";

    private final PollService pollService;

    @Operation(summary = "투표 생성 (HOST)",
            description = "방장이 투표 주제와 선택지(최소 2개)를 만든다. label 필수(50자), placeName 선택(100자), "
                    + "lat·lng는 함께 보내거나 둘 다 생략. 모임 참가자가 아니거나 HOST가 아니면 FORBIDDEN(403)."
                    + BROADCAST_UPDATE)
    @PostMapping("/api/rooms/{roomId}/polls")
    public ApiResponse<PollResponse> create(@PathVariable Long roomId,
                                            @Valid @RequestBody CreatePollRequest request,
                                            @Parameter(hidden = true) @CurrentMember Member requester) {
        return ApiResponse.ok(pollService.create(roomId, requester, request));
    }

    @Operation(summary = "투표 목록 조회",
            description = "모임의 투표를 생성 순으로 조회한다. 투표별 선택지(득표 수 포함), 투표 인원, 미응답자, "
                    + "요청자의 선택(myOptionId)을 담는다. 모임 참가자가 아니면 FORBIDDEN(403).")
    @GetMapping("/api/rooms/{roomId}/polls")
    public ApiResponse<List<PollResponse>> getPolls(@PathVariable Long roomId,
                                                    @Parameter(hidden = true) @CurrentMember Member requester) {
        return ApiResponse.ok(pollService.getPolls(roomId, requester));
    }

    @Operation(summary = "선택지 추가 (HOST)",
            description = "열려 있는 투표에 선택지를 추가한다. 이미 결정된 투표면 INVALID_INPUT(400), "
                    + "투표가 없으면 NOT_FOUND(404), 모임 참가자가 아니거나 HOST가 아니면 FORBIDDEN(403)."
                    + BROADCAST_UPDATE)
    @PostMapping("/api/polls/{pollId}/options")
    public ApiResponse<PollResponse> addOption(@PathVariable Long pollId,
                                               @Valid @RequestBody PollOptionRequest request,
                                               @Parameter(hidden = true) @CurrentMember Member requester) {
        return ApiResponse.ok(pollService.addOption(pollId, requester, request));
    }

    @Operation(summary = "투표하기 / 투표 변경",
            description = "1인 1표. 처음이면 투표를 만들고, 이미 투표했으면 선택지를 바꾼다(동시 요청도 변경으로 처리). "
                    + "다른 투표의 선택지이거나 이미 결정된 투표면 INVALID_INPUT(400), 모임 참가자가 아니면 FORBIDDEN(403)."
                    + BROADCAST_UPDATE)
    @PutMapping("/api/polls/{pollId}/votes")
    public ApiResponse<PollResponse> vote(@PathVariable Long pollId,
                                          @Valid @RequestBody VoteRequest request,
                                          @Parameter(hidden = true) @CurrentMember Member requester) {
        return ApiResponse.ok(pollService.vote(pollId, requester, request.optionId()));
    }

    @Operation(summary = "최종 결정 (HOST)",
            description = "방장이 선택지 하나를 최종 결정한다. status는 DECIDED가 되고 이후 투표·선택지 추가는 막힌다. "
                    + "이미 결정된 투표에 다시 요청하면 결정을 바꾼다. 다른 투표의 선택지면 INVALID_INPUT(400), "
                    + "모임 참가자가 아니거나 HOST가 아니면 FORBIDDEN(403)."
                    + BROADCAST_DECIDED)
    @PatchMapping("/api/polls/{pollId}/decision")
    public ApiResponse<PollResponse> decide(@PathVariable Long pollId,
                                            @Valid @RequestBody DecidePollRequest request,
                                            @Parameter(hidden = true) @CurrentMember Member requester) {
        return ApiResponse.ok(pollService.decide(pollId, requester, request.optionId()));
    }
}
