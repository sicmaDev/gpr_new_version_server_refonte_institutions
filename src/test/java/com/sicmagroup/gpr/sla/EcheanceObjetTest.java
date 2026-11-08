package com.sicmagroup.gpr.sla;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.sla.service.BusinessTime;
import com.sicmagroup.gpr.sla.service.EcheanceObjet;

/** Échéance d'une plainte : le délai de l'objet, en jours ouvrés (lundi à vendredi, 8 h - 17 h). */
class EcheanceObjetTest {

    private final EcheanceObjet sansFerie = new EcheanceObjet(new BusinessTime());

    private static LocalDateTime date(int mois, int jour, int heure) {
        return LocalDateTime.of(2026, mois, jour, heure, 0);
    }

    @Test
    void lesJoursOuvresSautentLeWeekEnd() {
        // vendredi 6 novembre 2026, 10 h + 2 jours ouvrés = mardi 10 novembre, 10 h
        assertThat(sansFerie.echeance(date(11, 6, 10), 2)).isEqualTo(date(11, 10, 10));
    }

    @Test
    void lesJoursOuvresSautentUnJourFerie() {
        Map<Integer, LocalTime[]> semaine = new HashMap<>();
        for (int d = 1; d <= 5; d++) {
            semaine.put(d, new LocalTime[] { LocalTime.of(8, 0), LocalTime.of(17, 0) });
        }
        EcheanceObjet avecFerie = new EcheanceObjet(new BusinessTime(semaine, Set.of(LocalDate.of(2026, 11, 9))));
        // le lundi 9 novembre est férié : l'échéance passe au mercredi 11 novembre
        assertThat(avecFerie.echeance(date(11, 6, 10), 2)).isEqualTo(date(11, 11, 10));
    }

    @Test
    void lEcheanceDUnePlaintePartDeSaDateDeReception() {
        Objet objet = new Objet();
        objet.setProcessingTime(1);
        Claim claim = new Claim();
        claim.setObjet(objet);
        claim.setReceiptDateTime(date(11, 6, 10));
        claim.setCreatedAt(date(11, 2, 10));
        claim.setAffectedAt(date(11, 3, 10));
        assertThat(sansFerie.echeance(claim)).isEqualTo(date(11, 9, 10));
    }

    @Test
    void sansDelaiLEcheanceEstLaDateDeDepart() {
        assertThat(sansFerie.echeance(date(11, 6, 10), 0)).isEqualTo(date(11, 6, 10));
    }
}
