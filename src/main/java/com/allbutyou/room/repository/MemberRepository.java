package com.allbutyou.room.repository;

import com.allbutyou.room.domain.Member;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    /** 인증용. room까지 함께 로딩해 open-in-view=false 환경에서도 member.getRoom() 접근이 안전하도록 한다. */
    @EntityGraph(attributePaths = "room")
    Optional<Member> findByToken(String token);

    boolean existsByRoomIdAndNickname(Long roomId, String nickname);

    List<Member> findAllByRoomIdOrderByIdAsc(Long roomId);
}
