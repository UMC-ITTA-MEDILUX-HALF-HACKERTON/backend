package com.allbutyou.common.config;

import com.allbutyou.common.auth.MemberAuthInterceptor;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "participantToken";

    @Bean
    public OpenAPI allButYouOpenApi() {
        SecurityScheme tokenScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(MemberAuthInterceptor.HEADER)
                .description("모임 생성/참여 응답의 token 값");

        return new OpenAPI()
                .info(new Info().title("너빼고 다옴 API").version("v1"))
                .components(new Components().addSecuritySchemes(SCHEME_NAME, tokenScheme))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }
}
