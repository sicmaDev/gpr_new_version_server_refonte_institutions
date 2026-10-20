package com.sicmagroup.gpr.cron;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sicmagroup.gpr.domain.model.Claim;
// import com.sicmagroup.gpr.domain.model.HistoriqueAffectation;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.domain.dto.ScheduledReportTestResult;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.repository.ClaimRepository;
// import com.sicmagroup.gpr.repository.HistoriqueAffectationRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.service.MailService;
import com.sicmagroup.gpr.service.scheduledReport.ScheduledReportConfigService;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
import com.sicmagroup.gpr.sla.service.SlaConfig;
import com.sicmagroup.gpr.utils.Utils;
import com.sicmagroup.gpr.domain.enumeration.ReportRecipientType;
import com.sicmagroup.gpr.domain.model.ScheduledReportConfig;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AlertRetardCron {

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SettingServiceImpl settingService;
    // Suivi des délais (SLA) : quand il est actif, il envoie lui-même les rappels et alertes (une seule règle, sans doublon)
    @Autowired
    private SlaConfig slaConfig;

    private final MailService mailService;
    private final ScheduledReportConfigService scheduledReportConfigService;

    @Scheduled(fixedDelay = 60000)
    public void sendRelanceMail() {
        // SLA actif : cette relance planifiée est remplacée par les rappels et alertes du SLA
        if (slaConfig.enabled()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledReportConfig> dueConfigurations;
        try {
            dueConfigurations = scheduledReportConfigService.getDueConfigurations(now);
        } catch (Exception e) {
            System.out.println("Erreur lors du cron retard (lecture de la planification) : " + e.getMessage());
            return;
        }

        if (dueConfigurations.isEmpty()) {
            return;
        }

        LocalDateTime fourDaysLater = now.plusDays(4);

        // Chaque configuration due est traitée et marquée exécutée indépendamment des autres,
        // dans un finally : si l'envoi à l'une échoue (ex: exception imprévue le temps de
        // calculer les destinataires), on ne doit surtout pas laisser l'autre - ni celle en
        // échec - "due" indéfiniment. Sans ce isolement, une seule erreur ponctuelle
        // transforme le cron (qui tourne toutes les 60s) en boucle de spam ignorant
        // totalement la fréquence planifiée, jusqu'au prochain redémarrage du serveur.
        for (ScheduledReportConfig config : dueConfigurations) {
            // sent = true seulement si au moins un mail est réellement parti (envoi
            // synchrone, donc confirmé) : c'est cette valeur qui pilote "Dernière relance",
            // distincte de lastExecutionAt qui avance de toute façon pour respecter le
            // rythme "une fois par jour" même quand il n'y avait rien à envoyer ou que
            // l'envoi a échoué.
            boolean sent = false;
            try {
                if (config.getRecipientType() == ReportRecipientType.AGENTS) {
                    AgentClaimSets agentSets = computeAgentClaimSets(now, fourDaysLater);
                    if (!agentSets.enRetard().isEmpty() || !agentSets.bientot().isEmpty()) {
                        sent = sendToAgents(agentSets.enRetard(), agentSets.bientot());
                    }
                } else if (config.getRecipientType() == ReportRecipientType.PILOTE) {
                    PiloteClaimSets piloteSets = computePiloteClaimSets(now, fourDaysLater);
                    if (!piloteSets.affectees().isEmpty() || !piloteSets.nonAffectees().isEmpty()) {
                        sent = sendToPilotes(piloteSets.affectees(), piloteSets.nonAffectees());
                    }
                }
            } catch (Exception e) {
                System.out.println("Erreur lors du cron retard (" + config.getRecipientType() + ") : " + e.getMessage());
            } finally {
                scheduledReportConfigService.markExecuted(config, now, sent);
            }
        }
    }

    /**
     * Envoi manuel de test déclenché depuis la page "Fréquences alertes" (bouton "Tester
     * l'envoi"). Génère le même contenu que la relance réelle (mêmes filtres, mêmes données
     * courantes) mais l'envoie uniquement à l'adresse fournie par l'utilisateur, de façon
     * synchrone, sans jamais toucher lastExecutionAt/nextExecutionAt : ce test n'a aucun effet
     * sur la planification réelle et ne contacte jamais les agents ou le pilote en production.
     */
    public ScheduledReportTestResult sendTestMail(ReportRecipientType type, String to) {
        if (to == null || to.isBlank()) {
            return ScheduledReportTestResult.builder()
                    .success(false)
                    .message("L'adresse email de test est obligatoire")
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fourDaysLater = now.plusDays(4);

        try {
            String subject;
            String body;
            int claimCount;

            if (type == ReportRecipientType.AGENTS) {
                AgentClaimSets sets = computeAgentClaimSets(now, fourDaysLater);
                List<Claim> all = new ArrayList<>();
                all.addAll(sets.enRetard());
                all.addAll(sets.bientot());
                claimCount = all.size();
                subject = "[TEST] Alerte - Réclamations en retard (" + claimCount + ")";
                body = all.isEmpty()
                        ? "<p>Aucune réclamation affectée en retard ou bientôt en retard pour le moment.</p>"
                        : buildAgentMail(all);
            } else {
                PiloteClaimSets sets = computePiloteClaimSets(now, fourDaysLater);
                claimCount = sets.affectees().size() + sets.nonAffectees().size();
                subject = "[TEST] Alerte - Réclamations en retard";
                body = buildPiloteMail(sets.affectees(), sets.nonAffectees());
            }

            mailService.sendMailSync(to, subject, body, null);

            return ScheduledReportTestResult.builder()
                    .success(true)
                    .claimCount(claimCount)
                    .message("Mail de test envoyé à " + to + " (" + claimCount + " réclamation(s) concernée(s)).")
                    .build();
        } catch (Exception e) {
            return ScheduledReportTestResult.builder()
                    .success(false)
                    .message("Échec de l'envoi : " + e.getMessage())
                    .build();
        }
    }

    private record AgentClaimSets(List<Claim> enRetard, List<Claim> bientot) {
    }

    private record PiloteClaimSets(List<Claim> affectees, List<Claim> nonAffectees) {
    }

    private AgentClaimSets computeAgentClaimSets(LocalDateTime now, LocalDateTime limit) {
        List<Claim> base = baseEligibleClaims();
        List<Claim> enRetard = base.stream()
                .filter(c -> c.getStatus().equals(ClaimStatus.AFFECTED))
                .filter(c -> isRetard(c, now))
                .toList();
        List<Claim> bientot = base.stream()
                .filter(c -> c.getStatus().equals(ClaimStatus.AFFECTED))
                .filter(c -> isSoonToRetard(c, now, limit))
                .toList();
        return new AgentClaimSets(enRetard, bientot);
    }

    private PiloteClaimSets computePiloteClaimSets(LocalDateTime now, LocalDateTime limit) {
        List<Claim> base = baseEligibleClaims();
        List<Claim> affectees = new ArrayList<>();
        affectees.addAll(base.stream().filter(c -> isRetard(c, now)).toList());
        affectees.addAll(base.stream().filter(c -> isSoonToRetard(c, now, limit)).toList());

        List<Claim> nonAffectees = claimRepository.findByStatusAndIsDeletedFalse(ClaimStatus.SAVED).stream()
                .filter(c -> isRetard(c, now))
                .filter(c -> c.getStatus() != ClaimStatus.LITIGATION)
                .filter(c -> c.getStatus() != ClaimStatus.SATISFIED)
                .filter(c -> c.getStatus() != ClaimStatus.UNSATISFIED)
                .filter(c -> c.getStatus() != ClaimStatus.PARTIAL_SATISFIED)
                .filter(c -> c.getStatus() != ClaimStatus.CLASSED)
                .filter(c -> !(c.getType() == ClaimType.DENUNCIACION
                        && c.getStatus() == ClaimStatus.TREAT))
                .toList();

        return new PiloteClaimSets(affectees, nonAffectees);
    }

    // Statuts suivis par la relance : ni terminés (satisfait, non satisfait, partiel, classé, contentieux), ni en
    // attente du client (le délai interne est alors en pause).
    private static final List<ClaimStatus> ELIGIBLE_STATUSES = List.of(ClaimStatus.SAVED, ClaimStatus.AFFECTED,
            ClaimStatus.TO_APPROUVED, ClaimStatus.DESAPPROUVED, ClaimStatus.TREAT, ClaimStatus.TRANSMITTED);

    private List<Claim> baseEligibleClaims() {
        List<Claim> claims = new ArrayList<>();
        claims.addAll(claimRepository.findByTypeAndIsDeletedFalseAndStatusIn(ClaimType.CLAIM, ELIGIBLE_STATUSES));
        // une dénonciation « traitée » est terminée
        claims.addAll(claimRepository.findByTypeAndIsDeletedFalseAndStatusIn(ClaimType.DENUNCIACION,
                ELIGIBLE_STATUSES.stream().filter(st -> st != ClaimStatus.TREAT).toList()));
        return claims.stream().filter(c -> c.getTreatmentAffectedTo() != null).toList();
    }

    private boolean isRetard(Claim claim, LocalDateTime reference) {
        if (claim.getObjet() == null)
            return false;
        int delai = claim.getObjet().getProcessingTime();
        if (delai <= 0)
            return false;

        LocalDateTime dateBase = claim.getAffectedAt() != null ? claim.getAffectedAt() : claim.getCreatedAt();
        LocalDateTime deadline = dateBase.plusDays(delai);
        return deadline.isBefore(reference);
    }

    private boolean isSoonToRetard(Claim claim, LocalDateTime now, LocalDateTime limit) {
        if (claim.getObjet() == null)
            return false;
        int delai = claim.getObjet().getProcessingTime();
        if (delai <= 0)
            return false;

        LocalDateTime dateBase = claim.getAffectedAt() != null ? claim.getAffectedAt() : claim.getCreatedAt();
        LocalDateTime deadline = dateBase.plusDays(delai);
        return deadline.isAfter(now) && deadline.isBefore(limit);
    }

    // ===== Envoi aux agents =====
    // Envoi synchrone (sendMailSync) : contrairement à mailService.sendMail (@Async, qui avale
    // toute erreur SMTP et rend la main immédiatement), on doit ici savoir si l'envoi a
    // vraiment abouti avant de marquer la relance comme "envoyée" (voir sendRelanceMail).
    // @return true si au moins un agent a effectivement reçu son mail.
    private boolean sendToAgents(List<Claim> enRetard, List<Claim> bientot) {
        List<Claim> all = new ArrayList<>();
        all.addAll(enRetard);
        all.addAll(bientot);

        Map<User, List<Claim>> parAgent = all.stream()
                .collect(Collectors.groupingBy(Claim::getTreatmentAffectedTo));

        boolean atLeastOneSent = false;
        for (var entry : parAgent.entrySet()) {
            User agent = entry.getKey();
            List<Claim> claims = entry.getValue();

            try {
                String subject = "Alerte - Réclamations en retard (" + claims.size() + ")";
                String body = buildAgentMail(claims);

                mailService.sendMailSync(agent.getEmail(), subject, body, null);
                atLeastOneSent = true;
            } catch (Exception e) {
                System.out.println("Erreur envoi agent (" + agent.getEmail() + ") : " + e.getMessage());
            }
        }
        return atLeastOneSent;
    }

    private String buildAgentMail(List<Claim> claims) {

        StringBuilder body = new StringBuilder();

        body.append("""
                    <html>
                    <body style="font-family: Arial, sans-serif; background-color: #f4f6f8; padding: 20px;">
                    <div style="max-width:900px; margin:auto; background:#ffffff; border-radius:8px;
                                box-shadow:0 2px 8px rgba(0,0,0,0.1); padding:25px;">
                    <h2 style="color:#004080; text-align:center;">Rapport de suivi des plaintes - GPR</h2>
                """);

        body.append("<p>Bonjour <strong>")
                // .append(claims.get(0).getCollector().getFirstandlastname())
                .append("</strong>,</p>");

        body.append("<p style='margin-top:10px;'>")
                .append(claims.size())
                .append(" Plainte(s) affectée(s) :</p>");

        body.append("<h3 style='color:#d9534f; margin-top:20px;'>Détails des plaintes affectées</h3>");

        body.append("<table style='width:100%; border-collapse: collapse;'>");

        body.append("<tr style='background:#f2f2f2;'>")
                .append("<th style='padding:8px; border:1px solid #ddd;'>Code</th>")
                .append("<th style='padding:8px; border:1px solid #ddd;'>Type</th>")
                .append("<th style='padding:8px; border:1px solid #ddd;'>Affectée le</th>")
                .append("<th style='padding:8px; border:1px solid #ddd;'>Statut</th>")
                .append("<th style='padding:8px; border:1px solid #ddd;'>Retard</th>")
                .append("</tr>");

        for (Claim c : claims) {

            LocalDateTime dateBase = (c.getAffectedAt() != null) ? c.getAffectedAt() : c.getCreatedAt();

            Integer processingTime = (c.getObjet() != null)
                    ? c.getObjet().getProcessingTime()
                    : null;

            long delaiJours = (processingTime != null) ? processingTime : 0;

            LocalDateTime deadline = dateBase.plusDays(delaiJours);
            long diff = java.time.temporal.ChronoUnit.DAYS.between(deadline, LocalDateTime.now());

            body.append("<tr>")
                    .append("<td style='padding:8px; border:1px solid #ddd;'>")
                    .append(displayCode(c)).append("</td>")
                    .append("<td style='padding:8px; border:1px solid #ddd;'>")
                    .append(c.getType().equals(ClaimType.CLAIM) ? "Réclamation" : "Dénonciation").append("</td>")
                    .append("<td style='padding:8px; border:1px solid #ddd;'>")
                    .append(c.getAffectedAt() != null ? c.getAffectedAt().toLocalDate() : "Non renseigné")
                    .append("</td>")
                    .append("<td style='padding:8px; border:1px solid #ddd;'>")
                    .append(getStatusLabel(c.getStatus()))
                    .append("</td>")
                    .append("<td style='padding:8px; border:1px solid #ddd; color:")
                    .append(diff > 0 ? "red" : "green")
                    .append(";'>")
                    .append(diff > 0 ? diff + " jours" : "Dans " + (-diff) + " jours")
                    .append("</td>")
                    .append("</tr>");
        }

        body.append("</table>");

        // ---- FOOTER ----
        body.append("<p style='margin-top:25px; line-height:1.6;'>");
        body.append("Merci de traiter ces plaintes dans les plus brefs délais afin d’assurer un suivi efficace.");
        body.append("</p>");

        body.append("<p style='margin-top:20px;'>Cordialement,<br>");
        body.append("<strong>GPR</strong></p>");

        body.append("<p style='font-size:12px; color:gray; text-align:center; margin-top:30px;'>");
        body.append(" Cet email a été généré automatiquement. Merci de ne pas y répondre.<br>");
        body.append("</p>");

        body.append("</body></html>");

        return body.toString();
    }

    // ===== Envoi aux pilotes =====
    // @return true si le mail est réellement parti (envoi synchrone confirmé).
    private boolean sendToPilotes(List<Claim> affectees, List<Claim> nonAffectees) {
        List<User> pilotes = userRepository.findAll().stream()
                .filter(u -> !u.isDeleted())
                .filter(u -> u.getAdditionalrole() == Role.PILOTE || u.canSeeAll())
                .toList();

        if (pilotes.isEmpty())
            return false;

        String subject = "Alerte - Réclamations en retard";
        String body = buildPiloteMail(affectees, nonAffectees);

        User to = pilotes.get(0);
        String cc = pilotes.stream().map(User::getEmail).collect(Collectors.joining(","));

        try {
            mailService.sendMailSync(to.getEmail(), subject, body, cc);
            return true;
        } catch (Exception e) {
            System.out.println("Erreur envoi pilote (" + to.getEmail() + ") : " + e.getMessage());
            return false;
        }
    }

    private String buildPiloteMail(List<Claim> affectees, List<Claim> nonAffectees) {

        StringBuilder body = new StringBuilder();

        // Début HTML
        body.append("""
                    <html>
                    <body style="font-family: Arial, sans-serif; background-color: #f4f6f8; padding: 20px;">
                    <div style="max-width:900px; margin:auto; background:#ffffff; border-radius:8px;
                                box-shadow:0 2px 8px rgba(0,0,0,0.1); padding:25px;">
                    <h2 style="color:#004080; text-align:center;">Rapport de suivi des plaintes - GPR</h2>
                    <p>Bonjour,</p>
                    <p>Veuillez trouver ci-dessous le résumé des plaintes.</p>
                """);

        /*
         * ============================================================
         * PLAINTES AFFECTÉES
         * ============================================================
         */
        if (!affectees.isEmpty()) {

            body.append("""
                        <h3 style="color:#d9534f;">Plaintes affectées</h3>
                        <p><strong>Total :
                    """);
            body.append(affectees.size());
            body.append("""
                        plainte(s)</strong></p>
                    """);

            // Groupement par agent
            Map<String, List<Claim>> parAgent = affectees.stream()
                    .filter(c -> c.getTreatmentAffectedTo() != null)
                    .collect(Collectors.groupingBy(c -> c.getTreatmentAffectedTo().getEmail()));

            for (var entry : parAgent.entrySet()) {

                String agent = entry.getKey();
                List<Claim> plaintesAgent = entry.getValue();

                body.append("<h4 style='color:#004080;'>Agent : ")
                        .append(agent)
                        .append(" (")
                        .append(plaintesAgent.size())
                        .append(" plainte(s))</h4>");

                // Début tableau
                body.append("""
                            <table style="width:100%; border-collapse: collapse; margin-bottom:20px;">
                            <thead>
                            <tr style="background-color:#f2f2f2;">
                                <th style="border:1px solid #ddd; padding:8px;">Code</th>
                                <th style="border:1px solid #ddd; padding:8px;">Type</th>
                                <th style="border:1px solid #ddd; padding:8px;">Affectée le</th>
                                <th style="border:1px solid #ddd; padding:8px;">Échéance</th>
                                <th style="border:1px solid #ddd; padding:8px;">Statut</th>
                                <th style="border:1px solid #ddd; padding:8px;">Retard</th>
                            </tr>
                            </thead>
                            <tbody>
                        """);

                // Lignes
                for (Claim c : plaintesAgent) {

                    LocalDateTime dateBase = (c.getAffectedAt() != null ? c.getAffectedAt() : c.getCreatedAt());
                    LocalDateTime deadline = dateBase.plusDays(c.getObjet().getProcessingTime());
                    long joursRetard = ChronoUnit.DAYS.between(deadline, LocalDateTime.now());

                    body.append("<tr>");

                    body.append("<td style='border:1px solid #ddd; padding:8px;'>")
                            .append(displayCode(c)).append("</td>");

                    body.append("<td style='border:1px solid #ddd; padding:8px;'>")
                            .append(c.getType().equals(ClaimType.CLAIM) ? "Réclamation" : "Dénonciation")
                            .append("</td>");

                    body.append("<td style='border:1px solid #ddd; padding:8px;'>")
                            .append(dateBase.toLocalDate())
                            .append("</td>");

                    body.append("<td style='border:1px solid #ddd; padding:8px;'>")
                            .append(deadline.toLocalDate())
                            .append("</td>");

                    body.append("<td style='border:1px solid #ddd; padding:8px;'>")
                            .append(getStatusLabel(c.getStatus()))
                            .append("</td>");

                    body.append("<td style='border:1px solid #ddd; padding:8px; color:")
                            .append(joursRetard > 0 ? "#d9534f" : "#5bc0de")
                            .append(";'>")
                            .append(joursRetard > 0
                                    ? joursRetard + " jour(s) de retard"
                                    : "Échéance dans " + (-joursRetard) + " jour(s)")
                            .append("</td>");

                    body.append("</tr>");
                }

                body.append("</tbody></table>");
            }
        }

        /*
         * ============================================================
         * PLAINTES NON AFFECTÉES
         * ============================================================
         */
        if (!nonAffectees.isEmpty()) {

            body.append("""
                        <h3 style="color:#f0ad4e; margin-top:30px;">Plaintes non affectées en retard</h3>
                        <p><strong>Total :
                    """);
            body.append(nonAffectees.size());
            body.append("""
                        plainte(s)</strong></p>
                        <table style="width:100%; border-collapse: collapse; margin-bottom:20px;">
                        <thead>
                        <tr style="background-color:#f9f2e7;">
                            <th style="border:1px solid #ddd; padding:8px;">Code</th>
                            <th style="border:1px solid #ddd; padding:8px;">Type</th>
                            <th style="border:1px solid #ddd; padding:8px;">Reçue le</th>
                            <th style="border:1px solid #ddd; padding:8px;">Échéance</th>
                            <th style="border:1px solid #ddd; padding:8px;">Statut</th>
                            <th style="border:1px solid #ddd; padding:8px;">Retard</th>
                        </tr>
                        </thead>
                        <tbody>
                    """);

            for (Claim c : nonAffectees) {

                LocalDateTime deadline = c.getCreatedAt().plusDays(c.getObjet().getProcessingTime());
                long joursRetard = ChronoUnit.DAYS.between(deadline, LocalDateTime.now());

                body.append("<tr>");

                body.append("<td style='border:1px solid #ddd; padding:8px;'>")
                        .append(displayCode(c)).append("</td>");

                body.append("<td style='border:1px solid #ddd; padding:8px;'>")
                        .append(c.getType().equals(ClaimType.CLAIM) ? "Réclamation" : "Dénonciation")
                        .append("</td>");

                body.append("<td style='border:1px solid #ddd; padding:8px;'>")
                        .append(c.getCreatedAt().toLocalDate())
                        .append("</td>");

                body.append("<td style='border:1px solid #ddd; padding:8px;'>")
                        .append(deadline.toLocalDate())
                        .append("</td>");

                body.append("<td style='border:1px solid #ddd; padding:8px;'>")
                        .append(getStatusLabel(c.getStatus()))
                        .append("</td>");

                body.append("<td style='border:1px solid #ddd; padding:8px; color:")
                        .append(joursRetard > 0 ? "#d9534f" : "#5bc0de")
                        .append(";'>")
                        .append(joursRetard > 0
                                ? joursRetard + " jour(s) de retard"
                                : "Échéance dans " + (-joursRetard) + " jour(s)")
                        .append("</td>");

                body.append("</tr>");
            }

            body.append("""
                        </tbody>
                        </table>
                    """);
        }

        // Footer
        body.append("""
                    <p style="margin-top:30px;">Merci de traiter ces plaintes dans les plus brefs délais.</p>
                    <p>Cordialement,<br><strong>GPR</strong></p>
                    <p style="font-size:12px; color:gray; text-align:center; margin-top:20px;">
                        Cet email a été généré automatiquement. Merci de ne pas y répondre.
                    </p>
                    </div>
                    </body>
                    </html>
                """);

        return body.toString();
    }

    /** Code lisible par les agents (REC-xxxx, DEN-xxxx) ; le code interne seulement à défaut. */
    private String displayCode(Claim claim) {
        return claim.getCodeClient() != null && !claim.getCodeClient().isBlank() ? claim.getCodeClient() : claim.getCode();
    }

    private String getStatusLabel(ClaimStatus status) {
        if (status == null)
            return "-";

        switch (status) {
            case SAVED:
                return "Enregistrée";
            case AFFECTED:
                return "Affectée";
            case TO_APPROUVED:
                return "En attente d'approbation";
            case DESAPPROUVED:
                return "Désapprouvée";
            case TREAT:
                return "Traitée";
            case SATISFIED:
                return "Satisfaite";
            case UNSATISFIED:
                return "Non satisfaite";
            case PARTIAL_SATISFIED:
                return "Partiellement satisfaite";
            case LITIGATION:
                return "Litige";
            case CLASSED:
                return "Classée";
            case TRANSMITTED:
                return "Transmise";
            default:
                return status.toString(); // fallback
        }
    }

}
