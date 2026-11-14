package com.allbutyou.poll.repository;

import com.allbutyou.poll.domain.PollOption;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    /** 여러 투표의 선택지를 한 번에. 투표별·등록 순으로 정렬. */
    @Query("select o from PollOption o where o.poll.id in :pollIds order by o.poll.id, o.id")
    List<PollOption> findAllByPollIds(@Param("pollIds") Collection<Long> pollIds);
}
