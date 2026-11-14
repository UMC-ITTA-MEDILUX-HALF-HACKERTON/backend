package com.allbutyou.room.dto;

import com.allbutyou.room.domain.Member;
import com.allbutyou.room.domain.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "참가자")
public record MemberResponse(
        @Schema(description = "멤버 ID", example = "1") Long memberId,
        @Schema(description = "닉네임", example = "민수") String nickname,
        @Schema(description = "역할", example = "HOST") MemberRole role,
        @Schema(description = "도착 시각 (미도착이면 null)", example = "2026-11-20T19:05:00", nullable = true)
        LocalDateTime arrivedAt,
        @Schema(description = "지각 분 (미도착이면 null)", example = "5", nullable = true) Integer lateMinutes
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getNickname(), member.getRole(),
                member.getArrivedAt(), member.getLateMinutes());
    }
}
