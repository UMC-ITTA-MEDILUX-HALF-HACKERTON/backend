package com.allbutyou.game.controller;

import com.allbutyou.common.auth.CurrentMember;
import com.allbutyou.common.response.ApiResponse;
import com.allbutyou.game.dto.GameResultResponse;
import com.allbutyou.game.dto.GameStartResponse;
import com.allbutyou.game.dto.SubmitScoreRequest;
import com.allbutyou.game.service.GameService;
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

@Tag(name = "Game", description = "지각자 팀 vs 도착자 팀 평균 점수 대결과 지각비 결과")
@RestController
@RequestMapping("/api/rooms/{roomId}/game")
@RequiredArgsConstructor
public class GameController {

    private static final String RESULT_BROADCAST =
            " 성공 시 커밋 후 /topic/rooms/{roomId}로 GAME_RESULT(결과 조회 응답과 같은 형태)를 방송한다.";

    private final GameService gameService;

    @Operation(summary = "게임 시작 (HOST)",
            description = "약속 시각 이후에 방장이 게임을 시작한다. 이 순간의 모임 멤버 전원이 참가자가 되고 팀이 고정된다. "
                    + "도착자(ON_TIME): arrivedAt <= 약속 시각, 지각자(LATE): 미도착이거나 arrivedAt > 약속 시각. "
                    + "약속 시각 전이거나 지각자가 0명이면 INVALID_INPUT(400), 이미 게임이 있으면 DUPLICATE(409), "
                    + "모임 참가자가 아니거나 HOST가 아니면 FORBIDDEN(403). "
                    + "성공 시 커밋 후 /topic/rooms/{roomId}로 GAME_START(이 응답과 같은 형태)를 방송한다.")
    @PostMapping("/start")
    public ApiResponse<GameStartResponse> start(@PathVariable Long roomId,
                                                @Parameter(hidden = true) @CurrentMember Member requester) {
        return ApiResponse.ok(gameService.start(roomId, requester));
    }

    @Operation(summary = "게임 점수 제출",
            description = "게임 참가자가 자기 점수를 1회 제출한다(0~100000). 응답은 제출 후 현재 결과 전체. "
                    + "참가자 전원이 제출하면 자동으로 FINISHED가 된다. "
                    + "점수 범위를 벗어나거나 게임이 없거나 종료됐으면 INVALID_INPUT(400), 이미 제출했으면 DUPLICATE(409), "
                    + "모임 멤버가 아니거나 게임 시작 후 들어와 참가자가 아니면 FORBIDDEN(403)." + RESULT_BROADCAST)
    @PostMapping("/scores")
    public ApiResponse<GameResultResponse> submitScore(@PathVariable Long roomId,
                                                       @Valid @RequestBody SubmitScoreRequest request,
                                                       @Parameter(hidden = true) @CurrentMember Member requester) {
        return ApiResponse.ok(gameService.submitScore(roomId, requester, request.score()));
    }

    @Operation(summary = "게임 종료 (HOST)",
            description = "미제출자가 있어도 게임을 종료한다. 게임이 없거나 이미 종료됐으면 INVALID_INPUT(400), "
                    + "모임 참가자가 아니거나 HOST가 아니면 FORBIDDEN(403)." + RESULT_BROADCAST)
    @PostMapping("/finish")
    public ApiResponse<GameResultResponse> finish(@PathVariable Long roomId,
                                                  @Parameter(hidden = true) @CurrentMember Member requester) {
        return ApiResponse.ok(gameService.finish(roomId, requester));
    }

    @Operation(summary = "게임 결과 조회",
            description = "status(NOT_STARTED/PLAYING/FINISHED)와 시작 시 고정된 팀별 제출 점수 평균을 비교한 "
                    + "지각자 팀 기준 결과 WIN(지각비 0원) / LOSE(기본 x2) / DRAW(기본) / PENDING(한 팀이라도 제출자 0명, 기본), "
                    + "지각자별 지각비, 전체 제출 목록을 조회한다. 시작 전이면 집계가 비어 있고 PENDING. "
                    + "모임 참가자가 아니면 FORBIDDEN(403).")
    @GetMapping("/result")
    public ApiResponse<GameResultResponse> getResult(@PathVariable Long roomId,
                                                     @Parameter(hidden = true) @CurrentMember Member requester) {
        return ApiResponse.ok(gameService.getResult(roomId, requester));
    }
}
