package com.allbutyou.game.service;

import com.allbutyou.game.dto.GameResultResponse;

/** 트랜잭션 커밋 후 GAME_RESULT를 방송하기 위한 이벤트. */
public record GameResultEvent(Long roomId, GameResultResponse result) {
}
