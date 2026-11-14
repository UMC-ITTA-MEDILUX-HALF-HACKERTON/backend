package com.allbutyou.common.config;

import com.allbutyou.common.auth.CurrentMemberArgumentResolver;
import com.allbutyou.common.auth.MemberAuthInterceptor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final MemberAuthInterceptor memberAuthInterceptor;
    private final CurrentMemberArgumentResolver currentMemberArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 공개 경로(메서드 구분 포함, swagger 경로)는 인터셉터 내부 PUBLIC_ENDPOINTS에서 판정한다.
        registry.addInterceptor(memberAuthInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentMemberArgumentResolver);
    }
}
