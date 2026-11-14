package com.allbutyou.room.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.allbutyou.common.websocket.Broadcaster;
import com.allbutyou.common.websocket.WsType;
import com.allbutyou.room.RoomFixtures;
import com.allbutyou.room.dto.ArrivalResponse;
import org.junit.jupiter.api.Test;

class RoomEventListenerTest {

    private final Broadcaster broadcaster = mock(Broadcaster.class);
    private final RoomEventListener listener = new RoomEventListener(broadcaster);

    @Test
    void 도착_이벤트는_ARRIVED로_방송한다() {
        ArrivalResponse arrival = new ArrivalResponse(2L, "지영", RoomFixtures.START_AT.plusMinutes(5), 5);

        listener.onMemberArrived(new MemberArrivedEvent(1L, arrival));

        verify(broadcaster).send(1L, WsType.ARRIVED, arrival);
    }
}
