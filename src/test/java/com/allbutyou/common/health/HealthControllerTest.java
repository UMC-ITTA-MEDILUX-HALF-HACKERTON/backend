package com.allbutyou.common.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.allbutyou.common.auth.CurrentParticipant;
import com.allbutyou.common.auth.ParticipantAuthInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(HealthController.class)
@Import(HealthControllerTest.ProtectedTestController.class)
class HealthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 헬스체크는_토큰_없이_ok를_반환한다() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("ok"));
    }

    @Test
    void 보호된_경로는_토큰이_없으면_401() throws Exception {
        mockMvc.perform(get("/api/test/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 토큰이_있으면_CurrentParticipant로_주입된다() throws Exception {
        mockMvc.perform(get("/api/test/me").header(ParticipantAuthInterceptor.HEADER, "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("abc"));
    }

    @RestController
    static class ProtectedTestController {
        @GetMapping("/api/test/me")
        String me(@CurrentParticipant Object participant) {
            return participant.toString();
        }
    }
}
