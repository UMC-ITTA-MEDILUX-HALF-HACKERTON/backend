package com.allbutyou.game.service;

import com.allbutyou.game.dto.GameStartResponse;

/** 트랜잭션 커밋 후 GAME_START를 방송하기 위한 이벤트. */
public record GameStartedEvent(Long roomId, GameStartResponse game) {
}
