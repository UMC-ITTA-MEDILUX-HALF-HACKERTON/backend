package com.allbutyou.candidate.controller;

import com.allbutyou.candidate.dto.CandidateListResponse;
import com.allbutyou.candidate.dto.CandidateResponse;
import com.allbutyou.candidate.dto.CreateCandidateRequest;
import com.allbutyou.candidate.service.CandidateService;
import com.allbutyou.common.auth.CurrentMember;
import com.allbutyou.common.response.ApiResponse;
import com.allbutyou.room.domain.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Candidate", description = "장소 후보 등록·조회")
@RestController
@RequestMapping("/api/rooms/{roomId}/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;

    @Operation(summary = "후보 등록",
            description = "모임 참가자가 장소 후보를 등록한다. placeName·activity 필수, lat·lng는 함께 보내거나 둘 다 생략. "
                    + "다른 모임 토큰이면 FORBIDDEN(403), 이미 확정된 모임이면 INVALID_INPUT(400). "
                    + "성공 시 /topic/rooms/{roomId}로 CANDIDATE_UPDATE(후보 목록 조회 응답과 같은 형태)를 방송한다.")
    @PostMapping
    public ApiResponse<CandidateResponse> register(@PathVariable Long roomId,
                                                   @Valid @RequestBody CreateCandidateRequest request,
                                                   @Parameter(hidden = true) @CurrentMember Member requester) {
        return ApiResponse.ok(candidateService.register(roomId, requester, request));
    }

    @Operation(summary = "후보 목록 조회",
            description = "후보 목록(등록 순, 득표 수 포함)과 투표 현황(전체 인원, 투표 인원, 미투표자), 확정 후보 ID를 조회한다. "
                    + "다른 모임 토큰이면 FORBIDDEN(403).")
    @GetMapping
    public ApiResponse<CandidateListResponse> getCandidates(@PathVariable Long roomId,
                                                            @Parameter(hidden = true) @CurrentMember Member requester) {
        return ApiResponse.ok(candidateService.getCandidates(roomId, requester));
    }
}
