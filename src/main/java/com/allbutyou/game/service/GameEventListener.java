package com.allbutyou.game.service;

import com.allbutyou.common.websocket.Broadcaster;
import com.allbutyou.common.websocket.WsType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class GameEventListener {

    private final Broadcaster broadcaster;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGameStarted(GameStartedEvent event) {
        broadcaster.send(event.roomId(), WsType.GAME_START, event.game());
    }

    /** 커밋 이후에 방송해야 클라이언트가 곧바로 GET 했을 때 같은 결과가 보인다. 롤백되면 방송하지 않는다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGameResult(GameResultEvent event) {
        broadcaster.send(event.roomId(), WsType.GAME_RESULT, event.result());
    }
}
