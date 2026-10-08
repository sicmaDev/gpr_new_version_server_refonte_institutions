package com.sicmagroup.gpr.sla.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.dto.AlertDto;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.sla.dto.SlaRows.OverdueRow;
import com.sicmagroup.gpr.sla.repository.ClaimSlaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Alertes de retard (/api/v1/alert/* et tableau de bord) calculées par le moteur SLA : une seule règle pour
 * tous les écrans. Une seule lecture, seulement les colonnes utiles. Pour une dénonciation, le nom du
 * plaignant n'est jamais renvoyé.
 */
@Service
@RequiredArgsConstructor
public class SlaAlertService {

    private final ClaimSlaRepository repository;

    /** Plaintes de ce type dont l'échéance en vigueur est dépassée (tout périmètre), les plus anciennes d'abord. */
    public List<AlertDto> overdue(ClaimType type) {
        LocalDateTime now = LocalDateTime.now();
        List<AlertDto> out = new ArrayList<>();
        for (OverdueRow r : repository.overdueRows(type, now)) {
            long hours = Duration.between(r.dueAt(), now).toHours();
            out.add(AlertDto.builder()
                    .claimClient(type == ClaimType.DENUNCIACION ? null : r.clientName())
                    .claimCodeClient(r.codeClient())
                    .claimCode(r.code())
                    .claimId(r.claimId())
                    .objetLibelle(r.objet())
                    .gravity(r.risk())
                    .retardDay(hours / 24 + " jr(s) " + hours % 24 + " heure(s)")
                    .retardDayNumber(hours / 24)
                    .declenchedDate(r.dueAt())
                    .receiptDateTime(r.receiptDateTime())
                    .status(r.status())
                    .type(type)
                    .servicePointLibelle(r.agence())
                    .build());
        }
        return out;
    }
}