package com.sicmagroup.gpr.api.alerte;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.AlertDto;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.service.claim.ClaimServiceImpl;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/alert")
public class Alert {

    private final ClaimServiceImpl claimService;

    @GetMapping(value = "/claim")
    public ResponseEntity<ApiResponseDto> getAlertClaim() {

        List<AlertDto> claimAlertDtos = claimService.getAllAlertDtosByType(ClaimType.CLAIM);

        ApiResponseDto apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(claimAlertDtos)
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }

    @GetMapping(value = "/denun")
    public ResponseEntity<ApiResponseDto> getAlertDenun() {

        List<AlertDto> claimAlertDtos = claimService.getAllAlertDtosByType(ClaimType.DENUNCIACION);
        ApiResponseDto apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(claimAlertDtos)
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }

}
