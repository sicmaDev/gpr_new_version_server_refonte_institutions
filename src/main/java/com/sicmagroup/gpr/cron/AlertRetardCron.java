package com.sicmagroup.gpr.cron;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.HistoriqueAffectation;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.HistoriqueAffectationRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
import com.sicmagroup.gpr.utils.Utils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AlertRetardCron {

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private HistoriqueAffectationRepository historiqueAffectationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SettingServiceImpl settingService;

    // Exécution une fois par jour à 8h du matin
    @Scheduled(cron = "0 0 8 * * *")
    public void sendRelanceMail() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime fourDaysLater = now.plusDays(4);

            List<HistoriqueAffectation> affectationsEnRetard = historiqueAffectationRepository.findRetardAffectations(now);
            List<HistoriqueAffectation> affectationsBientotEnRetard =
                    historiqueAffectationRepository.findSoonToRetardAffectations(now, fourDaysLater);

            affectationsEnRetard.addAll(affectationsBientotEnRetard);

            List<Claim> plaintesNonAffectees = getClaimsNonAffecteesEnRetard(now, fourDaysLater);

            sendToAgents(affectationsEnRetard);

            sendToPilotes(affectationsEnRetard, plaintesNonAffectees);
        } catch (Exception e) {
            System.out.println("Erreur lors de l'envoi du mail : " + e.getMessage());
        }
    }

    private List<Claim> getClaimsNonAffecteesEnRetard(LocalDateTime now, LocalDateTime fourDaysLater) {
        return claimRepository.findByStatus(ClaimStatus.SAVED).stream()
                .filter(claim -> claim.getObjet() != null && claim.getObjet().getProcessingTime() > 0)
                .filter(claim -> claim.getCreatedAt().plusDays(claim.getObjet().getProcessingTime())
                        .isBefore(fourDaysLater))
                .collect(Collectors.toList());
    }

    private void sendToAgents(List<HistoriqueAffectation> affectations) {
        Map<String, List<HistoriqueAffectation>> affectationsParAgent =
                affectations.stream().collect(Collectors.groupingBy(HistoriqueAffectation::getEmailAgent));

        for (Map.Entry<String, List<HistoriqueAffectation>> entry : affectationsParAgent.entrySet()) {
            String emailAgent = entry.getKey();
            List<HistoriqueAffectation> affectationsAgent = entry.getValue();

            try {
                String subject = "Alerte - Réclamations en retard (" + affectationsAgent.size() + ")";
                String body = buildAgentMail(affectationsAgent);

                User user = userRepository.findByEmailAndIsDeleted(emailAgent, false)
                        .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

                Utils.sendmail(emailAgent, subject, body, null, " ", settingService);

            } catch (Exception e) {
                System.out.println("Erreur envoi agent : " + e.getMessage());
            }
        }
    }

    private void sendToPilotes(List<HistoriqueAffectation> affectations, List<Claim> claimsNonAffectees) {
        try {
            List<User> pilotes = userRepository.findAll().stream()
                    .filter(user -> user.getAdditionalrole() == Role.PILOTE || user.canSeeAll())
                    .collect(Collectors.toList());

            if (pilotes.isEmpty()) return;

            String subject = "Alerte - Réclamations en retard (" +
                    (affectations.size() + claimsNonAffectees.size()) + ")";
            String body = buildPiloteMail(affectations, claimsNonAffectees);

            User firstPilote = pilotes.get(0);
            String pilotesCC = pilotes.stream().map(User::getEmail).collect(Collectors.joining(","));

            Utils.sendmail(firstPilote.getEmail(), subject, body, pilotesCC, " ", settingService);

        } catch (Exception e) {
            System.out.println("Erreur envoi pilotes : " + e.getMessage());
        }
    }

    // ==== Builders de mail identiques à ta version ====
    private String buildAgentMail(List<HistoriqueAffectation> affectations) {
        StringBuilder body = new StringBuilder();
        body.append("Bonjour,\n\n");
        body.append("Vous avez ").append(affectations.size()).append(" plainte(s) en retard de traitement :\n\n");

        for (HistoriqueAffectation affectation : affectations) {
            LocalDateTime dateLimite = affectation.getDateAffectation().plusDays(affectation.getDelaiJours());
            long joursRetard = java.time.temporal.ChronoUnit.DAYS.between(dateLimite, LocalDateTime.now());

            body.append("- Code: ").append(affectation.getCodePlainte()).append("\n")
                    .append(" | Type: ").append(affectation.getTypePlainte()).append("\n")
                    .append(" | Affectée le: ").append(affectation.getDateAffectation().toLocalDate()).append("\n")
                    .append(" | Délai: ").append(affectation.getDelaiJours()).append(" jours").append("\n");

            if (joursRetard > 0) {
                body.append(" | RETARD: ").append(joursRetard).append(" jours");
            } else {
                body.append(" | Échéance dans: ").append(-joursRetard).append(" jours");
            }
            body.append("\n \n");
        }

        body.append("\nMerci de traiter ces plaintes dans les plus brefs délais.\n\n");
        body.append("Cordialement, GPR -  WEB");

        return body.toString();
    }

    private String buildPiloteMail(List<HistoriqueAffectation> affectations, List<Claim> claimsNonAffectees) {
        StringBuilder body = new StringBuilder();
        body.append("Bonjour,\n\n");
        body.append("Résumé des plaintes en retard de traitement :\n\n");

        if (!affectations.isEmpty()) {
            body.append("=== PLAINTES AFFECTÉES EN RETARD (").append(affectations.size()).append(") ===\n");

            Map<String, List<HistoriqueAffectation>> parAgent = affectations.stream()
                    .collect(Collectors.groupingBy(HistoriqueAffectation::getEmailAgent));

            for (Map.Entry<String, List<HistoriqueAffectation>> entry : parAgent.entrySet()) {
                body.append("\nAgent: ").append(entry.getKey()).append(" (").append(entry.getValue().size())
                        .append(" plaintes)\n").append("\n");

                for (HistoriqueAffectation affectation : entry.getValue()) {
                    LocalDateTime dateLimite = affectation.getDateAffectation().plusDays(affectation.getDelaiJours());
                    long joursRetard = java.time.temporal.ChronoUnit.DAYS.between(dateLimite, LocalDateTime.now());

                    body.append("  - ").append(affectation.getCodePlainte()).append("\n")
                            .append(" (").append(affectation.getTypePlainte()).append(")").append("\n");

                    if (joursRetard > 0) {
                        body.append(" - RETARD: ").append(joursRetard).append(" jours");
                    } else {
                        body.append(" - Échéance: ").append(-joursRetard).append(" jours");
                    }
                    body.append("\n");
                }
            }
        }

        if (!claimsNonAffectees.isEmpty()) {
            body.append("\n \n === PLAINTES NON AFFECTÉES EN RETARD (").append(claimsNonAffectees.size())
                    .append(") ===\n");

            for (Claim claim : claimsNonAffectees) {
                LocalDateTime dateLimite = claim.getCreatedAt().plusDays(claim.getObjet().getProcessingTime());
                long joursRetard = java.time.temporal.ChronoUnit.DAYS.between(dateLimite, LocalDateTime.now());

                body.append("- Code: ").append(claim.getCode())
                        .append(" | Type: ").append(claim.getType())
                        .append(" | Objet: ").append(claim.getObjet().getLibelle())
                        .append(" | Créée le: ").append(claim.getCreatedAt().toLocalDate());

                if (joursRetard > 0) {
                    body.append(" | RETARD: ").append(joursRetard).append(" jours");
                } else {
                    body.append(" | Échéance: ").append(-joursRetard).append(" jours");
                }
                body.append("\n");
            }
        }

        body.append("\nMerci de traiter ces plaintes dans les plus brefs délais.\n\n");
        body.append("\nCordialement, GPR - WEB");

        return body.toString();
    }

}
