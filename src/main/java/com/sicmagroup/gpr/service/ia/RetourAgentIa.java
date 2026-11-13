package com.sicmagroup.gpr.service.ia;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.api.claim.AiFeedbackRequest;
import com.sicmagroup.gpr.domain.enumeration.AiFeedbackStatus;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ClaimRepository;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;

/**
 * Enregistre ce que l'agent a fait de la proposition de l'IA (catégorie, motif et solution retenus),
 * pour mesurer si l'IA a vu juste. Même calcul de note que l'ancienne version GPR.
 */
@Service
@RequiredArgsConstructor
public class RetourAgentIa {

    private final ClaimRepository claimRepository;

    public Claim enregistrer(Long claimId, AiFeedbackRequest r, User agent) {
        if (r == null || r.getStatus() == null) {
            throw new IllegalArgumentException("Le statut du retour sur l'IA est obligatoire");
        }
        Claim claim = claimRepository.findById(claimId).orElseThrow(() -> new NoSuchElementException("Dossier introuvable"));
        claim.setAiFeedbackStatus(r.getStatus());
        claim.setAiFinalCategory(limiter(r.getFinalCategory(), 255));
        claim.setAiFinalMotif(limiter(r.getFinalMotif(), 255));
        claim.setAiFinalUrgency(limiter(r.getFinalUrgency(), 255));
        claim.setAiRetainedSolution(r.getRetainedSolution());
        claim.setAiFeedbackAgentId(agent != null ? agent.getId() : null);
        claim.setAiFeedbackJustification(r.getJustification());
        claim.setAiFeedbackSatisfactionStatus(limiter(r.getSatisfactionStatus(), 40));
        claim.setAiFeedbackDate(LocalDateTime.now());
        claim.setAiQualityScore(note(r));
        return claimRepository.save(claim);
    }

    static int note(AiFeedbackRequest r) {
        int score = 0;
        if (r.getFinalCategory() != null && !r.getFinalCategory().isBlank()) score++;
        if (r.getFinalMotif() != null && !r.getFinalMotif().isBlank()) score++;
        if (r.getRetainedSolution() != null && !r.getRetainedSolution().isBlank()) score++;
        if ("SATISFIED".equalsIgnoreCase(r.getSatisfactionStatus())) score++;
        if (r.getStatus() == AiFeedbackStatus.REJECTED) score--;
        return Math.max(score, 0);
    }

    private static String limiter(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
