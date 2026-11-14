
package com.allbutyou.location.controller;

import com.allbutyou.location.dto.LocationResponse;
import com.allbutyou.location.dto.LocationUpdateRequest;
import com.allbutyou.location.dto.MemberLocationResponse;
import com.allbutyou.location.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    // 참가자 GPS 위치 갱신
    @PatchMapping("/members/{memberId}/location")
    public ResponseEntity<LocationResponse> updateLocation(
            @PathVariable Long roomId,
            @PathVariable Long memberId,
            @RequestHeader("X-Member-Token") String token,
            @RequestBody LocationUpdateRequest request
    ) {
        LocationResponse response = locationService.updateLocation(
                roomId,
                memberId,
                token,
                request
        );

        return ResponseEntity.ok(response);
    }

    // 모임 참가자 전체 GPS 현황 조회
    @GetMapping("/locations")
    public ResponseEntity<List<MemberLocationResponse>> getRoomLocations(
            @PathVariable Long roomId,
            @RequestHeader("X-Member-Token") String token
    ) {
        List<MemberLocationResponse> response =
                locationService.getRoomLocations(roomId, token);

        return ResponseEntity.ok(response);
    }
}
