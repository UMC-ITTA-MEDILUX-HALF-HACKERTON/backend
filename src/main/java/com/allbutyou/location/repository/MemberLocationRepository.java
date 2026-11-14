
package com.allbutyou.location.repository;

import com.allbutyou.location.domain.MemberLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberLocationRepository
        extends JpaRepository<MemberLocation, Long> {

    // 특정 참가자의 GPS 정보 조회
    Optional<MemberLocation> findByMemberId(Long memberId);

    // 특정 모임의 참가자 GPS 정보 전체 조회
    List<MemberLocation> findAllByMemberRoomId(Long roomId);
}
