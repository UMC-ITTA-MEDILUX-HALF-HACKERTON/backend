package com.allbutyou.game.dto;

import com.allbutyou.room.domain.Member;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "팀 구성원")
public record TeamMemberResponse(
        @Schema(description = "멤버 ID", example = "3") Long memberId,
        @Schema(description = "닉네임", example = "철수") String nickname
) {

    public static TeamMemberResponse from(Member member) {
        return new TeamMemberResponse(member.getId(), member.getNickname());
    }
}
