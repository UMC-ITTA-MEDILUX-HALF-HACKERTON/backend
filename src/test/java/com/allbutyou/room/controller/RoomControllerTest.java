package com.allbutyou.room.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.allbutyou.common.auth.MemberAuthInterceptor;
import com.allbutyou.common.exception.BusinessException;
import com.allbutyou.common.exception.ErrorCode;
import com.allbutyou.room.RoomFixtures;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.Room;
import com.allbutyou.room.dto.CreateRoomResponse;
import com.allbutyou.room.dto.JoinRoomResponse;
import com.allbutyou.room.dto.MemberResponse;
import com.allbutyou.room.dto.RoomDetailResponse;
import com.allbutyou.room.repository.MemberRepository;
import com.allbutyou.room.service.RoomService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RoomController.class)
class RoomControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean RoomService roomService;
    @MockitoBean MemberRepository memberRepository;

    private static final String CREATE_BODY = """
            {"title":"금요일 저녁 모임","placeName":"강남역","lat":37.4979,"lng":127.0276,
             "startAt":"2026-11-20T19:00:00","lateFee":1000,"nickname":"민수"}
            """;

    @Test
    void 모임_생성은_토큰_없이_가능하다() throws Exception {
        given(roomService.createRoom(any())).willReturn(new CreateRoomResponse(1L, "A7K2QZ", 1L, "tok"));

        mockMvc.perform(post("/api/rooms").contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("A7K2QZ"))
                .andExpect(jsonPath("$.data.token").value("tok"));
    }

    @Test
    void 모임_생성_입력값이_잘못되면_400_INVALID_INPUT() throws Exception {
        String body = """
                {"title":" ","placeName":"강남역","lat":100,"lng":127.0,
                 "startAt":"2026-11-20T19:00:00","lateFee":-1,"nickname":"민수"}
                """;
        mockMvc.perform(post("/api/rooms").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void 참여는_토큰_없이_가능하다() throws Exception {
        given(roomService.join(any())).willReturn(new JoinRoomResponse(1L, 2L, "tok2"));

        mockMvc.perform(post("/api/rooms/join").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"a7k2qz\",\"nickname\":\"지영\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(2));
    }

    @Test
    void 참여_코드_형식이_틀리면_400() throws Exception {
        mockMvc.perform(post("/api/rooms/join").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ABC\",\"nickname\":\"지영\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void 닉네임_중복은_409_DUPLICATE() throws Exception {
        given(roomService.join(any())).willThrow(new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 닉네임입니다."));

        mockMvc.perform(post("/api/rooms/join").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"A7K2QZ\",\"nickname\":\"민수\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE"));
    }

    @Test
    void 모임_조회는_토큰이_없으면_401() throws Exception {
        mockMvc.perform(get("/api/rooms/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 모임_조회는_잘못된_토큰이면_401() throws Exception {
        given(memberRepository.findByToken("bad")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/rooms/1").header(MemberAuthInterceptor.HEADER, "bad"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 모임_조회는_토큰의_멤버를_주입받아_상세를_반환한다() throws Exception {
        Room room = RoomFixtures.withId(RoomFixtures.room("A7K2QZ"), 1L);
        Member host = RoomFixtures.withId(Member.host(room, "민수"), 1L);
        given(memberRepository.findByToken(host.getToken())).willReturn(Optional.of(host));
        given(roomService.getRoomDetail(eq(1L), eq(host))).willReturn(
                RoomDetailResponse.of(room, List.of(MemberResponse.from(host))));

        mockMvc.perform(get("/api/rooms/1").header(MemberAuthInterceptor.HEADER, host.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("A7K2QZ"))
                .andExpect(jsonPath("$.data.startAt").value("2026-11-20T19:00:00"))
                .andExpect(jsonPath("$.data.members[0].nickname").value("민수"))
                .andExpect(jsonPath("$.data.members[0].role").value("HOST"))
                .andExpect(jsonPath("$.data.members[0].arrivedAt").doesNotExist());
    }
}
