package com.allbutyou.candidate.repository;

import com.allbutyou.candidate.domain.Candidate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    /** 등록 순. 응답에 등록자 닉네임이 필요해 createdBy를 함께 로딩한다(N+1 방지). */
    @EntityGraph(attributePaths = "createdBy")
    List<Candidate> findAllByRoomIdOrderByIdAsc(Long roomId);
}
