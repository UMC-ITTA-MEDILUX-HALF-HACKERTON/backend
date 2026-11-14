package com.allbutyou.game.repository;

import com.allbutyou.game.domain.GameParticipant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameParticipantRepository extends JpaRepository<GameParticipant, Long> {

    /** 참가자 전체. 응답에 닉네임·지각 분이 필요해 member를 함께 가져온다(N+1 방지). 참여 순. */
    @Query("select p from GameParticipant p join fetch p.member m where p.game.id = :gameId order by m.id")
    List<GameParticipant> findAllWithMember(@Param("gameId") Long gameId);
}
