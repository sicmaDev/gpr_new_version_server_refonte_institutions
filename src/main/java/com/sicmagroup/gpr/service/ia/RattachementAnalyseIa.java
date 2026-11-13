package com.sicmagroup.gpr.service.ia;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * À l'enregistrement d'un dossier, recopie le résultat de l'analyse IA que le serveur a lui-même reçue
 * du Core (mémoire {@link AnalysesIaRecentes}) : le navigateur ne fournit que le numéro d'analyse.
 * Ne bloque jamais l'enregistrement : en cas de problème, le dossier reste enregistré sans analyse.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RattachementAnalyseIa {

    private final AnalysesIaRecentes analyses;
    private final ClaimRepository claimRepository;
    private final SuggestionRepository suggestionRepository;
    private final ObjectMapper json;

    public Claim rattacher(Claim claim, String correlationId) {
        if (claim == null) return null;
        try {
            Optional<AnalyseIa> a = trouver(correlationId);
            if (a.isEmpty()) return claim;
            AnalyseIa x = a.get();
            claim.setCorrelationId(x.getCorrelationId());
            claim.setAiSummary(x.getResume());
            claim.setAiSentiment(x.getSentiment());
            claim.setAiUrgency(x.getUrgence());
            claim.setAiKeywords(json.writeValueAsString(x.getMotsCles()));
            claim.setAiSuggestedCategory(x.getCategorie());
            claim.setAiSuggestedMotif(x.getMotif());
            claim.setAiRaisonnementClassification(x.getRaisonnementClassification());
            claim.setAiRaisonnementUrgence(x.getRaisonnementUrgence());
            return claimRepository.save(claim);
        } catch (Exception e) {
            log.warn("Analyse IA non rattachée au dossier {} : {}", claim.getId(), e.getClass().getSimpleName());
            return claim;
        }
    }

    public Suggestion rattacher(Suggestion suggestion, String correlationId) {
        if (suggestion == null) return null;
        try {
            Optional<AnalyseIa> a = trouver(correlationId);
            if (a.isEmpty()) return suggestion;
            AnalyseIa x = a.get();
            suggestion.setCorrelationId(x.getCorrelationId());
            suggestion.setAiSummary(x.getResume());
            suggestion.setAiSentiment(x.getSentiment());
            suggestion.setAiUrgency(x.getUrgence());
            suggestion.setAiKeywords(json.writeValueAsString(x.getMotsCles()));
            suggestion.setAiSuggestedCategory(x.getCategorie());
            suggestion.setAiSuggestedMotif(x.getMotif());
            suggestion.setAiRaisonnementClassification(x.getRaisonnementClassification());
            suggestion.setAiRaisonnementUrgence(x.getRaisonnementUrgence());
            return suggestionRepository.save(suggestion);
        } catch (Exception e) {
            log.warn("Analyse IA non rattachée à la suggestion {} : {}", suggestion.getId(), e.getClass().getSimpleName());
            return suggestion;
        }
    }

    private Optional<AnalyseIa> trouver(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) return Optional.empty();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return Optional.empty();
        return analyses.recuperer(correlationId, auth.getName());
    }
}
