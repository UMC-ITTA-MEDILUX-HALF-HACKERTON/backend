package com.allbutyou.game.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/** 게임 시작 응답이자 WebSocket GAME_START 페이로드. */
@Schema(description = "게임 시작 결과와 팀 구성")
public record GameStartResponse(
        @Schema(description = "게임 ID", example = "1") Long gameId,
        @Schema(description = "시작 시각 (팀 기준 시각은 약속 시각)", example = "2026-11-20T19:05:00")
        LocalDateTime startedAt,
        @Schema(description = "지각자 팀: 미도착이거나 약속 시각 이후 도착 (참여 순)") List<TeamMemberResponse> lateTeam,
        @Schema(description = "도착자 팀: 약속 시각 이전(같은 시각 포함) 도착 (참여 순)") List<TeamMemberResponse> onTimeTeam
) {
}
