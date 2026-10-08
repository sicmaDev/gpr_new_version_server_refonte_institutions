package com.sicmagroup.gpr.sla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.GravityLevel;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ClaimEventRepository;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.sla.domain.ClaimSla;
import com.sicmagroup.gpr.sla.domain.SlaOwnerLevel;
import com.sicmagroup.gpr.sla.domain.SlaPhase;
import com.sicmagroup.gpr.sla.dto.SlaRows.ItemRow;
import com.sicmagroup.gpr.sla.repository.ClaimSlaRepository;
import com.sicmagroup.gpr.sla.service.BusinessTime;
import com.sicmagroup.gpr.sla.service.SlaConfig;
import com.sicmagroup.gpr.sla.service.SlaInfoService;
import com.sicmagroup.gpr.sla.service.SlaPerimeter;
import com.sicmagroup.gpr.sla.service.SlaQueryService;
import com.sicmagroup.gpr.sla.service.SlaSummaryCache;

/**
 * Lecture du SLA pour les écrans : résumé en une lecture, mémoire de 60 s, totaux des listes lus dans le résumé,
 * recherche par code, identité jamais exposée. Test unitaire : aucune base de données.
 */
class SlaQueryServiceTest {

    private final ClaimSlaRepository repo = mock(ClaimSlaRepository.class);
    private final SlaPerimeter perimeter = mock(SlaPerimeter.class);
    private final SlaConfig config = mock(SlaConfig.class);
    private final User pilote = User.builder().id(9L).build();

    private SlaQueryService service;

    /** [total, ouvertes, en cours, à risque, dépassées, suspendues, respectées, hors délai, traitées non mesurées, proches] */
    private static final Object[] COUNTS = { 100L, 60L, 30L, 10L, 20L, 15L, 22L, 3L, 7L, 4L };

    @BeforeEach
    void setUp() {
        SlaSummaryCache.clear();
        service = new SlaQueryService(repo, mock(ClaimRepository.class), perimeter,
                new SlaInfoService(repo, mock(UserRepository.class), new BusinessTime(), config), config,
                mock(ClaimEventRepository.class));
        when(config.regulatoryWarningDays()).thenReturn(5);
        when(perimeter.scopeOf(any())).thenReturn(new SlaPerimeter.Scope(true, List.of(-1L), 9L, 9L));
        when(repo.summaryCounts(any(), any(), any(), anyBoolean(), anyCollection(), anyLong(), anyLong()))
                .thenReturn(List.<Object[]>of(COUNTS));
    }

    private ClaimSla counter(long claimId, ClaimType type) {
        LocalDateTime received = LocalDateTime.now().minusHours(5);
        return ClaimSla.builder().id(claimId).targetType(type).claimId(claimId).cycle(1).phase(SlaPhase.OPEN)
                .receivedAt(received).resolutionDueAt(received.plusHours(20)).dueAt(received.plusHours(20))
                .resolutionMinutes(1200).reminder1Pct(50).reminder2Pct(75).graceMinutes(60).businessTime(false)
                .ownerLevel(SlaOwnerLevel.RA).build();
    }

    private ItemRow row(long id, ClaimType type) {
        return new ItemRow(id, type, "code-" + id, "REC-" + id, ClaimStatus.AFFECTED, "Objet", GravityLevel.GRAVE,
                "Agence A", LocalDateTime.now().minusDays(1));
    }

    // --- Résumé -------------------------------------------------------------------------------------------------

    @Test
    void leResumeEstLuEnUneSeuleRequeteEtMisEnForme() {
        Map<String, Object> s = service.summary(pilote, null);

        assertThat(s.get("TOTAL")).isEqualTo(100L);
        assertThat(s.get("OUVERTES")).isEqualTo(60L);
        assertThat(s.get("EN_COURS")).isEqualTo(30L);
        assertThat(s.get("A_RISQUE")).isEqualTo(10L);
        assertThat(s.get("DEPASSE")).isEqualTo(20L);
        assertThat(s.get("SUSPENDU")).isEqualTo(15L);
        assertThat(s.get("RESPECTE")).isEqualTo(22L);
        assertThat(s.get("HORS_DELAI")).isEqualTo(3L);
        assertThat(s.get("TRAITEES_NON_MESUREES")).isEqualTo(7L);
        assertThat(s.get("PROCHES_REGLEMENTAIRE")).isEqualTo(4L);
        // taux de respect = respectées / (respectées + dépassées + hors délai) = 22 / 45
        assertThat((Double) s.get("tauxRespect")).isEqualTo(48.9);
        verify(repo, times(1)).summaryCounts(any(), any(), any(), anyBoolean(), anyCollection(), anyLong(), anyLong());
    }

    @Test
    void leResumeEstGardeEnMemoireJusquAuProchainChangement() {
        service.summary(pilote, null);
        service.summary(pilote, null);
        service.count(pilote, "DEPASSE", null);

        verify(repo, times(1)).summaryCounts(any(), any(), any(), anyBoolean(), anyCollection(), anyLong(), anyLong());

        // une action sur une plainte vide la mémoire : la lecture suivante est fraîche
        SlaSummaryCache.clear();
        service.summary(pilote, null);
        verify(repo, times(2)).summaryCounts(any(), any(), any(), anyBoolean(), anyCollection(), anyLong(), anyLong());
    }

    @Test
    void chaquePerimetreADonneSonPropreResume() {
        // (les utilisateurs se comparent par leurs données : on distingue par identifiant)
        User agent = User.builder().id(5L).build();
        when(perimeter.scopeOf(any())).thenAnswer(inv -> ((User) inv.getArgument(0)).getId() == 9L
                ? new SlaPerimeter.Scope(true, List.of(-1L), 9L, 9L)
                : new SlaPerimeter.Scope(false, List.of(-1L), 5L, 5L));

        service.summary(pilote, null);
        service.summary(agent, null);

        verify(repo, times(2)).summaryCounts(any(), any(), any(), anyBoolean(), anyCollection(), anyLong(), anyLong());
    }

    @Test
    void sansPlainteLeResumeEstA_Zero() {
        when(repo.summaryCounts(any(), any(), any(), anyBoolean(), anyCollection(), anyLong(), anyLong()))
                .thenReturn(List.<Object[]>of(new Object[] { 0L, null, null, null, null, null, null, null, null, null }));

        Map<String, Object> s = service.summary(pilote, null);

        assertThat(s.get("DEPASSE")).isEqualTo(0L);
        assertThat(s.get("tauxRespect")).isNull();
    }

    @Test
    void leBadgeDuMenuLitLeResumeSansRequeteSupplementaire() {
        assertThat(service.count(pilote, "DEPASSE", null)).isEqualTo(20L);
        assertThat(service.count(pilote, "A_RISQUE", null)).isEqualTo(10L);
        assertThat(service.count(pilote, "ALL", null)).isEqualTo(100L);

        verify(repo, times(1)).summaryCounts(any(), any(), any(), anyBoolean(), anyCollection(), anyLong(), anyLong());
        verify(repo, never()).findInScope(anyString(), any(), any(), anyBoolean(), anyCollection(), anyLong(), anyLong(),
                any(Pageable.class));
    }

    @Test
    void lesDenonciationsNeCompterentPasDeMesureDeSatisfaction() {
        Map<String, Object> s = service.summary(pilote, ClaimType.DENUNCIACION);

        assertThat(s.get("TRAITEES_NON_MESUREES")).isEqualTo(0L);
    }

    // --- Listes --------------------------------------------------------------------------------------------------

    @Test
    void laListeNeFaitPasDeSecondComptageEtLeTotalVientDuResume() {
        when(repo.findInScope(eq("DEPASSE"), isNull(), any(), anyBoolean(), anyCollection(), anyLong(), anyLong(),
                any(Pageable.class))).thenReturn(new SliceImpl<>(List.of(counter(1, ClaimType.CLAIM)), PageRequest.of(0, 20), false));
        when(repo.itemRows(anyCollection())).thenReturn(List.of(row(1, ClaimType.CLAIM)));

        Map<String, Object> page = service.items(pilote, "DEPASSE", null, null, 0, 20);

        assertThat(page.get("total")).isEqualTo(20L); // nombre de plaintes en retard du résumé
        assertThat(page.get("totalPages")).isEqualTo(1);
        assertThat((List<?>) page.get("items")).hasSize(1);
        verify(repo, never()).claimIdsMatching(anyString(), any(Pageable.class));
    }

    @Test
    void uneRechercheParCodeTrouveD_abordLesPlaintesPuisAppliqueLePerimetre() {
        when(repo.claimIdsMatching(eq("%rec-12%"), any(Pageable.class))).thenReturn(List.of(12L));
        when(repo.findInScopeByIds(eq("ALL"), isNull(), anyCollection(), any(), anyBoolean(), anyCollection(), anyLong(),
                anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(counter(12, ClaimType.CLAIM)), PageRequest.of(0, 20), 1));
        when(repo.itemRows(anyCollection())).thenReturn(List.of(row(12, ClaimType.CLAIM)));

        Map<String, Object> page = service.items(pilote, null, null, " REC-12 ", 0, 20);

        assertThat(page.get("total")).isEqualTo(1L);
        assertThat((List<?>) page.get("items")).hasSize(1);
    }

    @Test
    void uneRechercheSansResultatRenvoieUneListeVide() {
        when(repo.claimIdsMatching(anyString(), any(Pageable.class))).thenReturn(List.of());

        Map<String, Object> page = service.items(pilote, "ALL", null, "introuvable", 0, 20);

        assertThat((List<?>) page.get("items")).isEmpty();
        assertThat(page.get("total")).isEqualTo(0L);
        verify(repo, never()).findInScopeByIds(anyString(), any(), anyCollection(), any(), anyBoolean(), anyCollection(),
                anyLong(), anyLong(), any(Pageable.class));
    }

    @Test
    void laListeAfficheLeCodeClientPourLesDeuxTypes() {
        when(repo.findInScope(anyString(), any(), any(), anyBoolean(), anyCollection(), anyLong(), anyLong(), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(counter(2, ClaimType.DENUNCIACION), counter(3, ClaimType.CLAIM)),
                        PageRequest.of(0, 20), false));
        when(repo.itemRows(anyCollection())).thenReturn(List.of(row(2, ClaimType.DENUNCIACION), row(3, ClaimType.CLAIM)));

        @SuppressWarnings("unchecked")
        List<com.sicmagroup.gpr.sla.dto.SlaItem> items = (List<com.sicmagroup.gpr.sla.dto.SlaItem>) service
                .items(pilote, "ALL", null, null, 0, 20).get("items");

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getCodeClient()).isEqualTo("REC-2"); // même code client que dans la liste des dénonciations
        assertThat(items.get(0).getCode()).isEqualTo("code-2");
        assertThat(items.get(1).getCodeClient()).isEqualTo("REC-3"); // réclamation : code habituel
    }

    @Test
    void laTailleDePageEstBornee() {
        when(repo.findInScope(anyString(), any(), any(), anyBoolean(), anyCollection(), anyLong(), anyLong(), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 200), false));

        Map<String, Object> page = service.items(pilote, "ALL", null, null, 0, 100000);

        assertThat(page.get("size")).isEqualTo(200);
    }
}
