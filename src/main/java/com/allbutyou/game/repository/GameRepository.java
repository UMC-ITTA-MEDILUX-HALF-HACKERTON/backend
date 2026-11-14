package com.allbutyou.game.repository;

import com.allbutyou.game.domain.Game;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRepository extends JpaRepository<Game, Long> {

    @Query("select g from Game g where g.room.id = :roomId")
    Optional<Game> findByRoomId(@Param("roomId") Long roomId);

    /**
     * 제출·종료는 게임 행을 잠그고 직렬화한다. 같은 멤버의 중복 제출, 마지막 두 명이 동시에 내서
     * 자동 종료가 빠지는 경우, 종료와 제출이 겹치는 경우를 막는다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Game g where g.room.id = :roomId")
    Optional<Game> findByRoomIdForUpdate(@Param("roomId") Long roomId);
}
