package com.allbutyou.candidate.dto;

import com.allbutyou.room.domain.Member;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "아직 투표하지 않은 참가자")
public record NotVotedMemberResponse(
        @Schema(description = "멤버 ID", example = "3") Long memberId,
        @Schema(description = "닉네임", example = "지영") String nickname
) {

    public static NotVotedMemberResponse from(Member member) {
        return new NotVotedMemberResponse(member.getId(), member.getNickname());
    }
}
