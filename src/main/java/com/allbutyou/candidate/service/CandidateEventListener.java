package com.allbutyou.candidate.service;

import com.allbutyou.common.websocket.Broadcaster;
import com.allbutyou.common.websocket.WsType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CandidateEventListener {

    private final Broadcaster broadcaster;

    /** 커밋 이후에 방송해야 클라이언트가 곧바로 GET 했을 때 새 후보가 보인다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCandidateUpdated(CandidateUpdatedEvent event) {
        broadcaster.send(event.roomId(), WsType.CANDIDATE_UPDATE, event.candidates());
    }
}
