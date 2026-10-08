package com.sicmagroup.gpr.sla.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;

/**
 * Textes des e-mails et SMS du SLA. Règle absolue : aucun nom, téléphone, e-mail, adresse ni code client
 * du plaignant. Seuls figurent le code de la plainte, l'objet, l'agence et les échéances.
 */
public final class SlaMessages {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private SlaMessages() {
    }

    /** Une plainte telle qu'elle apparaît dans un message. */
    public record Line(ClaimType type, String code, String objet, String agence, LocalDateTime dueAt, String detail) {
    }

    public static String typeLabel(ClaimType type) {
        return switch (type) {
            case CLAIM -> "Réclamation";
            case DENUNCIACION -> "Dénonciation";
            default -> "Suggestion";
        };
    }

    public static String date(LocalDateTime d) {
        return d == null ? "-" : FMT.format(d);
    }

    private static String esc(String s) {
        return s == null ? "-" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String page(String title, String intro, String table, String outro) {
        StringBuilder b = new StringBuilder();
        b.append("<html><body style='font-family: Arial, sans-serif; background-color: #f4f6f8; padding: 20px;'>");
        b.append("<div style='max-width: 760px; margin: auto; background: #ffffff; border-radius: 10px;");
        b.append("box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 25px;'>");
        b.append("<h2 style='color: #004080; text-align: center;'>").append(esc(title)).append("</h2>");
        b.append("<p>").append(intro).append("</p>");
        b.append(table);
        if (outro != null && !outro.isBlank()) {
            b.append("<p style='margin-top:20px;'>").append(outro).append("</p>");
        }
        b.append("<p style='margin-top:20px;'>Cordialement,<br><strong>L'équipe GPR</strong></p>");
        b.append("<p style='font-size:12px; color:gray; text-align:center; margin-top:25px;'>");
        b.append("Message généré automatiquement par la plateforme GPR (suivi des délais). Merci de ne pas y répondre.");
        b.append("</p></div></body></html>");
        return b.toString();
    }

    private static String table(List<Line> lines, String lastHeader) {
        StringBuilder b = new StringBuilder();
        b.append("<table style='width:100%; border-collapse:collapse; font-size:14px; margin-top:10px;'>");
        b.append("<thead><tr style='background:#f2f2f2; text-align:left;'>");
        for (String h : new String[] { "Plainte", "Type", "Objet", "Agence", "Échéance", lastHeader }) {
            b.append("<th style='padding:8px; border:1px solid #ddd;'>").append(h).append("</th>");
        }
        b.append("</tr></thead><tbody>");
        for (Line l : lines) {
            b.append("<tr>");
            b.append(cell(l.code())).append(cell(typeLabel(l.type()))).append(cell(l.objet()))
                    .append(cell(l.agence())).append(cell(date(l.dueAt()))).append(cell(l.detail()));
            b.append("</tr>");
        }
        b.append("</tbody></table>");
        return b.toString();
    }

    private static String cell(String v) {
        return "<td style='padding:8px; border:1px solid #ddd;'>" + esc(v) + "</td>";
    }

    public static String subjectReminder(int pct) {
        return "GPR - Rappel : " + pct + " % du délai écoulé";
    }

    public static String reminder(String greeting, Line line, int pct) {
        return page("Rappel de délai - GPR",
                greeting + ", " + pct + " % du délai de traitement de la plainte suivante est écoulé.",
                table(List.of(line), "Avancement"), "Merci de la traiter avant l'échéance.");
    }

    public static String breach(String greeting, Line line) {
        return page("Délai dépassé - GPR", greeting + ", le délai de la plainte suivante est dépassé.",
                table(List.of(line), "Situation"),
                "Sans action de votre part, le dossier sera remonté automatiquement au niveau supérieur.");
    }

    public static String takeoverBreach(String greeting, Line line) {
        return page("Plainte non prise en charge - GPR",
                greeting + ", la plainte suivante n'a pas encore été prise en charge dans le délai prévu.",
                table(List.of(line), "Situation"), "Merci de l'affecter ou de la traiter.");
    }

    public static String escalation(String greeting, Line line, String from, String level, int unanswered) {
        return page("Plainte remontée automatiquement - GPR",
                greeting + ", la plainte suivante vous a été transmise automatiquement (niveau " + esc(level)
                        + ") : son délai est dépassé et les alertes envoyées à " + esc(from) + " (" + unanswered
                        + " alerte(s)) sont restées sans action.",
                table(List.of(line), "Situation"), "Merci de la traiter ou de la réaffecter.");
    }

    public static String stuck(String greeting, List<Line> lines) {
        return page("Dossiers bloqués au dernier niveau - GPR",
                greeting + ", les plaintes suivantes sont en retard et ont atteint le dernier niveau de remontée.",
                table(lines, "Situation"), null);
    }

    public static String regulatoryWarning(String greeting, Line line, int days) {
        return page("Limite réglementaire proche - GPR",
                greeting + ", la limite réglementaire de la plainte suivante est dans moins de " + days
                        + " jour(s).",
                table(List.of(line), "Limite réglementaire"), "Merci de la traiter en priorité.");
    }

    public static String regulatoryBreach(String greeting, Line line) {
        return page("Hors délai réglementaire - GPR",
                greeting + ", la limite réglementaire de la plainte suivante est dépassée.",
                table(List.of(line), "Limite réglementaire"),
                "La plainte est désormais marquée « Hors délai réglementaire » dans les rapports.");
    }

    public static String digest(String greeting, List<Line> lines) {
        return page("Récapitulatif quotidien des délais - GPR",
                greeting + ", voici les plaintes dont un seuil de délai a été atteint.",
                table(lines, "Avancement"), "Merci de les traiter avant leur échéance.");
    }

    /** Message d'attente au client : seulement sa référence, jamais d'information interne. */
    public static String clientWaiting(String reference) {
        return "Madame, Monsieur, nous accusons réception de votre réclamation (réf. " + reference
                + "). Son traitement prend plus de temps que prévu : nous vous prions de nous en excuser et vous"
                + " tiendrons informé(e) dès que possible.";
    }

    public static String sms(String kind, Line line) {
        return "GPR : " + typeLabel(line.type()) + " " + line.code() + " - " + kind + " (échéance "
                + date(line.dueAt()) + ").";
    }
}
