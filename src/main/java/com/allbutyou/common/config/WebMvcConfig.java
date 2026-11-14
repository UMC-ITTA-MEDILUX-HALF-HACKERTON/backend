package com.allbutyou.common.config;

import com.allbutyou.common.auth.CurrentParticipantArgumentResolver;
import com.allbutyou.common.auth.ParticipantAuthInterceptor;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ParticipantAuthInterceptor participantAuthInterceptor;
    private final CurrentParticipantArgumentResolver currentParticipantArgumentResolver;

    public WebMvcConfig(ParticipantAuthInterceptor participantAuthInterceptor,
                        CurrentParticipantArgumentResolver currentParticipantArgumentResolver) {
        this.participantAuthInterceptor = participantAuthInterceptor;
        this.currentParticipantArgumentResolver = currentParticipantArgumentResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 공개 경로는 HTTP 메서드까지 구분해야 하므로 인터셉터 내부에서 판정한다.
        registry.addInterceptor(participantAuthInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentParticipantArgumentResolver);
    }
}
