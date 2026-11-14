package com.allbutyou.game.dto;

import com.allbutyou.game.domain.GameOutcome;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 게임 결과 조회 응답이자 WebSocket GAME_RESULT 페이로드. 게임 시작 전(NOT_STARTED)이면 집계는 비어 있다. */
@Schema(description = "게임 결과 (지각자 팀 기준)")
public record GameResultResponse(
        @Schema(description = "게임 진행 상태", example = "PLAYING") GameProgress status,
        @Schema(description = "지각자 팀 평균 (제출자 없으면 null, 소수 둘째 자리 반올림)", example = "1200.5",
                nullable = true)
        Double lateAverage,
        @Schema(description = "도착자 팀 평균 (제출자 없으면 null, 소수 둘째 자리 반올림)", example = "1500.0",
                nullable = true)
        Double onTimeAverage,
        @Schema(description = "지각자 팀 제출 인원", example = "1") int lateSubmittedCount,
        @Schema(description = "지각자 팀 전체 인원 (시작 시 고정)", example = "2") int lateTotalCount,
        @Schema(description = "도착자 팀 제출 인원", example = "2") int onTimeSubmittedCount,
        @Schema(description = "도착자 팀 전체 인원 (시작 시 고정)", example = "3") int onTimeTotalCount,
        @Schema(description = "결과: WIN(지각비 0원) / LOSE(x2) / DRAW(기본) / PENDING(한 팀이라도 제출자 0명, 기본)",
                example = "LOSE")
        GameOutcome result,
        @Schema(description = "지각자 1인당 지각비", example = "2000") int penaltyPerPerson,
        @Schema(description = "지각자 목록 (참여 순)") List<LateMemberResponse> lateMembers,
        @Schema(description = "전체 제출 목록 (제출 순)") List<ScoreResponse> scores
) {
}
