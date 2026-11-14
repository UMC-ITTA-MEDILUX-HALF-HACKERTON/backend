package com.allbutyou.candidate.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.allbutyou.candidate.dto.CandidateResponse;
import com.allbutyou.candidate.service.CandidateService;
import com.allbutyou.common.auth.MemberAuthInterceptor;
import com.allbutyou.room.RoomFixtures;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.Room;
import com.allbutyou.room.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CandidateController.class)
class CandidateControllerTest {

    private static final String URL = "/api/rooms/1/candidates";

    @Autowired MockMvc mockMvc;
    @MockitoBean CandidateService candidateService;
    @MockitoBean MemberRepository memberRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        Room room = RoomFixtures.withId(RoomFixtures.room("A7K2QZ"), 1L);
        member = RoomFixtures.withId(Member.member(room, "지영"), 2L);
        given(memberRepository.findByToken(member.getToken())).willReturn(Optional.of(member));
    }

    @Test
    void 후보를_등록한다() throws Exception {
        given(candidateService.register(eq(1L), eq(member), any()))
                .willReturn(new CandidateResponse(10L, "성수 볼링장", 37.5446, 127.0559, "볼링", "지영", 0));

        mockMvc.perform(post(URL).header(MemberAuthInterceptor.HEADER, member.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placeName\":\"성수 볼링장\",\"lat\":37.5446,\"lng\":127.0559,\"activity\":\"볼링\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidateId").value(10))
                .andExpect(jsonPath("$.data.createdByNickname").value("지영"))
                .andExpect(jsonPath("$.data.voteCount").value(0));
    }

    @Test
    void activity가_없으면_400() throws Exception {
        mockMvc.perform(post(URL).header(MemberAuthInterceptor.HEADER, member.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placeName\":\"성수 볼링장\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
        verify(candidateService, never()).register(any(), any(), any());
    }

    @Test
    void 위도만_보내면_400() throws Exception {
        mockMvc.perform(post(URL).header(MemberAuthInterceptor.HEADER, member.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placeName\":\"성수 볼링장\",\"lat\":37.5,\"activity\":\"볼링\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }
}
