package com.allbutyou.common.health;

import com.allbutyou.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "서버 상태 확인")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Operation(summary = "헬스 체크", description = "서버가 살아 있으면 data에 \"ok\"를 반환한다.")
    @SecurityRequirements
    @GetMapping
    public ApiResponse<String> health() {
        return ApiResponse.ok("ok");
    }
}
