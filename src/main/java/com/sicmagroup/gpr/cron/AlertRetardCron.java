package com.sicmagroup.gpr.cron;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
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
   
    // @Scheduled(cron = "0 0 8 * * *")
    // @Scheduled(cron = "0 01 18 * * *")
    @Scheduled(cron = "0 45 14 * * *")
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
                String email = emailAgent.trim();
                Utils.sendmail(email, subject, body, null, " ", settingService);

            } catch (Exception e) {
                System.out.println("Erreur envoi agent : " + e.getMessage());
            }
        }
    }

    private void sendToPilotes(List<HistoriqueAffectation> affectations, List<Claim> claimsNonAffectees) {
        try {
            List<User> pilotes = userRepository.findAll().stream()
                    .filter(user -> user.getAdditionalrole() == Role.PILOTE)
                    .collect(Collectors.toList());

            if (pilotes.isEmpty()) return;

            // String subject = "Alerte - Réclamations en retard (" +
            //     (affectations.size() + claimsNonAffectees.size()) + ")"+
            //     (System.currentTimeMillis() % 100000);
            String subject = "Alerte - Réclamations en retard " + (int)(System.currentTimeMillis() % 100000);

            String body = buildPiloteMail(affectations, claimsNonAffectees);

            User firstPilote = pilotes.get(0);
            
            // String pilotesCC = pilotes.stream().map(User::getEmail).collect(Collectors.joining(","));

            // Utils.sendmail(firstPilote.getEmail(), subject, body, pilotesCC, " ", settingService);
            String email = firstPilote.getEmail().trim();
            Utils.sendmail(email, subject, body, null, " ", settingService);

        } catch (Exception e) {
            System.out.println("Erreur envoi pilotes : " + e.getMessage());
        }
    }

    private String buildAgentMail(List<HistoriqueAffectation> affectations) {
        // En-tête du mail avec le nombre de plaintes
        String body = """
            <html>
            <body style="font-family: Arial, sans-serif; background-color: #f4f6f8; padding: 20px;">
            <div style="max-width: 750px; margin: auto; background: #ffffff; border-radius: 10px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 25px;">
                <h2 style="color: #004080; text-align: center; margin-bottom: 10px;">
                Suivi des plaintes affectées - GPR
                </h2>
                <p style="text-align:center; color:#666; margin-bottom:25px;">
                Bonjour,<br>
                Vous avez <strong>%d plainte(s)</strong> en retard ou proches de l’échéance :
                </p>
                <table style="width:100%; border-collapse:collapse; font-size:14px; margin-top:10px;">
                <thead>
                    <tr style="background:#f2f2f2; text-align:left;">
                    <th style="padding:8px; border:1px solid #ddd;">Code</th>
                    <th style="padding:8px; border:1px solid #ddd;">Type</th>
                    <th style="padding:8px; border:1px solid #ddd;">Affectée le</th>
                    <th style="padding:8px; border:1px solid #ddd;">Délai (jours)</th>
                    <th style="padding:8px; border:1px solid #ddd;">Statut</th>
                    </tr>
                </thead>
                <tbody>
            """.formatted(affectations.size());

        // Boucle pour ajouter les lignes de plaintes
        for (HistoriqueAffectation affectation : affectations) {
            LocalDateTime dateLimite = affectation.getDateAffectation().plusDays(affectation.getDelaiJours());
            long joursRetard = java.time.temporal.ChronoUnit.DAYS.between(dateLimite, LocalDateTime.now());

            String statut = (joursRetard > 0)
                    ? "<span style='color:#d9534f;'>⏰ Retard de " + joursRetard + " jour(s)</span>"
                    : "<span style='color:#5bc0de;'>🕒 Échéance dans " + (-joursRetard) + " jour(s)</span>";

            body += """
                <tr>
                <td style='padding:8px; border:1px solid #ddd;'>%s</td>
                <td style='padding:8px; border:1px solid #ddd;'>%s</td>
                <td style='padding:8px; border:1px solid #ddd;'>%s</td>
                <td style='padding:8px; border:1px solid #ddd; text-align:center;'>%d</td>
                <td style='padding:8px; border:1px solid #ddd;'>%s</td>
                </tr>
            """.formatted(
                    affectation.getCodePlainte(),
                    affectation.getTypePlainte().equals(ClaimType.CLAIM) ? "Réclamation":"Dénonciation",
                    affectation.getDateAffectation().toLocalDate(),
                    affectation.getDelaiJours(),
                    statut
            );
        }

        // Footer du mail
        body += """
                </tbody>
                </table>
                <p style="margin-top:25px; line-height:1.6;">
                Merci de traiter ces plaintes dans les plus brefs délais afin d’assurer un suivi efficace.
                </p>
                <p style="margin-top:20px;">Cordialement,<br>
                <strong>L’équipe GPR - WEB</strong>
                </p>
                <p style="font-size:12px; color:gray; text-align:center; margin-top:30px;">
                Cet email a été généré automatiquement par la plateforme GPR.<br>
                Merci de ne pas y répondre.
                </p>
            </div>
            </body>
            </html>
        """;

        return body;
    }

    private String buildPiloteMail(List<HistoriqueAffectation> affectations, List<Claim> claimsNonAffectees) {

        StringBuilder body = new StringBuilder();

        body.append("<html>");
        body.append("<body style='font-family: Arial, sans-serif; background-color: #f4f6f8; padding: 20px;'>");
        body.append("<div style='max-width:800px; margin:auto; background:#ffffff; border-radius:8px;");
        body.append("box-shadow:0 2px 8px rgba(0,0,0,0.1); padding:25px;'>");

        // ---- TITRE ----
        body.append("<h2 style='color:#004080; text-align:center;'>Rapport de suivi des plaintes en retard - GPR</h2>");
        body.append("<p>Bonjour,</p>");
        body.append("<p>Veuillez trouver ci-dessous un résumé des plaintes en retard de traitement.</p>");

        // ==========================================================
        //         PLAINTES AFFECTÉES EN RETARD
        // ==========================================================
        if (!affectations.isEmpty()) {
            body.append("<h3 style='color:#d9534f;'>Plaintes affectées en retard (")
                .append(affectations.size()).append(")</h3>");

            Map<String, List<HistoriqueAffectation>> parAgent = affectations
                .stream().collect(Collectors.groupingBy(HistoriqueAffectation::getNomAgent));

            for (Map.Entry<String, List<HistoriqueAffectation>> entry : parAgent.entrySet()) {

                body.append("<h4 style='color:#004080;'>Agent : ").append(entry.getKey()).append(" (")
                    .append(entry.getValue().size()).append(" plainte(s))</h4>");

                body.append("<table style='width:100%; border-collapse: collapse; margin-bottom:20px;'>");
                body.append("<thead>");
                body.append("<tr style='background-color:#f2f2f2;'>");
                body.append("<th style='border:1px solid #ddd; padding:8px;'>Code</th>");
                body.append("<th style='border:1px solid #ddd; padding:8px;'>Type</th>");
                body.append("<th style='border:1px solid #ddd; padding:8px;'>Échéance</th>");
                body.append("<th style='border:1px solid #ddd; padding:8px;'>Retard</th>");
                body.append("</tr>");
                body.append("</thead>");
                body.append("<tbody>");

                for (HistoriqueAffectation affectation : entry.getValue()) {
                    LocalDateTime dateLimite = affectation.getDateAffectation()
                            .plusDays(affectation.getDelaiJours());
                    long joursRetard = ChronoUnit.DAYS.between(dateLimite, LocalDateTime.now());

                    body.append("<tr>");
                    body.append("<td style='border:1px solid #ddd; padding:8px;'>").append(affectation.getCodePlainte()).append("</td>");
                    body.append("<td style='border:1px solid #ddd; padding:8px;'>")
                        .append(affectation.getTypePlainte().equals(ClaimType.CLAIM) ? "Réclamation":"Dénonciation")
                        .append("</td>");
                    body.append("<td style='border:1px solid #ddd; padding:8px;'>")
                        .append(dateLimite.toLocalDate()).append("</td>");
                    body.append("<td style='border:1px solid #ddd; padding:8px; color:")
                        .append(joursRetard > 0 ? "#d9534f" : "#5bc0de").append(";'>")
                        .append(joursRetard > 0 ? joursRetard + " jour(s) de retard" : "Échéance dans " + (-joursRetard) + " jour(s)")
                        .append("</td>");
                    body.append("</tr>");
                }

                body.append("</tbody>");
                body.append("</table>");
            }
        }

        // ==========================================================
        //         PLAINTES NON AFFECTÉES EN RETARD
        // ==========================================================
        if (!claimsNonAffectees.isEmpty()) {
            body.append("<h3 style='color:#f0ad4e;'>Plaintes non affectées en retard (")
                .append(claimsNonAffectees.size()).append(")</h3>");

            body.append("<table style='width:100%; border-collapse: collapse; margin-bottom:20px;'>");
            body.append("<thead>");
            body.append("<tr style='background-color:#f9f2e7;'>");
            body.append("<th style='border:1px solid #ddd; padding:8px;'>Code</th>");
            body.append("<th style='border:1px solid #ddd; padding:8px;'>Type</th>");
            body.append("<th style='border:1px solid #ddd; padding:8px;'>Reçue le</th>");
            body.append("<th style='border:1px solid #ddd; padding:8px;'>Retard</th>");
            body.append("</tr>");
            body.append("</thead>");
            body.append("<tbody>");

            for (Claim claim : claimsNonAffectees) {
                LocalDateTime dateLimite = claim.getCreatedAt()
                        .plusDays(claim.getObjet().getProcessingTime());
                long joursRetard = ChronoUnit.DAYS.between(dateLimite, LocalDateTime.now());

                body.append("<tr>");
                body.append("<td style='border:1px solid #ddd; padding:8px;'>").append(claim.getCodeClient()).append("</td>");
                body.append("<td style='border:1px solid #ddd; padding:8px;'>")
                    .append(claim.getType().equals(ClaimType.CLAIM) ? "Réclamation":"Dénonciation")
                    .append("</td>");
                body.append("<td style='border:1px solid #ddd; padding:8px;'>").append(claim.getReceiptDateTime().toLocalDate()).append("</td>");
                body.append("<td style='border:1px solid #ddd; padding:8px; color:")
                    .append(joursRetard > 0 ? "#d9534f" : "#5bc0de").append(";'>")
                    .append(joursRetard > 0 ? joursRetard + " jour(s) de retard" : "Échéance dans " + (-joursRetard) + " jour(s)")
                    .append("</td>");
                body.append("</tr>");
            }

            body.append("</tbody>");
            body.append("</table>");
        }

        // ==========================================================
        //         CONCLUSION
        // ==========================================================
        body.append("<p style='margin-top:30px;'>Merci de prendre les dispositions nécessaires pour le traitement de ces plaintes dans les plus brefs délais.</p>");
        body.append("<p style='margin-top:20px;'>Cordialement,<br><strong>L’équipe GPR</strong></p>");
        body.append("<p style='font-size:12px; color:gray; text-align:center; margin-top:30px;'>Cet email a été généré automatiquement par la plateforme GPR.</p>");

        body.append("</div>");
        body.append("</body>");
        body.append("</html>");

        return body.toString();
    }


}
