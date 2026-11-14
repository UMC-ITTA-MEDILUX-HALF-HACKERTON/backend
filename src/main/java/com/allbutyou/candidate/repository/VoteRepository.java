package com.allbutyou.candidate.repository;

import com.allbutyou.candidate.domain.Vote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    /** 후보별 득표 수. 표가 없는 후보는 결과에 없다. */
    @Query("""
            select new com.allbutyou.candidate.repository.CandidateVoteCount(v.candidate.id, count(v))
            from Vote v
            where v.room.id = :roomId
            group by v.candidate.id
            """)
    List<CandidateVoteCount> countByCandidate(@Param("roomId") Long roomId);

    /** 해당 모임에서 투표한 멤버 ID. UK(room_id, member_id)로 중복 없음. */
    @Query("select v.member.id from Vote v where v.room.id = :roomId")
    List<Long> findVoterIds(@Param("roomId") Long roomId);
}
