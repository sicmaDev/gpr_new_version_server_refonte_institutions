package com.sicmagroup.gpr.sla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sicmagroup.gpr.domain.enumeration.ClaimEventType;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.sla.domain.ClaimSla;
import com.sicmagroup.gpr.sla.domain.SlaEvent;
import com.sicmagroup.gpr.sla.domain.SlaPhase;
import com.sicmagroup.gpr.sla.repository.ClaimSlaRepository;
import com.sicmagroup.gpr.sla.repository.SlaEventRepository;
import com.sicmagroup.gpr.sla.service.SlaConfig;
import com.sicmagroup.gpr.sla.service.SlaDigest;
import com.sicmagroup.gpr.sla.service.SlaEngine;
import com.sicmagroup.gpr.sla.service.SlaEventHook;
import com.sicmagroup.gpr.sla.service.SlaHierarchy;
import com.sicmagroup.gpr.sla.service.SlaNotifier;

/**
 * Récapitulatif quotidien (un seul envoi par jour, regroupé par personne) et pont avec le journal des événements
 * (le SLA ne doit jamais bloquer le traitement d'une plainte). Test unitaire : tout est simulé.
 */
class SlaDigestAndHookTest {

    private final SlaEventRepository eventRepo = mock(SlaEventRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final ClaimSlaRepository slaRepo = mock(ClaimSlaRepository.class);
    private final ClaimRepository claimRepo = mock(ClaimRepository.class);
    private final SlaNotifier notifier = mock(SlaNotifier.class);
    private final SlaHierarchy hierarchy = mock(SlaHierarchy.class);
    private final SlaConfig config = mock(SlaConfig.class);
    private SlaDigest digest;

    private final User agent = User.builder().id(5L).firstandlastname("Agent").email("a@gpr.local").build();
    private final User ra = User.builder().id(7L).firstandlastname("RA").email("r@gpr.local").build();

    @BeforeEach
    void setUp() {
        digest = new SlaDigest(eventRepo, userRepo, slaRepo, claimRepo, notifier, hierarchy, config);
        when(config.enabled()).thenReturn(true);
        when(config.digestTime()).thenReturn(LocalTime.of(8, 0));
        when(userRepo.findById(5L)).thenReturn(Optional.of(agent));
        when(userRepo.findById(7L)).thenReturn(Optional.of(ra));
    }

    private static final LocalDateTime APRES_8H = LocalDate.of(2026, 10, 7).atTime(9, 0);

    private SlaEvent digestItem(long userId, String code) {
        return SlaEvent.builder().eventType("DIGEST_ITEM").recipientUserId(userId).digestPending(true)
                .detail("CLAIM|" + code + "|Objet|Agence A|2026-10-08T09:00|50 % écoulé").createdAt(APRES_8H.minusHours(5)).build();
    }

    // --- Récapitulatif quotidien ----------------------------------------------------------------------------------

    @Test
    void lesRappelsSontRegroupesEnUnSeulMessageParPersonne() {
        SlaEvent e1 = digestItem(5, "REC-1");
        SlaEvent e2 = digestItem(5, "REC-2");
        SlaEvent e3 = digestItem(7, "REC-1");
        when(eventRepo.findByDigestPendingTrueOrderByRecipientUserIdAscCreatedAtAsc()).thenReturn(List.of(e1, e2, e3));

        boolean sent = digest.sendIfDue(APRES_8H);

        assertThat(sent).isTrue();
        // un message pour l'agent (2 plaintes), un pour le RA (1 plainte) : jamais un message par rappel
        verify(notifier, org.mockito.Mockito.times(2)).mail(any(), contains("Récapitulatif"), any());
        assertThat(e1.isDigestPending()).isFalse();
        assertThat(e2.getDigestSentAt()).isEqualTo(APRES_8H);
        assertThat(e3.isDigestPending()).isFalse();
    }

    @Test
    void leRecapitulatifNePartQuUneFoisParJour() {
        when(eventRepo.existsByDedupKey("DIGEST_RUN:2026-10-07")).thenReturn(true); // déjà envoyé (autre instance, ou passage précédent)

        assertThat(digest.sendIfDue(APRES_8H)).isFalse();

        verify(notifier, never()).mail(any(), any(), any());
        verify(eventRepo, never()).findByDigestPendingTrueOrderByRecipientUserIdAscCreatedAtAsc();
    }

    @Test
    void ilNePartPasAvantLHeurePrevue() {
        assertThat(digest.sendIfDue(LocalDate.of(2026, 10, 7).atTime(7, 59))).isFalse();

        verifyNoInteractions(notifier);
    }

    @Test
    void sansSlaActifRienNePart() {
        when(config.enabled()).thenReturn(false);

        assertThat(digest.sendIfDue(APRES_8H)).isFalse();

        verifyNoInteractions(notifier);
    }

    @Test
    void lesDossiersBloquesAuDernierNiveauSontRappelesChaqueJourAuPiloteEtAuDe() {
        when(eventRepo.findByDigestPendingTrueOrderByRecipientUserIdAscCreatedAtAsc()).thenReturn(List.of());
        ClaimSla bloque = ClaimSla.builder().id(1L).targetType(ClaimType.CLAIM).claimId(1L).phase(SlaPhase.OPEN)
                .stuckNotified(true).resolutionDueAt(APRES_8H.minusDays(2)).build();
        when(slaRepo.findByPhaseAndStuckNotifiedTrue(SlaPhase.OPEN)).thenReturn(List.of(bloque));
        Claim claim = new Claim();
        claim.setId(1L);
        claim.setType(ClaimType.CLAIM);
        claim.setCodeClient("REC-1");
        when(claimRepo.findById(1L)).thenReturn(Optional.of(claim));
        when(hierarchy.pilotes()).thenReturn(List.of(User.builder().id(9L).email("p@gpr.local").build()));
        when(hierarchy.des()).thenReturn(List.of());

        digest.sendIfDue(APRES_8H);

        verify(notifier).mail(any(), contains("bloqués"), any());
    }

    @Test
    void sansDossierBloqueAucunMessageDeBlocage() {
        when(eventRepo.findByDigestPendingTrueOrderByRecipientUserIdAscCreatedAtAsc()).thenReturn(List.of());
        when(slaRepo.findByPhaseAndStuckNotifiedTrue(SlaPhase.OPEN)).thenReturn(List.of());

        digest.sendIfDue(APRES_8H);

        verify(notifier, never()).mail(any(), contains("bloqués"), any());
    }

    // --- Pont avec le journal des événements ------------------------------------------------------------------------

    @Test
    void sansSlaActifLePontNeTouchePasAuMoteur() {
        SlaEngine engine = mock(SlaEngine.class);
        SlaConfig off = mock(SlaConfig.class);
        when(off.enabled()).thenReturn(false);

        new SlaEventHook(engine, off).afterEvent(1L, ClaimType.CLAIM, ClaimEventType.SAVED);

        verifyNoInteractions(engine);
    }

    @Test
    void uneErreurDuMoteurNeFaitJamaisEchouerLeTraitementDeLaPlainte() {
        SlaEngine engine = mock(SlaEngine.class);
        doThrow(new IllegalStateException("panne du SLA")).when(engine).onEvent(any(), any(), any());

        // aucune exception ne doit remonter jusqu'à l'agent qui traite la plainte
        new SlaEventHook(engine, config).afterEvent(1L, ClaimType.CLAIM, ClaimEventType.APPROVED);

        verify(engine).onEvent(eq(1L), eq(ClaimType.CLAIM), eq(ClaimEventType.APPROVED));
    }

    @Test
    void uneErreurDeLectureDesParametresNeFaitJamaisEchouerLeTraitement() {
        SlaEngine engine = mock(SlaEngine.class);
        SlaConfig broken = mock(SlaConfig.class);
        when(broken.enabled()).thenThrow(new IllegalStateException("base indisponible"));

        new SlaEventHook(engine, broken).afterEvent(1L, ClaimType.CLAIM, ClaimEventType.SAVED);

        verifyNoInteractions(engine);
    }
}
