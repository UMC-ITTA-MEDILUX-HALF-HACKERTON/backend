package com.allbutyou.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 인증된 참가자를 주입한다.
 * TODO: Participant 엔티티가 생기면 파라미터 타입을 Participant(또는 participantId)로 사용.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentParticipant {
}
