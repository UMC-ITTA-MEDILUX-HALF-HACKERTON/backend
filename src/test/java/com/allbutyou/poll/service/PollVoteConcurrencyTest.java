package com.allbutyou.poll.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.allbutyou.poll.domain.Poll;
import com.allbutyou.poll.domain.PollOption;
import com.allbutyou.poll.dto.PollResponse;
import com.allbutyou.poll.repository.PollOptionRepository;
import com.allbutyou.poll.repository.PollRepository;
import com.allbutyou.poll.repository.VoteRepository;
import com.allbutyou.room.RoomFixtures;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.Room;
import com.allbutyou.room.repository.MemberRepository;
import com.allbutyou.room.repository.RoomRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 테스트 트랜잭션 없이 실제 커밋으로 같은 멤버의 첫 투표를 동시에 보낸다.
 * UK(poll_id, member_id) 충돌이 나더라도 모든 요청이 성공하고 표는 한 행만 남아야 한다.
 */
@SpringBootTest
@AutoConfigureTestDatabase
class PollVoteConcurrencyTest {

    private static final int THREADS = 8;

    @Autowired PollService pollService;
    @Autowired RoomRepository roomRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired PollRepository pollRepository;
    @Autowired PollOptionRepository optionRepository;
    @Autowired VoteRepository voteRepository;

    @AfterEach
    void tearDown() {
        voteRepository.deleteAllInBatch();
        optionRepository.deleteAllInBatch();
        pollRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();
    }

    @Test
    void 같은_멤버의_첫_투표가_동시에_와도_모두_성공하고_한_표만_남는다() throws Exception {
        Room room = roomRepository.save(RoomFixtures.room("CON123"));
        memberRepository.save(Member.host(room, "민수"));
        Member member = memberRepository.save(Member.member(room, "지영"));
        Poll poll = pollRepository.save(Poll.create(room, "1차 메뉴"));
        PollOption pork = optionRepository.save(PollOption.create(poll, "삼겹살", null, null, null));
        PollOption chicken = optionRepository.save(PollOption.create(poll, "치킨", null, null, null));

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<PollResponse>> results = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            Long optionId = (i % 2 == 0 ? pork : chicken).getId();
            results.add(executor.submit(() -> {
                start.await();
                return pollService.vote(poll.getId(), member, optionId);
            }));
        }
        start.countDown();

        for (Future<PollResponse> result : results) {
            assertThat(result.get(10, TimeUnit.SECONDS).votedCount()).isEqualTo(1);
        }
        executor.shutdown();

        assertThat(voteRepository.count()).isEqualTo(1);
    }
}
