package com.allbutyou.common.auth;

import com.allbutyou.common.exception.BusinessException;
import com.allbutyou.common.exception.ErrorCode;
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
public class ParticipantAuthInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Participant-Token";
    public static final String ATTRIBUTE = ParticipantAuthInterceptor.class.getName() + ".PARTICIPANT";

    private static final List<PublicEndpoint> PUBLIC_ENDPOINTS = List.of(
            PublicEndpoint.of(HttpMethod.GET, "/api/health"),
            PublicEndpoint.of(HttpMethod.POST, "/api/gatherings"),
            PublicEndpoint.of(HttpMethod.POST, "/api/gatherings/*/join"),
            PublicEndpoint.of(HttpMethod.GET, "/api/gatherings/*")
    );

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

        // TODO: Participant 엔티티/리포지토리 생성 후 토큰으로 참가자 조회
        //   Participant participant = participantRepository.findByToken(token)
        //           .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        //   request.setAttribute(ATTRIBUTE, participant);
        // 현재는 구조만 잡기 위해 원본 토큰을 저장한다. (검증 없음)
        request.setAttribute(ATTRIBUTE, token);
        return true;
    }

    private boolean isPublic(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        PathContainer container = PathContainer.parsePath(path);
        return PUBLIC_ENDPOINTS.stream().anyMatch(e -> e.matches(request.getMethod(), container));
    }

    private record PublicEndpoint(HttpMethod method, PathPattern pattern) {

        static PublicEndpoint of(HttpMethod method, String pattern) {
            return new PublicEndpoint(method, PathPatternParser.defaultInstance.parse(pattern));
        }

        boolean matches(String requestMethod, PathContainer path) {
            return method.matches(requestMethod) && pattern.matches(path);
        }
    }
}
