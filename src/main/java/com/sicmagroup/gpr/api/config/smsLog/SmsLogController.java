package com.sicmagroup.gpr.api.config.smsLog;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.SmsLogDto;
import com.sicmagroup.gpr.domain.enumeration.MessageChannel;
import com.sicmagroup.gpr.domain.enumeration.SmsStatus;
import com.sicmagroup.gpr.domain.enumeration.SmsType;
import com.sicmagroup.gpr.service.smsLog.SmsLogService;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;

/**
 * Journal des envois (SMS, e-mails, WhatsApp). Réservée à l'administration (droit H12) et en lecture seule : l'archive
 * n'est alimentée que par l'envoi des SMS, jamais à la main.
 */
@RestController
@RequestMapping("/api/v1/config/sms-log")
@RolesAllowed("H12")
@RequiredArgsConstructor
public class SmsLogController {

    private final SmsLogService smsLogService;

    @GetMapping
    public ResponseEntity<ApiResponseDto> getAll(
            @RequestParam(required = false) MessageChannel channel,
            @RequestParam(required = false) SmsType type,
            @RequestParam(required = false) SmsStatus status,
            @RequestParam(required = false) Long claimId,
            @RequestParam(required = false) Long suggestId,
            @RequestParam(required = false) String numeroDestinataire,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        List<SmsLogDto> logs = smsLogService.getAll();
        if (channel != null) {
            logs = logs.stream().filter(l -> l.getChannel() == channel).toList();
        }
        if (type != null) {
            logs = logs.stream().filter(l -> l.getType() == type).toList();
        }
        if (status != null) {
            logs = logs.stream().filter(l -> l.getStatus() == status).toList();
        }
        if (claimId != null) {
            logs = logs.stream().filter(l -> claimId.equals(l.getClaimId())).toList();
        }
        if (suggestId != null) {
            logs = logs.stream().filter(l -> suggestId.equals(l.getSuggestId())).toList();
        }
        if (numeroDestinataire != null && !numeroDestinataire.isBlank()) {
            String wanted = numeroDestinataire.toLowerCase();
            logs = logs.stream().filter(l -> l.getNumeroDestinataire() != null
                    && l.getNumeroDestinataire().toLowerCase().contains(wanted)).toList();
        }
        if (startDate != null && !startDate.isBlank()) {
            LocalDateTime start = LocalDateTime.parse(startDate);
            logs = logs.stream().filter(l -> l.getCreatedAt() != null && !l.getCreatedAt().isBefore(start)).toList();
        }
        if (endDate != null && !endDate.isBlank()) {
            LocalDateTime end = LocalDateTime.parse(endDate);
            logs = logs.stream().filter(l -> l.getCreatedAt() != null && !l.getCreatedAt().isAfter(end)).toList();
        }
        return ResponseEntity.ok(ApiResponseDto.builder().status(true).content(logs).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto> getById(@PathVariable Long id) {
        return smsLogService.getById(id)
                .map(log -> ResponseEntity.ok(ApiResponseDto.builder().status(true).content(log).build()))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseDto.builder().status(false).content("SMS introuvable").build()));
    }
}
