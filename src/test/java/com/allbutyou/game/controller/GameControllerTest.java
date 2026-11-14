package com.allbutyou.game.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.allbutyou.common.auth.MemberAuthInterceptor;
import com.allbutyou.game.domain.GameOutcome;
import com.allbutyou.game.dto.GameProgress;
import com.allbutyou.game.dto.GameResultResponse;
import com.allbutyou.game.dto.GameStartResponse;
import com.allbutyou.game.dto.TeamMemberResponse;
import com.allbutyou.game.service.GameService;
import com.allbutyou.room.RoomFixtures;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.Room;
import com.allbutyou.room.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GameController.class)
class GameControllerTest {

    private static final String SCORES_URL = "/api/rooms/1/game/scores";

    @Autowired MockMvc mockMvc;
    @MockitoBean GameService gameService;
    @MockitoBean MemberRepository memberRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        Room room = RoomFixtures.withId(RoomFixtures.room("A7K2QZ"), 1L);
        member = RoomFixtures.withId(Member.member(room, "지영"), 2L);
        given(memberRepository.findByToken(member.getToken())).willReturn(Optional.of(member));
    }

    @Test
    void 점수를_제출하면_현재_결과를_돌려준다() throws Exception {
        given(gameService.submitScore(1L, member, 100000)).willReturn(pending());

        mockMvc.perform(post(SCORES_URL).header(MemberAuthInterceptor.HEADER, member.getToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"score\":100000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("PENDING"))
                .andExpect(jsonPath("$.data.penaltyPerPerson").value(1000))
                .andExpect(jsonPath("$.data.lateAverage").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"score\":100001}", "{\"score\":-1}", "{}", "{\"score\":\"abc\"}"})
    void 점수가_범위를_벗어나거나_없으면_400(String body) throws Exception {
        mockMvc.perform(post(SCORES_URL).header(MemberAuthInterceptor.HEADER, member.getToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
        verify(gameService, never()).submitScore(any(), any(), any(Integer.class));
    }

    @Test
    void 결과를_조회한다() throws Exception {
        given(gameService.getResult(1L, member)).willReturn(pending());

        mockMvc.perform(get("/api/rooms/1/game/result").header(MemberAuthInterceptor.HEADER, member.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PLAYING"))
                .andExpect(jsonPath("$.data.result").value("PENDING"));
    }

    @Test
    void 게임을_시작한다() throws Exception {
        given(gameService.start(1L, member)).willReturn(new GameStartResponse(5L,
                LocalDateTime.of(2026, 11, 20, 19, 5),
                List.of(new TeamMemberResponse(3L, "철수")), List.of(new TeamMemberResponse(2L, "지영"))));

        mockMvc.perform(post("/api/rooms/1/game/start").header(MemberAuthInterceptor.HEADER, member.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gameId").value(5))
                .andExpect(jsonPath("$.data.lateTeam[0].nickname").value("철수"))
                .andExpect(jsonPath("$.data.onTimeTeam[0].memberId").value(2));
    }

    @Test
    void 게임을_종료한다() throws Exception {
        given(gameService.finish(1L, member)).willReturn(pending());

        mockMvc.perform(post("/api/rooms/1/game/finish").header(MemberAuthInterceptor.HEADER, member.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("PENDING"));
    }

    private static GameResultResponse pending() {
        return new GameResultResponse(GameProgress.PLAYING, null, 1000.0, 0, 1, 1, 1, GameOutcome.PENDING, 1000, List.of(), List.of());
    }
}
