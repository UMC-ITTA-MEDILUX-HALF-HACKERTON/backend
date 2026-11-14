package com.allbutyou.common.auth;

import com.allbutyou.common.exception.BusinessException;
import com.allbutyou.common.exception.ErrorCode;
import com.allbutyou.room.domain.Member;
import com.allbutyou.room.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

@Component
public class MemberAuthInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Participant-Token";
    public static final String ATTRIBUTE = MemberAuthInterceptor.class.getName() + ".MEMBER";

    private static final List<PublicEndpoint> PUBLIC_ENDPOINTS = List.of(
            PublicEndpoint.of(HttpMethod.GET, "/api/health"),
            PublicEndpoint.of(HttpMethod.POST, "/api/rooms"),
            PublicEndpoint.of(HttpMethod.POST, "/api/rooms/join"),
            PublicEndpoint.of(null, "/swagger-ui/**"),
            PublicEndpoint.of(null, "/v3/api-docs/**")
    );

    private final MemberRepository memberRepository;

    public MemberAuthInterceptor(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)
                || HttpMethod.OPTIONS.matches(request.getMethod())
                || isPublic(request)) {
            return true;
        }

        String token = request.getHeader(HEADER);
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Member member = memberRepository.findByToken(token.strip())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 토큰입니다."));
        request.setAttribute(ATTRIBUTE, member);
        return true;
    }

    private boolean isPublic(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        PathContainer container = PathContainer.parsePath(path);
        return PUBLIC_ENDPOINTS.stream().anyMatch(e -> e.matches(request.getMethod(), container));
    }

    /** method가 null이면 모든 메서드 허용. */
    private record PublicEndpoint(HttpMethod method, PathPattern pattern) {

        static PublicEndpoint of(HttpMethod method, String pattern) {
            return new PublicEndpoint(method, PathPatternParser.defaultInstance.parse(pattern));
        }

        boolean matches(String requestMethod, PathContainer path) {
            return (method == null || method.matches(requestMethod)) && pattern.matches(path);
        }
    }
}
