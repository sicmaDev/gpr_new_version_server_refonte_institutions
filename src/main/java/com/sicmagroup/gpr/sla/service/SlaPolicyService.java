package com.sicmagroup.gpr.sla.service;

import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.GravityLevel;
import com.sicmagroup.gpr.sla.domain.BusinessDay;
import com.sicmagroup.gpr.sla.domain.SlaBreachReason;
import com.sicmagroup.gpr.sla.domain.SlaPolicy;
import com.sicmagroup.gpr.sla.repository.BusinessDayRepository;
import com.sicmagroup.gpr.sla.repository.SlaBreachReasonRepository;
import com.sicmagroup.gpr.sla.repository.SlaPolicyRepository;

import lombok.RequiredArgsConstructor;

/** Politiques de délais : lecture, valeurs par défaut et initialisation de la configuration. */
@Service
@RequiredArgsConstructor
public class SlaPolicyService {

    /** Un jour ouvré par défaut = 9 heures (8 h - 17 h). */
    private static final int DAY = 540;

    private final SlaPolicyRepository policyRepository;
    private final BusinessDayRepository dayRepository;
    private final SlaBreachReasonRepository reasonRepository;

    /** Politique applicable : celle enregistrée si elle est active, sinon la valeur par défaut. */
    public SlaPolicy policyFor(ClaimType type, GravityLevel risk) {
        GravityLevel level = risk == null ? GravityLevel.MOYEN : risk;
        return policyRepository.findByClaimTypeAndRiskLevel(type, level)
                .filter(SlaPolicy::isActive)
                .orElseGet(() -> defaultPolicy(type, level));
    }

    /** Valeurs par défaut de la spécification (jours ouvrés, rappels à 50 % et 75 %, grâce d'un jour ouvré). */
    public static SlaPolicy defaultPolicy(ClaimType type, GravityLevel risk) {
        int takeover;
        int resolution;
        int closure;
        switch (risk) {
            case GRAVE -> {
                takeover = 240;
                resolution = 3 * DAY;
                closure = 2 * DAY;
            }
            case MINEUR -> {
                takeover = 2 * DAY;
                resolution = 10 * DAY;
                closure = 5 * DAY;
            }
            default -> {
                takeover = DAY;
                resolution = 6 * DAY;
                closure = 3 * DAY;
            }
        }
        if (type == ClaimType.DENUNCIACION) {
            closure = 0; // pas de mesure de satisfaction : TREAT vaut clôture
        }
        if (type == ClaimType.SUGGESTION) {
            takeover = 0;
            resolution = 10 * DAY;
            closure = 0;
        }
        return SlaPolicy.builder()
                .claimType(type)
                .riskLevel(risk)
                .businessTime(true)
                .takeoverMinutes(takeover)
                .resolutionMinutes(resolution)
                .closureMinutes(closure)
                .reopenMinutes(closure)
                .reminder1Pct(50)
                .reminder2Pct(75)
                .graceMinutes(DAY)
                .complianceTarget(90)
                .active(true)
                .build();
    }

    /** Crée les politiques, le calendrier et les motifs par défaut s'ils n'existent pas encore. */
    @Transactional
    public void seedDefaults() {
        for (ClaimType type : List.of(ClaimType.CLAIM, ClaimType.DENUNCIACION)) {
            for (GravityLevel risk : List.of(GravityLevel.GRAVE, GravityLevel.MOYEN, GravityLevel.MINEUR)) {
                if (policyRepository.findByClaimTypeAndRiskLevel(type, risk).isEmpty()) {
                    policyRepository.save(defaultPolicy(type, risk));
                }
            }
        }
        if (policyRepository.findByClaimTypeAndRiskLevel(ClaimType.SUGGESTION, GravityLevel.MOYEN).isEmpty()) {
            policyRepository.save(defaultPolicy(ClaimType.SUGGESTION, GravityLevel.MOYEN));
        }
        if (dayRepository.count() == 0) {
            for (int d = 1; d <= 7; d++) {
                dayRepository.save(BusinessDay.builder().dayOfWeek(d).working(d <= 5)
                        .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(17, 0)).build());
            }
        }
        if (reasonRepository.count() == 0) {
            for (String libelle : List.of("Pièce justificative manquante", "Dossier complexe", "Surcharge de travail",
                    "Agent absent", "En attente d'une réponse interne")) {
                reasonRepository.save(SlaBreachReason.builder().libelle(libelle).kind("RETARD").active(true).build());
            }
            for (String libelle : List.of("Client injoignable", "Le client a refusé de répondre", "Numéro erroné")) {
                reasonRepository.save(SlaBreachReason.builder().libelle(libelle).kind("NON_MESURE").active(true).build());
            }
        }
    }
}
