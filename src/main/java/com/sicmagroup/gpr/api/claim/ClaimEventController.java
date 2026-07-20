package com.sicmagroup.gpr.api.claim;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.service.claimEvent.ClaimEventServiceImpl;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/claim-events")
@RequiredArgsConstructor
public class ClaimEventController {

    private final ClaimEventServiceImpl service;

    @GetMapping("/{claimId}")
    public ResponseEntity<ApiResponseDto> getByClaimId(@PathVariable Long claimId) {
        List<ClaimEventResponse> events = service.getByClaimId(claimId);
        return ResponseEntity.ok(ApiResponseDto.builder()
                .status(true)
                .content(events)
                .build());
    }
}
