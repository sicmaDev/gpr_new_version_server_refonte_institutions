package com.sicmagroup.gpr.sla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.sla.domain.ClaimSla;
import com.sicmagroup.gpr.sla.domain.SlaOwnerLevel;
import com.sicmagroup.gpr.sla.domain.SlaPhase;
import com.sicmagroup.gpr.sla.domain.SlaState;
import com.sicmagroup.gpr.sla.dto.SlaInfo;
import com.sicmagroup.gpr.sla.repository.ClaimSlaRepository;
import com.sicmagroup.gpr.sla.service.BusinessTime;
import com.sicmagroup.gpr.sla.service.SlaConfig;
import com.sicmagroup.gpr.sla.service.SlaInfoService;

/**
 * États affichés (en cours, à risque, dépassé, suspendu...) et bloc « sla » : un seul calcul, côté serveur.
 * Test unitaire : minutes calendaires, aucune base de données.
 */
class SlaInfoServiceTest {

    private final SlaInfoService service = new SlaInfoService(mock(ClaimSlaRepository.class), mock(UserRepository.class),
            new BusinessTime(), mock(SlaConfig.class));

    /** Compteur ouvert : 1000 minutes de délai, reçu il y a receivedMinutesAgo minutes. */
    private static ClaimSla open(long receivedMinutesAgo) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime received = now.minusMinutes(receivedMinutesAgo);
        return ClaimSla.builder().id(1L).targetType(ClaimType.CLAIM).claimId(1L).cycle(1).phase(SlaPhase.OPEN)
                .receivedAt(received).resolutionMinutes(1000).resolutionDueAt(received.plusMinutes(1000))
                .regulatoryDueAt(received.plusDays(30)).reminder1Pct(50).reminder2Pct(75).graceMinutes(540)
                .businessTime(false).ownerUserId(5L).ownerLevel(SlaOwnerLevel.AGENT).build();
    }

    @Test
    void dansLesDelaisEnCours() {
        assertThat(service.stateOf(open(200), LocalDateTime.now())).isEqualTo(SlaState.EN_COURS);
    }

    @Test
    void auDessusDeSoixanteQuinzePourCentARisque() {
        assertThat(service.stateOf(open(800), LocalDateTime.now())).isEqualTo(SlaState.A_RISQUE);
    }

    @Test
    void apresLEcheanceDepasse() {
        assertThat(service.stateOf(open(1200), LocalDateTime.now())).isEqualTo(SlaState.DEPASSE);
    }

    @Test
    void resolueDansLesDelaisRespectee() {
        ClaimSla s = open(1200);
        s.setPhase(SlaPhase.DONE);
        s.setResolvedAt(s.getReceivedAt().plusMinutes(500));
        s.setClosedAt(LocalDateTime.now());

        assertThat(service.stateOf(s, LocalDateTime.now())).isEqualTo(SlaState.RESPECTE);
    }

    @Test
    void resolueEnRetardResteDepassee() {
        ClaimSla s = open(2000);
        s.setPhase(SlaPhase.DONE);
        s.setResolvedAt(s.getReceivedAt().plusMinutes(1500));
        s.setClosedAt(LocalDateTime.now());

        assertThat(service.stateOf(s, LocalDateTime.now())).isEqualTo(SlaState.DEPASSE);
    }

    @Test
    void enAttenteDuClientOuClasseeSuspendue() {
        ClaimSla paused = open(1200);
        paused.setPhase(SlaPhase.PAUSED);
        paused.setPausedAt(LocalDateTime.now().minusMinutes(60));
        ClaimSla suspended = open(100);
        suspended.setPhase(SlaPhase.SUSPENDED);

        assertThat(service.stateOf(paused, LocalDateTime.now())).isEqualTo(SlaState.SUSPENDU);
        assertThat(service.stateOf(suspended, LocalDateTime.now())).isEqualTo(SlaState.SUSPENDU);
    }

    @Test
    void horsDelaiReglementaireEstDefinitif() {
        ClaimSla s = open(100);
        s.setRegulatoryBreached(true);

        assertThat(service.stateOf(s, LocalDateTime.now())).isEqualTo(SlaState.HORS_DELAI);
    }

    @Test
    void unCompteurAnnuleEstAnnule() {
        ClaimSla s = open(100);
        s.setPhase(SlaPhase.CANCELLED);

        assertThat(service.stateOf(s, LocalDateTime.now())).isEqualTo(SlaState.ANNULE);
    }

    @Test
    void leBlocSlaDonneLEcheanceLesMinutesRestantesEtLeResponsable() {
        ClaimSla s = open(250); // 25 % consommé, 750 minutes restantes

        SlaInfo info = service.toInfo(s, Map.of(5L, "Agent Cinq"));

        assertThat(info.getState()).isEqualTo(SlaState.EN_COURS);
        assertThat(info.getDueAt()).isEqualTo(s.getResolutionDueAt());
        assertThat(info.getRemainingMinutes()).isBetween(748L, 750L);
        assertThat(info.getConsumedPct()).isBetween(24, 26);
        assertThat(info.getOwnerName()).isEqualTo("Agent Cinq");
        assertThat(info.getOwnerLevel()).isEqualTo(SlaOwnerLevel.AGENT);
        assertThat(info.isPaused()).isFalse();
        assertThat(info.getDelay()).isEqualTo("RESOLUTION");
    }

    @Test
    void lesMinutesRestantesSontNegativesApresLEcheance() {
        SlaInfo info = service.toInfo(open(1100), Map.of());

        assertThat(info.getRemainingMinutes()).isNegative();
        assertThat(info.getState()).isEqualTo(SlaState.DEPASSE);
    }

    @Test
    void ApresLApprobationLeDelaiSuiviEstCeluiDeCloture() {
        ClaimSla s = open(1100);
        s.setResolvedAt(LocalDateTime.now().minusMinutes(100));
        s.setClosureMinutes(500);
        s.setClosureDueAt(s.getResolvedAt().plusMinutes(500));

        SlaInfo info = service.toInfo(s, Map.of());

        assertThat(info.getDelay()).isEqualTo("CLOTURE");
        assertThat(info.getDueAt()).isEqualTo(s.getClosureDueAt());
        assertThat(info.getState()).isEqualTo(SlaState.EN_COURS);
    }

    @Test
    void lePauseGeleLesMinutesRestantes() {
        ClaimSla s = open(250);
        s.setPhase(SlaPhase.PAUSED);
        s.setPausedAt(LocalDateTime.now().minusMinutes(100));

        SlaInfo info = service.toInfo(s, Map.of());

        assertThat(info.isPaused()).isTrue();
        // le temps ne s'écoule plus depuis la mise en pause : 250 - 100 = 150 minutes écoulées au moment de la pause
        assertThat(info.getRemainingMinutes()).isBetween(848L, 850L);
    }

    @Test
    void leBlocSlaNeContientAucuneIdentiteDeClient() {
        // le bloc ne porte que des états, des dates, un responsable interne : aucun champ nom, téléphone, e-mail
        for (java.lang.reflect.Field f : SlaInfo.class.getDeclaredFields()) {
            assertThat(f.getName().toLowerCase()).doesNotContain("client", "tel", "email", "address", "phone", "nom");
        }
    }
}
