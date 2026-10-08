package com.sicmagroup.gpr.sla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sicmagroup.gpr.domain.enumeration.ClaimEventType;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;
import com.sicmagroup.gpr.service.claimEvent.ClaimEventService;
import com.sicmagroup.gpr.sla.domain.ClaimSla;
import com.sicmagroup.gpr.sla.domain.ContactAttempt;
import com.sicmagroup.gpr.sla.domain.SlaBreachReason;
import com.sicmagroup.gpr.sla.domain.SlaOwnerLevel;
import com.sicmagroup.gpr.sla.domain.SlaPhase;
import com.sicmagroup.gpr.sla.repository.BusinessDayRepository;
import com.sicmagroup.gpr.sla.repository.ClaimSlaRepository;
import com.sicmagroup.gpr.sla.repository.ContactAttemptRepository;
import com.sicmagroup.gpr.sla.repository.SlaBreachReasonRepository;
import com.sicmagroup.gpr.sla.repository.SlaEventRepository;
import com.sicmagroup.gpr.sla.service.SlaNotifier;
import com.sicmagroup.gpr.sla.repository.SlaPolicyRepository;
import com.sicmagroup.gpr.sla.service.BusinessTime;
import com.sicmagroup.gpr.sla.service.SlaActionService;
import com.sicmagroup.gpr.sla.service.SlaActionService.SlaActionException;
import com.sicmagroup.gpr.sla.service.SlaConfig;
import com.sicmagroup.gpr.sla.service.SlaEngine;
import com.sicmagroup.gpr.sla.service.SlaHierarchy;
import com.sicmagroup.gpr.sla.service.SlaPerimeter;
import com.sicmagroup.gpr.sla.service.SlaPolicyService;

/**
 * Actions humaines : attente du client, reprise, justification, tentatives de contact, client injoignable.
 * Test unitaire : aucune base de données. Les compteurs sont en minutes calendaires pour des durées exactes.
 */
class SlaActionServiceTest {

    private final ClaimSlaRepository repo = mock(ClaimSlaRepository.class);
    private final ClaimRepository claimRepo = mock(ClaimRepository.class);
    private final ContactAttemptRepository attemptRepo = mock(ContactAttemptRepository.class);
    private final SlaBreachReasonRepository reasonRepo = mock(SlaBreachReasonRepository.class);
    private final SlaPerimeter perimeter = mock(SlaPerimeter.class);
    private final SlaConfig config = mock(SlaConfig.class);
    private final ClaimEventService claimEvents = mock(ClaimEventService.class);
    private final SlaNotifier notifier = mock(SlaNotifier.class);
    private final SlaEventRepository eventRepo = mock(SlaEventRepository.class);

    private SlaActionService service;
    private Claim claim;
    private ClaimSla sla;
    private final User agent = User.builder().id(5L).firstandlastname("Agent").email("a@gpr.local").build();
    private final User pilote = User.builder().id(9L).firstandlastname("Pilote").email("p@gpr.local")
            .additionalrole(Role.PILOTE).build();

    @BeforeEach
    void setUp() {
        BusinessTime bt = new BusinessTime();
        SlaPolicyService policies = new SlaPolicyService(mock(SlaPolicyRepository.class),
                mock(BusinessDayRepository.class), mock(SlaBreachReasonRepository.class));
        SlaEngine engine = new SlaEngine(repo, claimRepo, mock(SuggestionRepository.class), policies, bt, config,
                mock(SlaHierarchy.class));
        service = new SlaActionService(repo, claimRepo, attemptRepo, reasonRepo, perimeter, engine, config, bt,
                claimEvents, notifier, eventRepo);

        when(config.enabled()).thenReturn(true);
        when(config.autoEscalation()).thenReturn(true);
        when(config.regulatoryWarningDays()).thenReturn(5);
        when(config.unreachableAttempts()).thenReturn(3);
        when(config.unreachableDays()).thenReturn(7);
        when(config.getBoolean(eq("sla.backfill_done"), anyBoolean())).thenReturn(true);
        when(perimeter.scopeOf(any())).thenReturn(new SlaPerimeter.Scope(true, java.util.List.of(-1L), 5L, 5L));
        when(perimeter.canSee(any(), any())).thenReturn(true);
        when(repo.save(any(ClaimSla.class))).thenAnswer(inv -> inv.getArgument(0));

        claim = new Claim();
        claim.setId(1L);
        claim.setCode("code-1");
        claim.setCodeClient("REC-1");
        claim.setType(ClaimType.CLAIM);
        when(claimRepo.findById(1L)).thenReturn(Optional.of(claim));
    }

    private ClaimSla counter(long receivedMinutesAgo) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime received = now.minusMinutes(receivedMinutesAgo);
        sla = ClaimSla.builder().id(10L).targetType(ClaimType.CLAIM).claimId(1L).cycle(1).phase(SlaPhase.OPEN)
                .receivedAt(received).registeredAt(received).resolutionMinutes(1000)
                .resolutionDueAt(received.plusMinutes(1000)).regulatoryDueAt(received.plusDays(30))
                .reminder1Pct(50).reminder2Pct(75).graceMinutes(540).businessTime(false)
                .ownerUserId(5L).ownerLevel(SlaOwnerLevel.AGENT).closureMinutes(500).build();
        when(repo.findFirstByTargetTypeAndClaimIdAndPhaseNotOrderByCycleDesc(any(), any(), any()))
                .thenReturn(Optional.of(sla));
        return sla;
    }

    private SlaBreachReason reason(long id, String kind, boolean active) {
        SlaBreachReason r = SlaBreachReason.builder().id(id).libelle("Motif " + id).kind(kind).active(active).build();
        when(reasonRepo.findById(id)).thenReturn(Optional.of(r));
        return r;
    }

    // --- Attente du client -------------------------------------------------------------------------------------

    @Test
    void mettreEnAttenteDuClientSuspendLeDelaiInterne() {
        ClaimSla s = counter(100);
        s.setNextEscalationAt(LocalDateTime.now().plusHours(1));

        service.waitCustomer(agent, ClaimType.CLAIM, 1L, "Pièce manquante");

        assertThat(s.getPhase()).isEqualTo(SlaPhase.PAUSED);
        assertThat(s.getPausedAt()).isNotNull();
        assertThat(s.getNextEscalationAt()).isNull(); // l'escalade est arrêtée
        verify(claimEvents).log(eq(1L), eq("REC-1"), eq(ClaimType.CLAIM), eq(ClaimEventType.WAITING_CUSTOMER),
                eq("Agent"), eq("a@gpr.local"), eq("Pièce manquante"));
    }

    @Test
    void laReprisePousseLEcheanceInterneDeLaDureeDeLaPauseMaisPasLEcheanceReglementaire() {
        ClaimSla s = counter(500);
        LocalDateTime dueBefore = s.getResolutionDueAt();
        LocalDateTime regBefore = s.getRegulatoryDueAt();
        s.setPhase(SlaPhase.PAUSED);
        s.setPausedAt(LocalDateTime.now().minusMinutes(2880)); // 2 jours d'attente

        service.resume(agent, ClaimType.CLAIM, 1L);

        assertThat(s.getPhase()).isEqualTo(SlaPhase.OPEN);
        assertThat(s.getPausedAt()).isNull();
        assertThat(s.getPausedMinutes()).isBetween(2879L, 2881L);
        assertThat(s.getResolutionDueAt()).isEqualTo(dueBefore.plusMinutes(s.getPausedMinutes()));
        assertThat(s.getRegulatoryDueAt()).isEqualTo(regBefore); // l'échéance réglementaire ne bouge pas
        verify(claimEvents).log(eq(1L), any(), any(), eq(ClaimEventType.SLA_RESUMED), any(), any(), any());
    }

    @Test
    void onNePeutPasMettreEnAttenteUneReclamationDejaTraitee() {
        ClaimSla s = counter(100);
        s.setResolvedAt(LocalDateTime.now().minusMinutes(10));

        assertThatThrownBy(() -> service.waitCustomer(agent, ClaimType.CLAIM, 1L, "x"))
                .isInstanceOf(SlaActionException.class);
        assertThat(s.getPhase()).isEqualTo(SlaPhase.OPEN);
    }

    @Test
    void onNeReprendPasUnePlainteQuiNEstPasEnAttente() {
        counter(100);

        assertThatThrownBy(() -> service.resume(agent, ClaimType.CLAIM, 1L)).isInstanceOf(SlaActionException.class);
    }

    @Test
    void sansSlaActiveLesActionsSontRefusees() {
        when(config.enabled()).thenReturn(false);
        counter(100);

        assertThatThrownBy(() -> service.waitCustomer(agent, ClaimType.CLAIM, 1L, "x"))
                .isInstanceOf(SlaActionException.class).hasMessageContaining("pas activé");
    }

    @Test
    void uneActionSurUnePlainteHorsPerimetreEstRefusee() {
        when(perimeter.canSee(any(), any())).thenReturn(false);
        counter(100);

        assertThatThrownBy(() -> service.waitCustomer(agent, ClaimType.CLAIM, 1L, "x"))
                .isInstanceOf(SlaActionException.class).hasMessageContaining("introuvable");
    }

    // --- Justification d'un retard ---------------------------------------------------------------------------------

    @Test
    void justifierUnRetardEnregistreLeMotifEtRelanceLeDelaiDeGrace() {
        ClaimSla s = counter(1200); // en retard
        SlaBreachReason r = reason(3L, "RETARD", true);

        service.justify(agent, ClaimType.CLAIM, 1L, 3L, "Dossier complexe, en attente de la direction");

        assertThat(s.getBreachReasonId()).isEqualTo(3L);
        assertThat(s.getBreachComment()).contains("complexe");
        assertThat(s.getJustifiedById()).isEqualTo(5L);
        assertThat(s.getNextEscalationAt()).isNotNull(); // une action humaine : nouveau délai de grâce
        verify(claimEvents).log(eq(1L), any(), any(), eq(ClaimEventType.SLA_JUSTIFIED), any(), any(), eq(r.getLibelle()));
    }

    @Test
    void onNeJustifiePasUnePlainteDansLesDelais() {
        counter(100);
        reason(3L, "RETARD", true);

        assertThatThrownBy(() -> service.justify(agent, ClaimType.CLAIM, 1L, 3L, "x"))
                .isInstanceOf(SlaActionException.class).hasMessageContaining("pas en retard");
    }

    @Test
    void unMotifInactifOuDUnAutreTypeEstRefuse() {
        counter(1200);
        reason(3L, "RETARD", false);
        reason(4L, "NON_MESURE", true);

        assertThatThrownBy(() -> service.justify(agent, ClaimType.CLAIM, 1L, 3L, "x"))
                .isInstanceOf(SlaActionException.class);
        assertThatThrownBy(() -> service.justify(agent, ClaimType.CLAIM, 1L, 4L, "x"))
                .isInstanceOf(SlaActionException.class);
    }

    // --- Tentatives de contact et client injoignable ------------------------------------------------------------------

    private ClaimSla treatedCounter() {
        ClaimSla s = counter(2000);
        s.setResolvedAt(LocalDateTime.now().minusDays(2));
        s.setClosureDueAt(LocalDateTime.now().plusDays(1));
        return s;
    }

    @Test
    void uneTentativeDeContactSEnregistre() {
        treatedCounter();
        when(attemptRepo.save(any(ContactAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactAttempt a = service.addContactAttempt(agent, 1L, "APPEL", false, "Pas de réponse");

        assertThat(a.getChannel()).isEqualTo("APPEL");
        assertThat(a.isReached()).isFalse();
        verify(claimEvents).log(eq(1L), any(), any(), eq(ClaimEventType.CONTACT_ATTEMPT), any(), any(), any());
    }

    @Test
    void lesTentativesNeConcernentQueLesReclamationsTraitees() {
        counter(100); // pas encore traitée

        assertThatThrownBy(() -> service.addContactAttempt(agent, 1L, "APPEL", false, null))
                .isInstanceOf(SlaActionException.class);
    }

    @Test
    void uncanalInconnuEstRefuse() {
        treatedCounter();

        assertThatThrownBy(() -> service.addContactAttempt(agent, 1L, "PIGEON", false, null))
                .isInstanceOf(SlaActionException.class);
    }

    @Test
    void apresTroisTentativesSansSuccesLePiloteClotureCommeClientInjoignable() {
        ClaimSla s = treatedCounter();
        when(attemptRepo.countByClaimIdAndReachedFalseAndCreatedAtAfter(eq(1L), any())).thenReturn(3L);

        service.closeUnreachable(pilote, 1L, null);

        assertThat(s.getPhase()).isEqualTo(SlaPhase.DONE);
        assertThat(s.isClosedUnreachable()).isTrue();
        assertThat(s.getClosedAt()).isNotNull();
        verify(claimEvents).log(eq(1L), any(), any(), eq(ClaimEventType.CLOSED_UNREACHABLE), any(), any(), any());
    }

    @Test
    void avecMoinsDeTentativesQueLeSeuilLaClotureEstRefusee() {
        ClaimSla s = treatedCounter();
        when(attemptRepo.countByClaimIdAndReachedFalseAndCreatedAtAfter(eq(1L), any())).thenReturn(2L);

        assertThatThrownBy(() -> service.closeUnreachable(pilote, 1L, null)).isInstanceOf(SlaActionException.class)
                .hasMessageContaining("3 tentative");
        assertThat(s.getPhase()).isEqualTo(SlaPhase.OPEN);
    }

    @Test
    void seulLePiloteCloture() {
        treatedCounter();
        when(attemptRepo.countByClaimIdAndReachedFalseAndCreatedAtAfter(eq(1L), any())).thenReturn(5L);

        assertThatThrownBy(() -> service.closeUnreachable(agent, 1L, null)).isInstanceOf(SlaActionException.class)
                .hasMessageContaining("Pilote");
        verify(claimEvents, never()).log(any(), any(), any(), eq(ClaimEventType.CLOSED_UNREACHABLE), any(), any(), any());
    }

    // --- Message d'attente au client ------------------------------------------------------------------------------

    @Test
    void lMessageDAttenteEstRefuseQuandLeReglageEstAucun() {
        when(config.customerWaitingMessage()).thenReturn("NONE");
        counter(1200);

        assertThatThrownBy(() -> service.sendWaitingMessage(agent, 1L)).isInstanceOf(SlaActionException.class)
                .hasMessageContaining("pas activé");
        verify(notifier, never()).clientWaitingMessage(any());
    }

    @Test
    void lMessageDAttenteEstEnvoyeUneFoisPourUneReclamationEnRetard() {
        when(config.customerWaitingMessage()).thenReturn("MANUAL");
        when(notifier.clientWaitingMessage(any())).thenReturn(true);
        counter(1200);

        service.sendWaitingMessage(agent, 1L);

        verify(notifier).clientWaitingMessage(claim);
        verify(eventRepo).save(any(com.sicmagroup.gpr.sla.domain.SlaEvent.class));
        verify(claimEvents).log(eq(1L), any(), any(), eq(ClaimEventType.MAIL_SENT_CLIENT), any(), any(), any());
    }

    @Test
    void lMessageDAttenteNEstPasEnvoyeSiLaReclamationEstDansLesDelais() {
        when(config.customerWaitingMessage()).thenReturn("MANUAL");
        counter(100);

        assertThatThrownBy(() -> service.sendWaitingMessage(agent, 1L)).isInstanceOf(SlaActionException.class)
                .hasMessageContaining("pas en retard");
        verify(notifier, never()).clientWaitingMessage(any());
    }

    @Test
    void lMessageDAttenteNEstJamaisEnvoyeDeuxFoisPourLeMemeRetard() {
        when(config.customerWaitingMessage()).thenReturn("MANUAL");
        when(eventRepo.existsByDedupKey(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
        counter(1200);

        assertThatThrownBy(() -> service.sendWaitingMessage(agent, 1L)).isInstanceOf(SlaActionException.class)
                .hasMessageContaining("déjà été envoyé");
        verify(notifier, never()).clientWaitingMessage(any());
    }

    @Test
    void sansCoordonneesDuClientLeMessageDAttenteEstRefuse() {
        when(config.customerWaitingMessage()).thenReturn("MANUAL");
        when(notifier.clientWaitingMessage(any())).thenReturn(false);
        counter(1200);

        assertThatThrownBy(() -> service.sendWaitingMessage(agent, 1L)).isInstanceOf(SlaActionException.class)
                .hasMessageContaining("Aucun e-mail ni téléphone");
        verify(eventRepo, never()).save(any(com.sicmagroup.gpr.sla.domain.SlaEvent.class));
    }

    @Test
    void laClotureInjoignableNeCompteNiCommeSatisfaitNiCommeInsatisfait() {
        // la plainte n'est pas passée par une mesure : le statut de la plainte reste inchangé (TREAT)
        ClaimSla s = treatedCounter();
        claim.setStatus(com.sicmagroup.gpr.domain.enumeration.ClaimStatus.TREAT);
        when(attemptRepo.countByClaimIdAndReachedFalseAndCreatedAtAfter(eq(1L), any())).thenReturn(3L);

        service.closeUnreachable(pilote, 1L, null);

        assertThat(claim.getStatus()).isEqualTo(com.sicmagroup.gpr.domain.enumeration.ClaimStatus.TREAT);
        assertThat(s.isClosedUnreachable()).isTrue();
    }
}
