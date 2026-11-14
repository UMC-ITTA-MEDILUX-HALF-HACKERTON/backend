package com.allbutyou.game.dto;

import com.allbutyou.game.domain.Game;

/** 결과 응답의 진행 상태. 게임 행이 없으면 NOT_STARTED. */
public enum GameProgress {
    NOT_STARTED,
    PLAYING,
    FINISHED;

    public static GameProgress of(Game game) {
        if (game == null) {
            return NOT_STARTED;
        }
        return game.isFinished() ? FINISHED : PLAYING;
    }
}
