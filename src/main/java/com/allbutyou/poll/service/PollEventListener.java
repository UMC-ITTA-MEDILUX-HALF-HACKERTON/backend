package com.allbutyou.poll.service;

import com.allbutyou.common.websocket.Broadcaster;
import com.allbutyou.common.websocket.WsType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PollEventListener {

    private final Broadcaster broadcaster;

    /** 커밋 이후에 방송해야 클라이언트가 곧바로 GET 했을 때 같은 상태가 보인다. 롤백되면 방송하지 않는다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPollUpdated(PollUpdatedEvent event) {
        broadcaster.send(event.roomId(), WsType.POLL_UPDATE, event.poll());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPollDecided(PollDecidedEvent event) {
        broadcaster.send(event.roomId(), WsType.POLL_DECIDED, event.poll());
    }
}
