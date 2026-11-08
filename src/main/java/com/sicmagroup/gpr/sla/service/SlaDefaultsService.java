package com.sicmagroup.gpr.sla.service;

import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sicmagroup.gpr.sla.domain.BusinessDay;
import com.sicmagroup.gpr.sla.domain.SlaBreachReason;
import com.sicmagroup.gpr.sla.repository.BusinessDayRepository;
import com.sicmagroup.gpr.sla.repository.SlaBreachReasonRepository;

import lombok.RequiredArgsConstructor;

/** Initialisation du calendrier et des motifs du suivi des délais. */
@Service
@RequiredArgsConstructor
public class SlaDefaultsService {

    private final BusinessDayRepository dayRepository;
    private final SlaBreachReasonRepository reasonRepository;

    /** Crée le calendrier et les motifs par défaut s'ils n'existent pas encore. */
    @Transactional
    public void seedDefaults() {
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
