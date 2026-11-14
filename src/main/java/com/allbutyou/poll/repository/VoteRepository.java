package com.allbutyou.poll.repository;

import com.allbutyou.poll.domain.Vote;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findByPollIdAndMemberId(Long pollId, Long memberId);

    /** 선택지별 득표 수. 표가 없는 선택지는 결과에 없다. */
    @Query("""
            select new com.allbutyou.poll.repository.OptionVoteCount(v.poll.id, v.option.id, count(v))
            from Vote v
            where v.poll.id in :pollIds
            group by v.poll.id, v.option.id
            """)
    List<OptionVoteCount> countByOption(@Param("pollIds") Collection<Long> pollIds);

    /** 투표별 투표자와 선택. UK(poll_id, member_id)로 (poll, member)당 한 행. FK 컬럼만 읽어 조인 없음. */
    @Query("""
            select new com.allbutyou.poll.repository.VoterChoice(v.poll.id, v.member.id, v.option.id)
            from Vote v
            where v.poll.id in :pollIds
            """)
    List<VoterChoice> findVoterChoices(@Param("pollIds") Collection<Long> pollIds);
}
