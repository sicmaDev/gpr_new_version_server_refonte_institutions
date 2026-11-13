package com.sicmagroup.gpr.service.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.api.claim.AiFeedbackRequest;
import com.sicmagroup.gpr.domain.enumeration.AiFeedbackStatus;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;

/** À l'enregistrement, le serveur recopie lui-même l'analyse de l'IA (jamais celle d'une autre personne). */
class RattachementAnalyseIaTest {

    private final AnalysesIaRecentes analyses = new AnalysesIaRecentes();
    private final ClaimRepository claims = mock(ClaimRepository.class);
    private final SuggestionRepository suggestions = mock(SuggestionRepository.class);
    private final RattachementAnalyseIa rattachement = new RattachementAnalyseIa(analyses, claims, suggestions, new ObjectMapper());

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    private static void connecter(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }

    private AnalyseIa analyse(String id) {
        AnalyseIa a = new AnalyseIa();
        a.setCorrelationId(id);
        a.setCategorie("Compte");
        a.setMotif("Frais excessifs");
        a.setUrgence("FAIBLE");
        a.setSentiment("negatif");
        a.setResume("Conteste des frais.");
        a.getMotsCles().add("frais");
        return a;
    }

    @Test
    void lAnalyseDeLAgentEstRecopieeSurLaReclamation() {
        analyses.enregistrer(analyse("cai-1"), "agent@gpr");
        connecter("agent@gpr");
        when(claims.save(any(Claim.class))).thenAnswer(i -> i.getArgument(0));
        Claim c = rattachement.rattacher(Claim.builder().id(5L).build(), "cai-1");
        assertThat(c.getAiSuggestedCategory()).isEqualTo("Compte");
        assertThat(c.getAiSuggestedMotif()).isEqualTo("Frais excessifs");
        assertThat(c.getAiUrgency()).isEqualTo("FAIBLE");
        assertThat(c.getAiKeywords()).isEqualTo("[\"frais\"]");
        assertThat(c.getCorrelationId()).isEqualTo("cai-1");
    }

    @Test
    void lAnalyseDUneAutrePersonneNEstPasReprise() {
        analyses.enregistrer(analyse("cai-2"), "autre@gpr");
        connecter("agent@gpr");
        Claim c = rattachement.rattacher(Claim.builder().id(6L).build(), "cai-2");
        assertThat(c.getAiSuggestedCategory()).isNull();
        verify(claims, never()).save(any());
    }

    @Test
    void sansNumeroDAnalyseRienNeChangeEtLEnregistrementContinue() {
        connecter("agent@gpr");
        Suggestion s = Suggestion.builder().id(7L).build();
        assertThat(rattachement.rattacher(s, null)).isSameAs(s);
        assertThat(rattachement.rattacher((Claim) null, "cai-x")).isNull();
        verify(suggestions, never()).save(any());
    }

    @Test
    void noteDuRetourDeLAgent() {
        AiFeedbackRequest r = AiFeedbackRequest.builder().status(AiFeedbackStatus.VALIDATED).finalCategory("Compte")
                .finalMotif("Frais").retainedSolution("Rembourser").satisfactionStatus("SATISFIED").build();
        assertThat(RetourAgentIa.note(r)).isEqualTo(4);
        r.setStatus(AiFeedbackStatus.REJECTED);
        r.setFinalCategory(null);
        r.setFinalMotif(null);
        r.setRetainedSolution(null);
        r.setSatisfactionStatus(null);
        assertThat(RetourAgentIa.note(r)).isZero();
    }
}
