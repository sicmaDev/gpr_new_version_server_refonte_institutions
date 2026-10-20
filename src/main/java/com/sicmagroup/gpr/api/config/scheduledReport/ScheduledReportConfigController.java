package com.sicmagroup.gpr.api.config.scheduledReport;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.cron.AlertRetardCron;
import com.sicmagroup.gpr.domain.dto.ScheduledReportTestResult;
import com.sicmagroup.gpr.domain.enumeration.ReportRecipientType;
import com.sicmagroup.gpr.domain.model.ScheduledReportConfig;
import com.sicmagroup.gpr.service.scheduledReport.ScheduledReportConfigService;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;

/** Planification des relances de retard par e-mail (page « Fréquences alertes »), réservée à l'administration. */
@RestController
@RequestMapping("/api/v1/config/scheduled-reports")
@RolesAllowed("H12")
@RequiredArgsConstructor
public class ScheduledReportConfigController {

    private final ScheduledReportConfigService service;
    private final AlertRetardCron alertRetardCron;

    @GetMapping
    public ResponseEntity<List<ScheduledReportConfig>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ScheduledReportConfig request) {
        try {
            return ResponseEntity.ok(service.update(id, request));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    /** Envoi de test à une adresse choisie : sans effet sur la planification réelle. */
    @PostMapping("/{id}/test")
    public ResponseEntity<?> sendTest(@PathVariable Long id, @RequestBody ScheduledReportTestRequest request) {
        try {
            ReportRecipientType type = service.getRecipientType(id);
            ScheduledReportTestResult result = alertRetardCron.sendTestMail(type, request.getTo());
            return result.isSuccess()
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    /** Fuseau horaire unique appliqué à toute la planification (agents et Pilote). */
    @PutMapping("/timezone")
    public ResponseEntity<?> updateTimezone(@RequestBody ScheduledReportTimezoneRequest request) {
        try {
            return ResponseEntity.ok(service.updateTimezone(request.getTimezone()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }
}
