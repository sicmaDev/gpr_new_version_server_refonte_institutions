package com.sicmagroup.gpr.sla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sicmagroup.gpr.domain.enumeration.ClaimEventType;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.GravityLevel;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.HistoriqueTransmission;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.HistoriqueTransmissionRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.service.claimEvent.ClaimEventService;
import com.sicmagroup.gpr.sla.domain.ClaimSla;
import com.sicmagroup.gpr.sla.domain.SlaEvent;
import com.sicmagroup.gpr.sla.domain.SlaOwnerLevel;
import com.sicmagroup.gpr.sla.domain.SlaPhase;
import com.sicmagroup.gpr.sla.repository.BusinessDayRepository;
import com.sicmagroup.gpr.sla.repository.ClaimSlaRepository;
import com.sicmagroup.gpr.sla.repository.SlaBreachReasonRepository;
import com.sicmagroup.gpr.sla.repository.SlaEventRepository;
import com.sicmagroup.gpr.sla.repository.SlaPolicyRepository;
import com.sicmagroup.gpr.sla.service.BusinessTime;
import com.sicmagroup.gpr.sla.service.SlaConfig;
import com.sicmagroup.gpr.sla.service.SlaEngine;
import com.sicmagroup.gpr.sla.service.SlaHierarchy;
import com.sicmagroup.gpr.sla.service.SlaMessages;
import com.sicmagroup.gpr.sla.service.SlaNotifier;
import com.sicmagroup.gpr.sla.service.SlaNotifier.Recipients;
import com.sicmagroup.gpr.sla.service.SlaPolicyService;
import com.sicmagroup.gpr.sla.service.SlaScanner;

/**
 * Tâche de contrôle du SLA : rappels, dépassement, remontée automatique, échéance réglementaire, idempotence.
 * Test unitaire : tout est simulé, aucune base de données, aucun e-mail envoyé.
 */
class SlaScannerTest {

    private final ClaimSlaRepository repo = mock(ClaimSlaRepository.class);
    private final SlaEventRepository eventRepo = mock(SlaEventRepository.class);
    private final ClaimRepository claimRepo = mock(ClaimRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final HistoriqueTransmissionRepository transmissionRepo = mock(HistoriqueTransmissionRepository.class);
    private final ClaimEventService claimEvents = mock(ClaimEventService.class);
    private final SlaConfig config = mock(SlaConfig.class);
    private final SlaHierarchy hierarchy = mock(SlaHierarchy.class);
    private final SlaNotifier notifier = mock(SlaNotifier.class);

    private final Set<String> dedupKeys = new HashSet<>();
    private SlaScanner scanner;
    private SlaEngine engine;
    private Claim claim;
    private final User agent = user(5, "Agent Cinq");
    private final User ra = user(7, "RA Sept");
    private final User direction = user(8, "Direction Huit");

    private static User user(long id, String name) {
        return User.builder().id(id).firstandlastname(name).email("u" + id + "@gpr.local").build();
    }

    @BeforeEach
    void setUp() {
        BusinessTime bt = new BusinessTime();
        SlaPolicyService policies = new SlaPolicyService(mock(SlaPolicyRepository.class),
                mock(BusinessDayRepository.class), mock(SlaBreachReasonRepository.class));
        engine = new SlaEngine(repo, claimRepo, mock(SuggestionRepository.class), policies, bt, config, hierarchy);
        scanner = new SlaScanner(repo, eventRepo, claimRepo, mock(SuggestionRepository.class), userRepo,
                transmissionRepo, claimEvents, engine, config, bt, hierarchy, notifier);

        when(config.enabled()).thenReturn(true);
        when(config.autoEscalation()).thenReturn(true);
        when(config.dailyDigest()).thenReturn(false);
        when(config.regulatoryDays()).thenReturn(30);
        when(config.regulatoryWarningDays()).thenReturn(5);
        when(config.getBoolean(eq("sla.backfill_done"), anyBoolean())).thenReturn(true);

        // idempotence : la clé de déduplication est retenue comme en base (colonne unique)
        when(eventRepo.existsByDedupKey(any())).thenAnswer(inv -> dedupKeys.contains(inv.getArgument(0)));
        when(eventRepo.save(any(SlaEvent.class))).thenAnswer(inv -> {
            SlaEvent e = inv.getArgument(0);
            dedupKeys.add(e.getDedupKey());
            return e;
        });
        when(repo.save(any(ClaimSla.class))).thenAnswer(inv -> inv.getArgument(0));

        claim = new Claim();
        claim.setId(1L);
        claim.setCode("code-1");
        claim.setCodeClient("REC-1");
        claim.setType(ClaimType.CLAIM);
        claim.setStatus(ClaimStatus.AFFECTED);
        claim.setObjet(Objet.builder().libelle("Objet").risqueLevel(GravityLevel.MOYEN).build());
        claim.setServicePoint(ServicePoint.builder().id(1L).libelle("Agence A").build());
        claim.setTreatmentAffectedTo(agent);
        when(claimRepo.findById(1L)).thenReturn(Optional.of(claim));
        when(userRepo.findById(5L)).thenReturn(Optional.of(agent));
        when(notifier.ownerAndRa(any(), any())).thenReturn(new Recipients(List.of(agent, ra), List.of()));
        when(notifier.regulatory(any(), any())).thenReturn(new Recipients(List.of(agent, ra), List.of()));
    }

    /** Compteur en minutes calendaires : 1000 minutes de délai, reçu il y a receivedMinutesAgo minutes. */
    private ClaimSla counter(long receivedMinutesAgo) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime received = now.minusMinutes(receivedMinutesAgo);
        ClaimSla s = ClaimSla.builder()
                .id(10L).targetType(ClaimType.CLAIM).claimId(1L).cycle(1)
                .phase(SlaPhase.OPEN)
                .receivedAt(received).registeredAt(received).takeoverAt(received)
                .resolutionMinutes(1000).resolutionDueAt(received.plusMinutes(1000))
                .regulatoryDueAt(received.plusDays(30))
                .reminder1Pct(50).reminder2Pct(75).graceMinutes(540).businessTime(false)
                .ownerUserId(5L).ownerLevel(SlaOwnerLevel.AGENT)
                .nextCheckAt(now.minusMinutes(1))
                .build();
        when(repo.findById(10L)).thenReturn(Optional.of(s));
        return s;
    }

    // --- Rappels ------------------------------------------------------------------------------------------------

    @Test
    void rappelA50PourcentEnvoyeUneSeuleFoisAvecLeRaEnCopie() {
        ClaimSla s = counter(600); // 60 % consommé

        scanner.processOne(10L);

        verify(claimEvents).log(eq(1L), eq("REC-1"), eq(ClaimType.CLAIM), eq(ClaimEventType.SLA_REMINDER),
                eq("Système GPR"), isNull(), contains("50"));
        verify(notifier).mail(any(), contains("Rappel"), any());
        assertThat(s.isReminder1Sent()).isTrue();
        assertThat(s.isReminder2Sent()).isFalse();
        assertThat(s.getNextCheckAt()).isAfter(LocalDateTime.now()); // prochain contrôle : le seuil de 75 %
    }

    @Test
    void lesRappelsVontDansLeRecapitulatifQuotidienQuandIlEstActive() {
        when(config.dailyDigest()).thenReturn(true);
        counter(600);

        scanner.processOne(10L);

        ArgumentCaptor<SlaEvent> events = ArgumentCaptor.forClass(SlaEvent.class);
        verify(eventRepo, org.mockito.Mockito.atLeastOnce()).save(events.capture());
        List<SlaEvent> digest = events.getAllValues().stream().filter(e -> "DIGEST_ITEM".equals(e.getEventType())).toList();
        assertThat(digest).hasSize(2); // l'agent et le RA (copie de tous les rappels de ses agents)
        assertThat(digest).allMatch(SlaEvent::isDigestPending);
        verify(notifier, never()).mail(any(), any(), any()); // rien n'est envoyé tout de suite
    }

    // --- Dépassement ----------------------------------------------------------------------------------------------

    @Test
    void depassementAlerteLAgentEtLeRaEtPlanifieLaRemontee() {
        ClaimSla s = counter(1200); // 200 minutes de retard

        scanner.processOne(10L);

        verify(claimEvents).log(eq(1L), eq("REC-1"), eq(ClaimType.CLAIM), eq(ClaimEventType.SLA_BREACH),
                eq("Système GPR"), isNull(), any());
        verify(claimEvents, never()).log(any(), any(), any(), eq(ClaimEventType.SLA_REMINDER), any(), any(), any());
        verify(notifier).mail(any(), contains("dépassé"), any());
        verify(notifier).sms(any(), any());
        assertThat(s.isBreachNotified()).isTrue();
        // le dossier reste chez l'agent ; la remontée est planifiée après le délai de grâce
        assertThat(s.getOwnerUserId()).isEqualTo(5L);
        assertThat(s.getNextEscalationAt()).isAfter(LocalDateTime.now().plusMinutes(500));
        assertThat(claim.isTransmitted()).isFalse();
    }

    // --- Message d'attente au client ------------------------------------------------------------------------------

    @Test
    void enModeAutomatiqueLeClientRecoitUnMessageDAttenteUneSeuleFois() {
        when(config.customerWaitingMessage()).thenReturn("AUTO");
        when(notifier.clientWaitingMessage(any())).thenReturn(true);
        ClaimSla s = counter(1200);

        scanner.processOne(10L);
        s.setBreachNotified(false);
        s.setNextCheckAt(LocalDateTime.now().minusMinutes(1));
        scanner.processOne(10L);

        verify(notifier, times(1)).clientWaitingMessage(claim);
    }

    @Test
    void parDefautAucunMessageNEstEnvoyeAuClient() {
        when(config.customerWaitingMessage()).thenReturn("NONE");
        counter(1200);

        scanner.processOne(10L);

        verify(notifier, never()).clientWaitingMessage(any());
    }

    @Test
    void uneDenonciationNeRecoitJamaisDeMessageDAttente() {
        when(config.customerWaitingMessage()).thenReturn("AUTO");
        claim.setType(ClaimType.DENUNCIACION);
        ClaimSla s = counter(1200);
        s.setTargetType(ClaimType.DENUNCIACION);

        scanner.processOne(10L);

        verify(notifier, never()).clientWaitingMessage(any());
    }

    // --- Remontée automatique ------------------------------------------------------------------------------------

    private void stubLadder() {
        when(hierarchy.nextTarget(any(), eq(SlaOwnerLevel.AGENT), any()))
                .thenReturn(new SlaHierarchy.Target(ra, SlaOwnerLevel.RA, List.of(direction)));
        when(hierarchy.nextTarget(any(), eq(SlaOwnerLevel.RA), any()))
                .thenReturn(new SlaHierarchy.Target(direction, SlaOwnerLevel.DIRECTION, List.of(user(9, "Pilote Neuf"))));
        when(hierarchy.nextTarget(any(), eq(SlaOwnerLevel.DIRECTION), any()))
                .thenReturn(new SlaHierarchy.Target(user(9, "Pilote Neuf"), SlaOwnerLevel.PILOTE, List.of(user(11, "DE Onze"))));
        when(hierarchy.nextTarget(any(), eq(SlaOwnerLevel.PILOTE), any()))
                .thenReturn(new SlaHierarchy.Target(user(11, "DE Onze"), SlaOwnerLevel.DE, List.of()));
        when(hierarchy.nextTarget(any(), eq(SlaOwnerLevel.DE), any())).thenReturn(null);
        when(hierarchy.pilotes()).thenReturn(List.of(user(9, "Pilote Neuf")));
        when(hierarchy.des()).thenReturn(List.of(user(11, "DE Onze")));
    }

    /** Dossier déjà signalé en retard, dont le délai de grâce est écoulé : prêt à remonter. */
    private ClaimSla readyToEscalate() {
        ClaimSla s = counter(1200);
        s.setBreachNotified(true);
        s.setReminder1Sent(true);
        s.setReminder2Sent(true);
        s.setNextEscalationAt(LocalDateTime.now().minusMinutes(1));
        return s;
    }

    @Test
    void sansActionLeDossierRemonteAuRaAvecSystemeGprCommeAuteur() {
        stubLadder();
        ClaimSla s = readyToEscalate();

        scanner.processOne(10L);

        assertThat(claim.isTransmitted()).isTrue();
        assertThat(claim.getTransmittedTo()).isSameAs(ra);
        assertThat(claim.getTransmittedBy()).isNull();
        verify(claimEvents).log(eq(1L), eq("REC-1"), eq(ClaimType.CLAIM), eq(ClaimEventType.AUTO_TRANSMITTED),
                eq("Système GPR"), isNull(), contains("Agent Cinq"));
        ArgumentCaptor<HistoriqueTransmission> h = ArgumentCaptor.forClass(HistoriqueTransmission.class);
        verify(transmissionRepo).save(h.capture());
        assertThat(h.getValue().getTransmisParNom()).isEqualTo("Système GPR");
        assertThat(h.getValue().getTransmisAId()).isEqualTo(7L);
        assertThat(s.getOwnerUserId()).isEqualTo(7L);
        assertThat(s.getOwnerLevel()).isEqualTo(SlaOwnerLevel.RA);
        assertThat(s.getEscalationCount()).isEqualTo(1);
        // chaque niveau a son propre délai de grâce ; le chronomètre du client n'est jamais remis à zéro
        assertThat(s.getNextEscalationAt()).isAfter(LocalDateTime.now().plusMinutes(500));
        assertThat(s.getResolutionDueAt()).isBefore(LocalDateTime.now());
    }

    @Test
    void laRemonteeVaDeNiveauEnNiveauJusquAuDeEtSArrete() {
        stubLadder();
        ClaimSla s = readyToEscalate();
        List<SlaOwnerLevel> levels = new java.util.ArrayList<>();

        for (int i = 0; i < 4; i++) {
            scanner.processOne(10L);
            levels.add(s.getOwnerLevel());
            // on simule l'écoulement du délai de grâce du niveau atteint
            s.setNextCheckAt(LocalDateTime.now().minusMinutes(1));
            if (s.getNextEscalationAt() != null) {
                s.setNextEscalationAt(LocalDateTime.now().minusMinutes(1));
            }
        }

        assertThat(levels).containsExactly(SlaOwnerLevel.RA, SlaOwnerLevel.DIRECTION, SlaOwnerLevel.PILOTE,
                SlaOwnerLevel.DE);
        assertThat(s.getEscalationCount()).isEqualTo(4);
        // au dernier niveau : plus de transmission
        assertThat(s.getNextEscalationAt()).isNull();
        verify(claimEvents, times(4)).log(any(), any(), any(), eq(ClaimEventType.AUTO_TRANSMITTED), any(), any(), any());
    }

    @Test
    void auDernierNiveauLePiloteEtLeDeSontPrevenusUneSeuleFois() {
        stubLadder();
        ClaimSla s = readyToEscalate();
        s.setOwnerLevel(SlaOwnerLevel.DE);
        s.setOwnerUserId(11L);

        scanner.processOne(10L);

        verify(claimEvents).log(any(), any(), any(), eq(ClaimEventType.SLA_STUCK), eq("Système GPR"), any(), any());
        assertThat(s.isStuckNotified()).isTrue();
        assertThat(s.getNextEscalationAt()).isNull();
        assertThat(claim.isTransmitted()).isFalse(); // aucune transmission supplémentaire
        verify(transmissionRepo, never()).save(any());
    }

    @Test
    void uneActionHumaineArreteLaRemontee() {
        stubLadder();
        ClaimSla s = readyToEscalate();
        // le RA réaffecte : le moteur repousse la remontée et le prochain contrôle
        engine.humanAction(s, LocalDateTime.now());
        engine.recompute(s);
        assertThat(s.getNextEscalationAt()).isAfter(LocalDateTime.now());

        scanner.processOne(10L);

        verify(claimEvents, never()).log(any(), any(), any(), eq(ClaimEventType.AUTO_TRANSMITTED), any(), any(), any());
        assertThat(claim.isTransmitted()).isFalse();
    }

    @Test
    void uneRemonteeDesactiveeParLeParametreNeTransmetRien() {
        stubLadder();
        when(config.autoEscalation()).thenReturn(false);
        readyToEscalate();

        scanner.processOne(10L);

        assertThat(claim.isTransmitted()).isFalse();
        verifyNoInteractions(transmissionRepo);
    }

    // --- Idempotence --------------------------------------------------------------------------------------------

    @Test
    void uneAlerteN_EstJamaisEnvoyeeDeuxFois() {
        ClaimSla s = counter(1200);

        scanner.processOne(10L);
        // une deuxième instance du serveur (ou un second passage) revoit le même compteur
        s.setBreachNotified(false);
        s.setNextCheckAt(LocalDateTime.now().minusMinutes(1));
        scanner.processOne(10L);

        verify(claimEvents, times(1)).log(any(), any(), any(), eq(ClaimEventType.SLA_BREACH), any(), any(), any());
        verify(notifier, times(1)).mail(any(), contains("dépassé"), any());
    }

    // --- Échéance réglementaire --------------------------------------------------------------------------------

    @Test
    void auJour30SansReponseLaPlainteEstHorsDelaiReglementaire() {
        ClaimSla s = counter(1200);
        s.setRegulatoryDueAt(LocalDateTime.now().minusDays(1));

        scanner.processOne(10L);

        assertThat(s.isRegulatoryBreached()).isTrue();
        assertThat(s.isRegulatoryBreachSent()).isTrue();
        verify(claimEvents).log(any(), any(), any(), eq(ClaimEventType.SLA_REGULATORY_BREACH), eq("Système GPR"), any(), any());
        verify(notifier).mail(any(), contains("réglementaire"), any());
    }

    @Test
    void cinqJoursAvantLaLimiteUneAlerteEstEnvoyee() {
        ClaimSla s = counter(100);
        s.setRegulatoryDueAt(LocalDateTime.now().plusDays(3));

        scanner.processOne(10L);

        assertThat(s.isRegulatoryWarningSent()).isTrue();
        assertThat(s.isRegulatoryBreached()).isFalse();
        verify(claimEvents).log(any(), any(), any(), eq(ClaimEventType.SLA_REGULATORY_WARNING), any(), any(), any());
    }

    @Test
    void lAttenteDuClientSuspendLeDelaiInterneMaisPasLEcheanceReglementaire() {
        ClaimSla s = counter(1200); // délai interne déjà dépassé
        s.setPhase(SlaPhase.PAUSED);
        s.setPausedAt(LocalDateTime.now().minusDays(2));
        s.setRegulatoryDueAt(LocalDateTime.now().minusMinutes(5));

        scanner.processOne(10L);

        // pas d'alerte de délai interne pendant la pause
        verify(claimEvents, never()).log(any(), any(), any(), eq(ClaimEventType.SLA_BREACH), any(), any(), any());
        // mais l'échéance réglementaire, elle, continue
        verify(claimEvents).log(any(), any(), any(), eq(ClaimEventType.SLA_REGULATORY_BREACH), any(), any(), any());
        assertThat(s.isRegulatoryBreached()).isTrue();
    }

    @Test
    void uneEcheanceReglementaireNEstPlusSuivieApresLApprobation() {
        ClaimSla s = counter(1200);
        s.setResolvedAt(LocalDateTime.now().minusMinutes(10));
        s.setClosureDueAt(LocalDateTime.now().plusDays(1));
        s.setRegulatoryDueAt(LocalDateTime.now().minusDays(1));

        scanner.processOne(10L);

        verify(claimEvents, never()).log(any(), any(), any(), eq(ClaimEventType.SLA_REGULATORY_BREACH), any(), any(), any());
        assertThat(s.isRegulatoryBreached()).isFalse();
    }

    // --- Contrôles de sécurité du traitement -----------------------------------------------------------------------

    @Test
    void uneBrouillonOuUnCompteurNonEchuNEstPasTraite() {
        ClaimSla s = counter(1200);
        s.setNextCheckAt(LocalDateTime.now().plusHours(1));

        scanner.processOne(10L);

        verifyNoInteractions(claimEvents);
        verifyNoInteractions(notifier);
    }

    @Test
    void uneplainteSupprimeeAnnuleSonCompteur() {
        ClaimSla s = counter(1200);
        claim.setDeleted(true);

        scanner.processOne(10L);

        assertThat(s.getPhase()).isEqualTo(SlaPhase.CANCELLED);
        verifyNoInteractions(claimEvents);
    }

    // --- Aucune identité dans les messages -------------------------------------------------------------------------

    private static Claim claimWithIdentity(ClaimType type) {
        Claim c = new Claim();
        c.setId(9L);
        c.setType(type);
        c.setCode("code-interne-xyz");
        c.setCodeClient("REC-9999");
        c.setClientFirstAndLastName("Jean Dupont");
        c.setTel("+22900112233");
        c.setEmail("jean.dupont@example.com");
        c.setAddress("Rue des Fleurs 12");
        c.setObjet(Objet.builder().libelle("Frais de dossier").build());
        c.setServicePoint(ServicePoint.builder().libelle("Agence Test").build());
        return c;
    }

    private static List<String> allMessages(SlaMessages.Line line) {
        LocalDateTime d = LocalDateTime.of(2026, 10, 8, 9, 0);
        return List.of(
                SlaMessages.reminder("Bonjour Agent", line, 50),
                SlaMessages.breach("Bonjour Agent", line),
                SlaMessages.takeoverBreach("Bonjour RA", line),
                SlaMessages.escalation("Bonjour RA", line, "Agent", "RA", 3),
                SlaMessages.stuck("Bonjour DE", List.of(line)),
                SlaMessages.regulatoryWarning("Bonjour", line, 5),
                SlaMessages.regulatoryBreach("Bonjour", line),
                SlaMessages.digest("Bonjour", List.of(line)),
                SlaMessages.sms("délai dépassé", line),
                SlaMessages.date(d));
    }

    @Test
    void uneDenonciationNeReveleJamaisLIdentiteDuDenonciateur() {
        Claim c = claimWithIdentity(ClaimType.DENUNCIACION);
        SlaMessages.Line line = SlaNotifier.line(c, LocalDateTime.of(2026, 10, 8, 9, 0), "50 % écoulé");

        assertThat(line.code()).isEqualTo("code-interne-xyz"); // pas le code client
        for (String message : allMessages(line)) {
            assertThat(message).doesNotContain("Jean Dupont", "112233", "jean.dupont", "Rue des Fleurs", "REC-9999");
        }
    }

    @Test
    void uneReclamationNeRevelePasNonPlusLeNomNiLeTelephone() {
        Claim c = claimWithIdentity(ClaimType.CLAIM);
        SlaMessages.Line line = SlaNotifier.line(c, LocalDateTime.of(2026, 10, 8, 9, 0), "50 % écoulé");

        assertThat(line.code()).isEqualTo("REC-9999"); // le code d'une réclamation est l'identifiant habituel
        for (String message : allMessages(line)) {
            assertThat(message).doesNotContain("Jean Dupont", "112233", "jean.dupont", "Rue des Fleurs");
        }
    }
}
