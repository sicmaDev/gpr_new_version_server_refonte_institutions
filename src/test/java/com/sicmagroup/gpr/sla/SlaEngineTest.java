package com.sicmagroup.gpr.sla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sicmagroup.gpr.domain.enumeration.ClaimEventType;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.GravityLevel;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;
import com.sicmagroup.gpr.sla.domain.ClaimSla;
import com.sicmagroup.gpr.sla.domain.SlaCycleKind;
import com.sicmagroup.gpr.sla.domain.SlaOwnerLevel;
import com.sicmagroup.gpr.sla.domain.SlaPhase;
import com.sicmagroup.gpr.sla.repository.BusinessDayRepository;
import com.sicmagroup.gpr.sla.repository.ClaimSlaRepository;
import com.sicmagroup.gpr.sla.repository.SlaBreachReasonRepository;
import com.sicmagroup.gpr.sla.repository.SlaPolicyRepository;
import com.sicmagroup.gpr.sla.service.BusinessTime;
import com.sicmagroup.gpr.sla.service.SlaConfig;
import com.sicmagroup.gpr.sla.service.SlaEngine;
import com.sicmagroup.gpr.sla.service.SlaHierarchy;
import com.sicmagroup.gpr.sla.service.SlaPolicyService;

/**
 * Moteur SLA : création des échéances et transitions du cycle de vie. Test unitaire : les dépôts sont simulés,
 * aucune base de données. Octobre 2026 : le lundi 5 octobre à 9 h sert de date de réception.
 */
class SlaEngineTest {

    private static final LocalDateTime MONDAY_9H = LocalDateTime.of(2026, 10, 5, 9, 0);

    private final ClaimSlaRepository repo = mock(ClaimSlaRepository.class);
    private final ClaimRepository claimRepo = mock(ClaimRepository.class);
    private final SlaConfig config = mock(SlaConfig.class);
    private final SlaHierarchy hierarchy = mock(SlaHierarchy.class);

    private SlaEngine engine;
    private ClaimSla stored;
    private final List<ClaimSla> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        SlaPolicyRepository policyRepo = mock(SlaPolicyRepository.class); // aucune politique en base : valeurs par défaut
        SlaPolicyService policies = new SlaPolicyService(policyRepo, mock(BusinessDayRepository.class),
                mock(SlaBreachReasonRepository.class));
        engine = new SlaEngine(repo, claimRepo, mock(SuggestionRepository.class), policies, new BusinessTime(), config,
                hierarchy);

        when(config.enabled()).thenReturn(true);
        when(config.regulatoryDays()).thenReturn(30);
        when(config.regulatoryWarningDays()).thenReturn(5);
        when(config.autoEscalation()).thenReturn(true);
        when(config.getBoolean(eq("sla.backfill_done"), anyBoolean())).thenReturn(true);
        when(hierarchy.levelOf(any(), any())).thenReturn(SlaOwnerLevel.AGENT);

        // le dépôt simulé « conserve » le dernier compteur enregistré
        when(repo.save(any(ClaimSla.class))).thenAnswer(inv -> {
            stored = inv.getArgument(0);
            saved.add(stored);
            return stored;
        });
        // comme en base : le compteur courant est le dernier cycle non annulé du bon type
        when(repo.findFirstByTargetTypeAndClaimIdAndPhaseNotOrderByCycleDesc(any(), any(), any()))
                .thenAnswer(inv -> Optional.ofNullable(stored)
                        .filter(s -> s.getPhase() != SlaPhase.CANCELLED && s.getTargetType() == inv.getArgument(0)));
        when(repo.findByTargetTypeAndClaimId(any(), any())).thenAnswer(inv -> stored == null ? List.of() : List.of(stored));
    }

    private Claim claim(ClaimType type, GravityLevel risk, int processingTimeDays, LocalDateTime received) {
        Claim c = new Claim();
        c.setId(1L);
        c.setCode("c1");
        c.setCodeClient("REC-1");
        c.setType(type);
        c.setStatus(ClaimStatus.SAVED);
        c.setReceiptDateTime(received);
        c.setCreatedAt(received);
        c.setObjet(Objet.builder().libelle("Objet test").risqueLevel(risk).processingTime(processingTimeDays).build());
        c.setServicePoint(ServicePoint.builder().id(1L).libelle("Agence A").build());
        when(claimRepo.findById(1L)).thenReturn(Optional.of(c));
        return c;
    }

    private static User agent(long id) {
        return User.builder().id(id).firstandlastname("Agent " + id).email("agent" + id + "@gpr.local").build();
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    void reclamationGraveRecueLundi9hAEcheanceJeudi9h() {
        claim(ClaimType.CLAIM, GravityLevel.GRAVE, 0, MONDAY_9H);

        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.SAVED);

        assertThat(stored.getResolutionDueAt()).isEqualTo(LocalDateTime.of(2026, 10, 8, 9, 0));
        assertThat(stored.getTakeoverDueAt()).isEqualTo(LocalDateTime.of(2026, 10, 5, 13, 0)); // 4 h ouvrées
        assertThat(stored.getRegulatoryDueAt()).isEqualTo(MONDAY_9H.plusDays(30)); // 30 jours calendaires
        assertThat(stored.getPhase()).isEqualTo(SlaPhase.OPEN);
        assertThat(stored.isBusinessTime()).isTrue();
    }

    @Test
    void leDelaiDeLObjetPrimeSurLaPolitique() {
        claim(ClaimType.CLAIM, GravityLevel.GRAVE, 2, MONDAY_9H);

        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.SAVED);

        assertThat(stored.getResolutionMinutes()).isEqualTo(2 * 540);
        assertThat(stored.getResolutionDueAt()).isEqualTo(LocalDateTime.of(2026, 10, 7, 9, 0));
    }

    @Test
    void lAffectationRenseigneLeResponsableEtLaPriseEnCharge() {
        Claim c = claim(ClaimType.CLAIM, GravityLevel.MOYEN, 0, MONDAY_9H);
        c.setTreatmentAffectedTo(agent(5));

        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.SAVED);
        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.AFFECTED);

        assertThat(stored.getOwnerUserId()).isEqualTo(5L);
        assertThat(stored.getOwnerLevel()).isEqualTo(SlaOwnerLevel.AGENT);
        assertThat(stored.getTakeoverAt()).isNotNull();
        assertThat(stored.getLastHumanActionAt()).isNotNull();
    }

    @Test
    void lApprobationDUneReclamationDemarreLeDelaiDeCloture() {
        claim(ClaimType.CLAIM, GravityLevel.GRAVE, 0, MONDAY_9H);

        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.SAVED);
        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.APPROVED);

        assertThat(stored.getResolvedAt()).isNotNull();
        assertThat(stored.getPhase()).isEqualTo(SlaPhase.OPEN);
        assertThat(stored.getClosureDueAt()).isNotNull();
        assertThat(stored.getClosureDueAt()).isAfter(stored.getResolvedAt());
        assertThat(stored.getOwnerUserId()).isNull(); // le suivi revient aux personnes qui mesurent
    }

    @Test
    void uneDenonciationTraiteeEstCloturee_SansDelaiDeCloture() {
        claim(ClaimType.DENUNCIACION, GravityLevel.GRAVE, 0, MONDAY_9H);

        engine.onEvent(1L, ClaimType.DENUNCIACION, ClaimEventType.SAVED);
        engine.onEvent(1L, ClaimType.DENUNCIACION, ClaimEventType.APPROVED);

        assertThat(stored.getPhase()).isEqualTo(SlaPhase.DONE);
        assertThat(stored.getClosedAt()).isNotNull();
        assertThat(stored.getClosureDueAt()).isNull();
        assertThat(stored.getClosureMinutes()).isZero();
    }

    @Test
    void uneMesureNonSatisfaisanteOuvreUnNouveauCycle() {
        claim(ClaimType.CLAIM, GravityLevel.MOYEN, 0, MONDAY_9H);

        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.SAVED);
        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.APPROVED);
        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.UNSATISFIED);

        ClaimSla next = saved.stream().filter(s -> s.getCycleKind() == SlaCycleKind.SATISFACTION).findFirst().orElseThrow();
        assertThat(next.getCycle()).isEqualTo(2);
        assertThat(next.getPhase()).isEqualTo(SlaPhase.OPEN);
        assertThat(next.getResolvedAt()).isNull();
        // l'ancien cycle est fermé
        assertThat(saved.stream().filter(s -> s.getCycle() == 1).reduce((a, b) -> b).orElseThrow().getPhase())
                .isEqualTo(SlaPhase.DONE);
    }

    @Test
    void classerUnePlainteSuspendLeCompteur() {
        claim(ClaimType.CLAIM, GravityLevel.MOYEN, 0, MONDAY_9H);

        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.SAVED);
        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.CLASSED);

        assertThat(stored.getPhase()).isEqualTo(SlaPhase.SUSPENDED);
        assertThat(stored.getNextCheckAt()).isNull();
    }

    @Test
    void uneSolutionDesapprouvee_RouvreLaPlainte() {
        claim(ClaimType.CLAIM, GravityLevel.MOYEN, 0, MONDAY_9H);

        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.SAVED);
        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.APPROVED);
        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.REJECTED);

        assertThat(stored.getResolvedAt()).isNull();
        assertThat(stored.getClosureDueAt()).isNull();
        assertThat(stored.getPhase()).isEqualTo(SlaPhase.OPEN);
    }

    // --- Plaintes arrivées en retard : pas de rafale d'alertes -------------------------------------------------

    @Test
    void plainteSynchroniseeEnRetard_PasDeRafaleDeRappels() {
        claim(ClaimType.CLAIM, GravityLevel.MOYEN, 0, LocalDateTime.now().minusDays(40));

        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.SAVED);

        // les rappels déjà passés sont considérés envoyés ; le dépassement reste à signaler une seule fois
        assertThat(stored.isReminder1Sent()).isTrue();
        assertThat(stored.isReminder2Sent()).isTrue();
        assertThat(stored.isBreachNotified()).isFalse();
        assertThat(stored.getNextCheckAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void repriseDesPlaintesOuvertes_AucuneAlerteRetroactive() {
        Claim c = claim(ClaimType.CLAIM, GravityLevel.MOYEN, 0, LocalDateTime.now().minusDays(40));

        ClaimSla s = engine.open(c, true);

        assertThat(s.isReminder1Sent()).isTrue();
        assertThat(s.isReminder2Sent()).isTrue();
        assertThat(s.isBreachNotified()).isTrue();
        assertThat(s.isRegulatoryBreachSent()).isTrue();
        assertThat(s.isRegulatoryBreached()).isTrue();
        assertThat(s.getNextEscalationAt()).isNull();
    }

    // --- Action humaine et escalade ---------------------------------------------------------------------------------

    @Test
    void uneActionHumaineSurUnDossierEnRetardRepousseLaRemontee() {
        Claim c = claim(ClaimType.CLAIM, GravityLevel.MOYEN, 0, LocalDateTime.now().minusDays(40));
        ClaimSla s = engine.open(c, true);
        LocalDateTime now = LocalDateTime.now();

        engine.humanAction(s, now);

        assertThat(s.getLastHumanActionAt()).isEqualTo(now);
        assertThat(s.getNextEscalationAt()).isNotNull().isAfter(now); // nouveau délai de grâce
    }

    @Test
    void uneActionHumaineSurUnDossierDansLesDelaisNePlanifieAucuneRemontee() {
        Claim c = claim(ClaimType.CLAIM, GravityLevel.MOYEN, 0, LocalDateTime.now());
        ClaimSla s = engine.open(c, false);

        engine.humanAction(s, LocalDateTime.now());

        assertThat(s.getNextEscalationAt()).isNull();
    }

    // --- Désactivation ----------------------------------------------------------------------------------------------

    @Test
    void sansActivationDuSlaLeMoteurNeFaitRien() {
        when(config.enabled()).thenReturn(false);

        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.SAVED);

        verifyNoInteractions(repo);
        verifyNoInteractions(claimRepo);
    }

    @Test
    void lesEvenementsEcritsParLeSlaNeChangentPasLeCompteur() {
        claim(ClaimType.CLAIM, GravityLevel.MOYEN, 0, MONDAY_9H);
        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.SAVED);
        int before = saved.size();

        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.SLA_BREACH);
        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.SLA_REMINDER);

        assertThat(saved).hasSize(before);
    }

    @Test
    void uneConversionAnnuleLAncienCompteur() {
        Claim c = claim(ClaimType.CLAIM, GravityLevel.MOYEN, 0, MONDAY_9H);
        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.SAVED);
        ClaimSla old = stored;

        // la réclamation devient une dénonciation
        c.setType(ClaimType.DENUNCIACION);
        engine.onEvent(1L, ClaimType.CLAIM, ClaimEventType.CONVERTED);

        assertThat(old.getPhase()).isEqualTo(SlaPhase.CANCELLED);
    }
}
