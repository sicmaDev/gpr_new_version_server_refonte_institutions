package com.sicmagroup.gpr.api.alerte;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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
import com.sicmagroup.gpr.sla.service.SlaAlertService;
import com.sicmagroup.gpr.sla.service.SlaConfig;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/alert")
public class Alert {

    private final ClaimServiceImpl claimService;
    private final SlaConfig slaConfig;
    private final SlaAlertService slaAlertService;

    // SLA actif : le moteur est la seule source de vérité ; sinon, ancien calcul inchangé
    private List<AlertDto> alerts(ClaimType type) {
        if (slaConfig.enabled()) {
            return slaAlertService.overdue(type);
        }
        return claimService.getAllAlertDtosByType(type);
    }

    @GetMapping(value = "/claim")
    public ResponseEntity<ApiResponseDto> getAlertClaim() {

        List<AlertDto> claimAlertDtos = alerts(ClaimType.CLAIM)
            .stream()
            .sorted((c1, c2) -> c2.getDeclenchedDate().compareTo(c1.getDeclenchedDate()))
            .collect(Collectors.toList()); 

        ApiResponseDto apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(claimAlertDtos)
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }

    @GetMapping(value = "/denun")
    public ResponseEntity<ApiResponseDto> getAlertDenun() {

        List<AlertDto> claimAlertDtos = alerts(ClaimType.DENUNCIACION)
            .stream()
            .sorted((c1, c2) -> c2.getDeclenchedDate().compareTo(c1.getDeclenchedDate()))
            .collect(Collectors.toList());
            
        ApiResponseDto apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(claimAlertDtos)
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }

}
