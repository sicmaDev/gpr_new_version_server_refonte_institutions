package com.sicmagroup.gpr.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sicmagroup.gpr.api.report.FilterRequest;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.GravityLevel;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerObjLevelPro;

/**
 * Tableaux récapitulatifs du rapport Global (constats 7 et 8) : les calculs ne modifient plus le filtre
 * partagé, et le taux de satisfaction divise par les réclamations du filtre. Aussi : gravité additionnée
 * par niveau, ligne « Faible » des dénonciations, suggestions sous filtre d'objet, filtre par état.
 * Sans base de données.
 */
class TableauxRecapitulatifsTest {

    private static FilterRequest filtreAgence() {
        return FilterRequest.builder()
                .objets(new ArrayList<>()).etats(new ArrayList<>()).products(new ArrayList<>())
                .savedBy(new ArrayList<>()).canals(new ArrayList<>())
                .servicePoints(new ArrayList<>(List.of(22L)))
                .build();
    }

    private static List<Claim> claims(int n) {
        List<Claim> l = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            l.add(new Claim());
        }
        return l;
    }

    @Test
    void copieIndependante() {
        FilterRequest f = filtreAgence();
        FilterRequest c = FilterRequest.copieDe(f);
        c.setEtats(List.of(ClaimStatus.SATISFIED));
        c.getServicePoints().add(23L);
        assertThat(f.getEtats()).isEmpty();
        assertThat(f.getServicePoints()).containsExactly(22L);
        assertThat(FilterRequest.copieDe(null)).isNull();
    }

    @Test
    void lesCalculsNeModifientPlusLeFiltre() {
        ClaimRepository repo = mock(ClaimRepository.class);
        when(repo.countClaimByCriteriaAndStatusIn(any(), any())).thenReturn(Collections.emptyList());
        when(repo.countClaimByCriteriaAndStatusNot(any(), any())).thenReturn(Collections.emptyList());
        StatsClaim statsClaim = new StatsClaim(repo);
        StatsDenun statsDenun = new StatsDenun(repo, null, null, null, null);
        FilterRequest f = filtreAgence();

        statsClaim.tauxSatisfaction(f);
        statsClaim.totalReclamantSatisfait(f);
        statsClaim.totalTreat(f);
        statsClaim.totalTreatByGravity(f);
        statsClaim.totalTreatByRespectTiming(f);
        statsClaim.totalUnTreat(f);
        statsClaim.totalUnTreatByGravity(f);
        statsDenun.totalTreat(f);
        statsDenun.totalTreatByGravity(f);
        statsDenun.totalTreatByRespectTiming(f);
        statsDenun.totalUnTreat(f);
        statsDenun.totalUnTreatByGravity(f);

        assertThat(f.getEtats()).isEmpty();
    }

    @Test
    void tauxDeSatisfactionDiviseParLesReclamationsDuFiltre() {
        ClaimRepository repo = mock(ClaimRepository.class);
        // 2 satisfaites sur 4 clôturées dans l'agence ; l'institution entière n'est jamais interrogée.
        when(repo.countClaimByCriteriaAndStatusIn(argThat(r -> r != null && r.getEtats().size() == 1), eq(ClaimType.CLAIM)))
                .thenReturn(claims(2));
        when(repo.countClaimByCriteriaAndStatusIn(argThat(r -> r != null && r.getEtats().size() == 5), eq(ClaimType.CLAIM)))
                .thenReturn(claims(4));
        StatsClaim statsClaim = new StatsClaim(repo);

        assertThat(statsClaim.tauxSatisfaction(filtreAgence()).get("Taux de satisfaction(%)")).isEqualTo(50.0);
        verify(repo, never()).findByTypeAndIsDeletedFalseAndStatusIn(any(), any());
    }

    @Test
    void graviteAdditionneLesObjetsDUnMemeNiveau() {
        ClaimRepository repo = mock(ClaimRepository.class);
        when(repo.countClaimByCriteriaAndByObjLevel(any(), eq(ClaimType.CLAIM))).thenReturn(List.of(
                new ClaimPerObjLevelPro(1L, "A", GravityLevel.MINEUR, 50L),
                new ClaimPerObjLevelPro(2L, "B", GravityLevel.MINEUR, 32L),
                new ClaimPerObjLevelPro(3L, "C", GravityLevel.GRAVE, 15L)));
        HashMap<String, Double> r = new StatsClaim(repo).totalByGravity(filtreAgence());

        assertThat(r.get("Nombre de réclamations à niveau de gravité Mineur enregistrées")).isEqualTo(82.0);
        assertThat(r.get("Nombre de réclamations à niveau de gravité Grave enregistrées")).isEqualTo(15.0);
        assertThat(r.get("Nombre de réclamations à niveau de gravité Moyen enregistrées")).isEqualTo(0.0);
    }

    @Test
    void denonciationsFaiblesNonTraiteesCompteesAPart() {
        ClaimRepository repo = mock(ClaimRepository.class);
        when(repo.findByTypeAndStatusNotIn(eq(ClaimType.DENUNCIACION), any()))
                .thenReturn(List.of(denonciation(GravityLevel.MINEUR), denonciation(GravityLevel.MINEUR),
                        denonciation(GravityLevel.MOYEN)));
        HashMap<String, Double> r = new StatsDenun(repo, null, null, null, null).totalUnTreatByGravity(null);

        assertThat(r.get("Nombre de dénonciations à niveau de gravité Faible non traitées")).isEqualTo(2.0);
        assertThat(r.get("Nombre de dénonciations à niveau de gravité Moyen non traitées")).isEqualTo(1.0);
    }

    @Test
    void suggestionsSansValeurSousUnFiltreParObjet() {
        StatsSuggest statsSuggest = new StatsSuggest(null, null, null, null, null);
        FilterRequest f = filtreAgence();
        f.setObjets(new ArrayList<>(List.of(1L)));

        assertThat(statsSuggest.totalSaved(f)).containsEntry("Nombre de suggestions Enregistrées", null);
        assertThat(statsSuggest.totalSuggest(f)).containsEntry("Nombre de suggestion Prises en compte", null)
                .containsEntry("Nombre de suggestion Non Prises en compte", null);
    }

    @Test
    void filtreParEtatSansBrouillons() {
        ClaimRepository repo = mock(ClaimRepository.class);
        when(repo.countClaimByCriteriaAndStatusIn(any(), any())).thenReturn(Collections.emptyList());
        FilterRequest f = filtreAgence();
        f.setEtats(new ArrayList<>(List.of(ClaimStatus.SATISFIED)));

        new StatsClaim(repo).pourcentageReelReclamationsTraitees(f);

        // Les réclamations de l'état choisi, et non « tout sauf cet état » (qui ramenait les brouillons sans objet).
        verify(repo).countClaimByCriteriaAndStatusIn(argThat(r -> r.getEtats().equals(List.of(ClaimStatus.SATISFIED))),
                eq(ClaimType.CLAIM));
        verify(repo, never()).countClaimByCriteriaAndStatusNot(any(), any());
        assertThat(f.getEtats()).containsExactly(ClaimStatus.SATISFIED);
    }

    private static Claim denonciation(GravityLevel niveau) {
        Objet objet = new Objet();
        objet.setRisqueLevel(niveau);
        Claim c = new Claim();
        c.setObjet(objet);
        return c;
    }

    @Test
    void tauxDeSatisfactionSansFiltreInchange() {
        ClaimRepository repo = mock(ClaimRepository.class);
        when(repo.findByTypeAndIsDeletedFalseAndStatusIn(eq(ClaimType.CLAIM), argThat(l -> l != null && l.size() == 1)))
                .thenReturn(claims(6));
        when(repo.findByTypeAndIsDeletedFalseAndStatusIn(eq(ClaimType.CLAIM), argThat(l -> l != null && l.size() == 5)))
                .thenReturn(claims(26));
        StatsClaim statsClaim = new StatsClaim(repo);

        assertThat(statsClaim.tauxSatisfaction(null).get("Taux de satisfaction(%)")).isEqualTo(23.08);
    }
}
