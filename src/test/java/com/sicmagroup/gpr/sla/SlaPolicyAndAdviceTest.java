package com.sicmagroup.gpr.sla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ClaimDto;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.GravityLevel;
import com.sicmagroup.gpr.sla.api.SlaResponseAdvice;
import com.sicmagroup.gpr.sla.domain.BusinessDay;
import com.sicmagroup.gpr.sla.domain.SlaBreachReason;
import com.sicmagroup.gpr.sla.domain.SlaPolicy;
import com.sicmagroup.gpr.sla.repository.BusinessDayRepository;
import com.sicmagroup.gpr.sla.repository.SlaBreachReasonRepository;
import com.sicmagroup.gpr.sla.repository.SlaPolicyRepository;
import com.sicmagroup.gpr.sla.service.SlaInfoService;
import com.sicmagroup.gpr.sla.service.SlaPolicyService;

/**
 * Politiques par défaut (valeurs de la spécification), initialisation de la configuration, et ajout du bloc « sla »
 * aux listes de plaintes existantes. Test unitaire : aucune base de données.
 */
class SlaPolicyAndAdviceTest {

    private final SlaPolicyRepository policyRepo = mock(SlaPolicyRepository.class);
    private final BusinessDayRepository dayRepo = mock(BusinessDayRepository.class);
    private final SlaBreachReasonRepository reasonRepo = mock(SlaBreachReasonRepository.class);
    private SlaPolicyService service;

    @BeforeEach
    void setUp() {
        service = new SlaPolicyService(policyRepo, dayRepo, reasonRepo);
    }

    // --- Valeurs par défaut de la spécification (un jour ouvré = 9 h = 540 minutes) --------------------------------

    @Test
    void reclamationGrave_4hPriseEnCharge_3jResolution_2jCloture() {
        SlaPolicy p = SlaPolicyService.defaultPolicy(ClaimType.CLAIM, GravityLevel.GRAVE);

        assertThat(p.getTakeoverMinutes()).isEqualTo(4 * 60);
        assertThat(p.getResolutionMinutes()).isEqualTo(3 * 540);
        assertThat(p.getClosureMinutes()).isEqualTo(2 * 540);
        assertThat(p.getReminder1Pct()).isEqualTo(50);
        assertThat(p.getReminder2Pct()).isEqualTo(75);
        assertThat(p.getGraceMinutes()).isEqualTo(540);
        assertThat(p.isBusinessTime()).isTrue();
        assertThat(p.isActive()).isTrue();
    }

    @Test
    void reclamationMoyenne_1j_6j_3j() {
        SlaPolicy p = SlaPolicyService.defaultPolicy(ClaimType.CLAIM, GravityLevel.MOYEN);

        assertThat(p.getTakeoverMinutes()).isEqualTo(540);
        assertThat(p.getResolutionMinutes()).isEqualTo(6 * 540);
        assertThat(p.getClosureMinutes()).isEqualTo(3 * 540);
    }

    @Test
    void reclamationMineure_2j_10j_5j() {
        SlaPolicy p = SlaPolicyService.defaultPolicy(ClaimType.CLAIM, GravityLevel.MINEUR);

        assertThat(p.getTakeoverMinutes()).isEqualTo(2 * 540);
        assertThat(p.getResolutionMinutes()).isEqualTo(10 * 540);
        assertThat(p.getClosureMinutes()).isEqualTo(5 * 540);
    }

    @Test
    void uneDenonciationNAPasDeDelaiDeCloture() {
        for (GravityLevel risk : GravityLevel.values()) {
            SlaPolicy p = SlaPolicyService.defaultPolicy(ClaimType.DENUNCIACION, risk);
            assertThat(p.getClosureMinutes()).as("clôture " + risk).isZero();
            assertThat(p.getResolutionMinutes()).isPositive();
        }
    }

    @Test
    void uneSuggestionADesDelaisUniques() {
        SlaPolicy p = SlaPolicyService.defaultPolicy(ClaimType.SUGGESTION, GravityLevel.MOYEN);

        assertThat(p.getTakeoverMinutes()).isZero();
        assertThat(p.getClosureMinutes()).isZero();
        assertThat(p.getResolutionMinutes()).isEqualTo(10 * 540);
    }

    // --- Lecture d'une politique -------------------------------------------------------------------------------------

    @Test
    void laPolitiqueEnregistreeActivePrimeSurLesValeursParDefaut() {
        SlaPolicy custom = SlaPolicy.builder().claimType(ClaimType.CLAIM).riskLevel(GravityLevel.GRAVE)
                .resolutionMinutes(999).active(true).build();
        when(policyRepo.findByClaimTypeAndRiskLevel(ClaimType.CLAIM, GravityLevel.GRAVE)).thenReturn(Optional.of(custom));

        assertThat(service.policyFor(ClaimType.CLAIM, GravityLevel.GRAVE).getResolutionMinutes()).isEqualTo(999);
    }

    @Test
    void uneLaPolitiqueDesactiveeRetombeSurLesValeursParDefaut() {
        SlaPolicy off = SlaPolicy.builder().claimType(ClaimType.CLAIM).riskLevel(GravityLevel.GRAVE)
                .resolutionMinutes(999).active(false).build();
        when(policyRepo.findByClaimTypeAndRiskLevel(ClaimType.CLAIM, GravityLevel.GRAVE)).thenReturn(Optional.of(off));

        assertThat(service.policyFor(ClaimType.CLAIM, GravityLevel.GRAVE).getResolutionMinutes()).isEqualTo(3 * 540);
    }

    @Test
    void sansNiveauDeRisqueLaPolitiqueMoyenneS_Applique() {
        assertThat(service.policyFor(ClaimType.CLAIM, null).getResolutionMinutes()).isEqualTo(6 * 540);
    }

    // --- Initialisation de la configuration ------------------------------------------------------------------------------

    @Test
    void lInitialisationCreeSeptPolitiquesLeCalendrierEtLesMotifs() {
        when(policyRepo.findByClaimTypeAndRiskLevel(any(), any())).thenReturn(Optional.empty());
        when(dayRepo.count()).thenReturn(0L);
        when(reasonRepo.count()).thenReturn(0L);

        service.seedDefaults();

        verify(policyRepo, times(7)).save(any(SlaPolicy.class)); // 2 types x 3 risques + suggestions
        ArgumentCaptor<BusinessDay> days = ArgumentCaptor.forClass(BusinessDay.class);
        verify(dayRepo, times(7)).save(days.capture());
        assertThat(days.getAllValues()).filteredOn(BusinessDay::isWorking).hasSize(5); // lundi à vendredi
        assertThat(days.getAllValues()).filteredOn(d -> d.getDayOfWeek() >= 6).noneMatch(BusinessDay::isWorking);
        ArgumentCaptor<SlaBreachReason> reasons = ArgumentCaptor.forClass(SlaBreachReason.class);
        verify(reasonRepo, org.mockito.Mockito.atLeast(2)).save(reasons.capture());
        assertThat(reasons.getAllValues()).extracting(SlaBreachReason::getKind).contains("RETARD", "NON_MESURE");
    }

    @Test
    void lInitialisationNeRemplaceRienDeCeQuiExiste() {
        when(policyRepo.findByClaimTypeAndRiskLevel(any(), any())).thenReturn(Optional.of(new SlaPolicy()));
        when(dayRepo.count()).thenReturn(7L);
        when(reasonRepo.count()).thenReturn(8L);

        service.seedDefaults();

        verify(policyRepo, never()).save(any());
        verify(dayRepo, never()).save(any());
        verify(reasonRepo, never()).save(any());
    }

    // --- Bloc « sla » ajouté aux listes existantes ---------------------------------------------------------------------

    private final SlaInfoService info = mock(SlaInfoService.class);
    private final SlaResponseAdvice advice = new SlaResponseAdvice(info);

    private Object write(Object body) {
        return advice.beforeBodyWrite(body, null, null, null, null, null);
    }

    @Test
    void uneListeDePlaintesReçoitLeBlocSla() {
        ClaimDto a = ClaimDto.builder().id(1L).type(ClaimType.CLAIM).build();
        ApiResponseDto body = ApiResponseDto.builder().status(true).content(List.of(a)).build();

        assertThat(write(body)).isSameAs(body);

        verify(info).attach(List.of(a));
    }

    @Test
    void lePlainteDUnEcranDeTraitementReçoitSonBlocSla() {
        ClaimDto one = ClaimDto.builder().id(2L).type(ClaimType.DENUNCIACION).build();

        write(ApiResponseDto.builder().status(true).content(one).build());

        verify(info).attach(List.of(one));
    }

    @Test
    void lesAutresReponsesNeSontPasTouchees() {
        write(ApiResponseDto.builder().status(true).content(List.of("texte")).build());
        write(ApiResponseDto.builder().status(true).content(List.of()).build());
        write(ApiResponseDto.builder().status(true).content("autre").build());
        write("pas une réponse de l'API");

        verify(info, never()).attach(any());
    }

    @Test
    void uneErreurDuSlaNeCassePasLaReponseDeLaListe() {
        ClaimDto a = ClaimDto.builder().id(1L).type(ClaimType.CLAIM).build();
        doThrow(new IllegalStateException("panne")).when(info).attach(any());
        ApiResponseDto body = ApiResponseDto.builder().status(true).content(List.of(a)).build();

        // la liste d'origine est renvoyée telle quelle
        assertThat(write(body)).isSameAs(body);
    }
}
