package com.allbutyou.common.auth;

import com.allbutyou.common.exception.BusinessException;
import com.allbutyou.common.exception.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentParticipantArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // TODO: Participant 엔티티 생성 후 파라미터 타입 검사 추가
        //   && Participant.class.isAssignableFrom(parameter.getParameterType())
        return parameter.hasParameterAnnotation(CurrentParticipant.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object participant = webRequest.getAttribute(ParticipantAuthInterceptor.ATTRIBUTE,
                RequestAttributes.SCOPE_REQUEST);
        if (participant == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return participant;
    }
}
