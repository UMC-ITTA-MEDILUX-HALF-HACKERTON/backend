package com.allbutyou.room.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.allbutyou.common.auth.MemberAuthInterceptor;
import com.allbutyou.common.exception.BusinessException;
import com.allbutyou.common.exception.ErrorCode;
import com.allbutyou.room.RoomFixtures;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.Room;
import com.allbutyou.room.dto.ArrivalResponse;
import com.allbutyou.room.repository.MemberRepository;
import com.allbutyou.room.service.ArrivalService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArrivalController.class)
class ArrivalControllerTest {

    private static final String URL = "/api/rooms/1/members/2/arrival";

    @Autowired MockMvc mockMvc;
    @MockitoBean ArrivalService arrivalService;
    @MockitoBean MemberRepository memberRepository;

    private Member host;

    @BeforeEach
    void setUp() {
        Room room = RoomFixtures.withId(RoomFixtures.room("A7K2QZ"), 1L);
        host = RoomFixtures.withId(Member.host(room, "민수"), 1L);
        given(memberRepository.findByToken(host.getToken())).willReturn(Optional.of(host));
    }

    @Test
    void 도착_시각을_지정하면_지각분을_포함해_응답한다() throws Exception {
        LocalDateTime arrivedAt = LocalDateTime.of(2026, 11, 20, 19, 5);
        given(arrivalService.markArrived(eq(1L), eq(2L), eq(host), eq(arrivedAt)))
                .willReturn(new ArrivalResponse(2L, "지영", arrivedAt, 5));

        mockMvc.perform(patch(URL).header(MemberAuthInterceptor.HEADER, host.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"arrivedAt\":\"2026-11-20T19:05:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(2))
                .andExpect(jsonPath("$.data.nickname").value("지영"))
                .andExpect(jsonPath("$.data.arrivedAt").value("2026-11-20T19:05:00"))
                .andExpect(jsonPath("$.data.lateMinutes").value(5));
    }

    @Test
    void arrivedAt이_null이거나_본문이_없으면_null을_넘긴다() throws Exception {
        given(arrivalService.markArrived(eq(1L), eq(2L), eq(host), isNull()))
                .willReturn(new ArrivalResponse(2L, "지영", LocalDateTime.of(2026, 11, 20, 19, 0), 0));

        mockMvc.perform(patch(URL).header(MemberAuthInterceptor.HEADER, host.getToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"arrivedAt\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lateMinutes").value(0));

        mockMvc.perform(patch(URL).header(MemberAuthInterceptor.HEADER, host.getToken()))
                .andExpect(status().isOk());
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 주최자가_아니면_403_FORBIDDEN() throws Exception {
        given(arrivalService.markArrived(any(), any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.FORBIDDEN, "주최자만 할 수 있습니다."));

        mockMvc.perform(patch(URL).header(MemberAuthInterceptor.HEADER, host.getToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void 대상이_모임_소속이_아니면_404() throws Exception {
        given(arrivalService.markArrived(any(), any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.NOT_FOUND, "모임에서 참가자를 찾을 수 없습니다."));

        mockMvc.perform(patch(URL).header(MemberAuthInterceptor.HEADER, host.getToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void 날짜_형식이_잘못되면_400() throws Exception {
        mockMvc.perform(patch(URL).header(MemberAuthInterceptor.HEADER, host.getToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"arrivedAt\":\"어제\"}"))
                .andExpect(status().isBadRequest());
    }
}
