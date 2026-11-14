package com.allbutyou.poll.repository;

import com.allbutyou.poll.domain.Poll;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PollRepository extends JpaRepository<Poll, Long> {

    /** 생성 순. Poll#getRoomId() 때문에 메서드 이름 파생이 roomId 속성으로 해석돼 JPQL을 직접 쓴다. */
    @Query("select p from Poll p where p.room.id = :roomId order by p.id")
    List<Poll> findAllByRoomId(@Param("roomId") Long roomId);
}
