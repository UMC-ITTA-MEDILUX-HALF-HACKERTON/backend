package com.allbutyou.poll.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.allbutyou.common.auth.MemberAuthInterceptor;
import com.allbutyou.poll.domain.PollStatus;
import com.allbutyou.poll.dto.PollOptionResponse;
import com.allbutyou.poll.dto.PollResponse;
import com.allbutyou.poll.service.PollService;
import com.allbutyou.room.RoomFixtures;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.Room;
import com.allbutyou.room.repository.MemberRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(PollController.class)
class PollControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PollService pollService;
    @MockitoBean MemberRepository memberRepository;

    private Member host;

    @BeforeEach
    void setUp() {
        Room room = RoomFixtures.withId(RoomFixtures.room("A7K2QZ"), 1L);
        host = RoomFixtures.withId(Member.host(room, "민수"), 2L);
        given(memberRepository.findByToken(host.getToken())).willReturn(Optional.of(host));
    }

    @Test
    void 투표를_생성한다() throws Exception {
        given(pollService.create(eq(1L), eq(host), any())).willReturn(new PollResponse(10L, "1차 메뉴",
                PollStatus.OPEN, null,
                List.of(new PollOptionResponse(100L, "삼겹살", null, null, null, 0),
                        new PollOptionResponse(101L, "볼링", "성수 볼링장", 37.5, 127.0, 0)),
                0, List.of(), null));

        mockMvc.perform(json(post("/api/rooms/1/polls"), """
                        {"title":"1차 메뉴","options":[{"label":"삼겹살"},
                         {"label":"볼링","placeName":"성수 볼링장","lat":37.5,"lng":127.0}]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pollId").value(10))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.options[1].placeName").value("성수 볼링장"));
    }

    @Test
    void 선택지가_2개_미만이면_400() throws Exception {
        mockMvc.perform(json(post("/api/rooms/1/polls"), """
                        {"title":"1차 메뉴","options":[{"label":"삼겹살"}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
        verify(pollService, never()).create(any(), any(), any());
    }

    @Test
    void 선택지_label이_비어_있으면_400() throws Exception {
        mockMvc.perform(json(post("/api/rooms/1/polls"), """
                        {"title":"1차 메뉴","options":[{"label":"삼겹살"},{"label":" "}]}
                        """))
                .andExpect(status().isBadRequest());
        verify(pollService, never()).create(any(), any(), any());
    }

    @Test
    void 위도만_보내면_400() throws Exception {
        mockMvc.perform(json(post("/api/polls/10/options"), """
                        {"label":"볼링","lat":37.5}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
        verify(pollService, never()).addOption(any(), any(), any());
    }

    @Test
    void optionId_없이_투표하면_400() throws Exception {
        mockMvc.perform(json(put("/api/polls/10/votes"), "{}"))
                .andExpect(status().isBadRequest());
        verify(pollService, never()).vote(any(), any(), any());
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        mockMvc.perform(put("/api/polls/10/votes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"optionId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.header(MemberAuthInterceptor.HEADER, host.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
