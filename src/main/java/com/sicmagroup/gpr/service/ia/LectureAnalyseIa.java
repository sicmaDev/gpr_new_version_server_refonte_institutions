package com.sicmagroup.gpr.service.ia;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Lit la réponse du Core IA à une analyse (/api/v1/analyze et /api/v1/analyze/stream)
 * et en tire un {@link AnalyseIa}. Mêmes règles de lecture que l'ancienne version GPR (AiApi.js).
 */
public final class LectureAnalyseIa {

    private LectureAnalyseIa() {
    }

    /** Réponse complète en un bloc (JSON). */
    public static Optional<AnalyseIa> depuisJson(String corps, ObjectMapper json) {
        try {
            JsonNode n = json.readTree(corps);
            if (n == null || !n.isObject()) return Optional.empty();
            AnalyseIa a = new AnalyseIa();
            lireBase(n, a);
            lireFinal(n, a, new StringBuilder(), new StringBuilder());
            return Optional.of(a);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Réponse au fil de l'eau (événements « data: {...} » séparés par une ligne vide). */
    public static Optional<AnalyseIa> depuisFlux(String texte, ObjectMapper json) {
        if (texte == null || texte.isBlank()) return Optional.empty();
        AnalyseIa a = new AnalyseIa();
        StringBuilder classification = new StringBuilder();
        StringBuilder urgence = new StringBuilder();
        boolean vu = false;
        for (String bloc : texte.replace("\r\n", "\n").split("\n\n")) {
            String evenement = null;
            StringBuilder donnees = new StringBuilder();
            for (String ligne : bloc.split("\n")) {
                if (ligne.startsWith("event:")) evenement = ligne.substring(6).trim();
                else if (ligne.startsWith("data:")) {
                    if (donnees.length() > 0) donnees.append('\n');
                    donnees.append(ligne.substring(5).trim());
                }
            }
            if (donnees.length() == 0) continue;
            JsonNode n;
            try {
                n = json.readTree(donnees.toString());
            } catch (Exception e) {
                continue;
            }
            if (n == null || !n.isObject()) continue;
            String type = evenement != null ? evenement : texte(n, "type");
            if (type == null) continue;
            vu = true;
            switch (type) {
                case "init_base" -> lireBase(n, a);
                case "reasoning", "raisonnement_chunk" -> classification.append(premier(n, "delta", "content"));
                case "raisonnement_urgence_chunk" -> urgence.append(premier(n, "delta", "content"));
                case "init_urgence" -> {
                    String u = texte(n, "urgence");
                    if (u != null) a.setUrgence(u);
                }
                case "final" -> {
                    JsonNode f = n.has("data") && n.get("data").isObject() ? n.get("data") : n;
                    lireBase(f, a);
                    lireFinal(f, a, classification, urgence);
                }
                default -> {
                    // autres événements (delta d'affichage, done…) : rien à garder
                }
            }
        }
        if (!vu) return Optional.empty();
        if (a.getRaisonnementClassification() == null && classification.length() > 0)
            a.setRaisonnementClassification(classification.toString().trim());
        if (a.getRaisonnementUrgence() == null && urgence.length() > 0)
            a.setRaisonnementUrgence(urgence.toString().trim());
        return Optional.of(a);
    }

    private static void lireBase(JsonNode n, AnalyseIa a) {
        String resume = texte(n, "resume");
        if (resume != null) a.setResume(resume);
        String sentiment = texte(n, "sentiment");
        if (sentiment != null) a.setSentiment(sentiment);
        JsonNode mots = n.get("mots_cles_detectes");
        if (mots != null && mots.isArray()) {
            a.getMotsCles().clear();
            mots.forEach(m -> {
                if (m.isTextual() && !m.asText().isBlank()) a.getMotsCles().add(m.asText());
            });
        }
    }

    private static void lireFinal(JsonNode f, AnalyseIa a, StringBuilder classification, StringBuilder urgence) {
        JsonNode r = f.has("result") && f.get("result").isObject() ? f.get("result") : null;
        String categorie = premierNonVide(texte(f, "categorie"), texte(f, "categorie_suggeree"), r == null ? null : texte(r, "category"));
        if (categorie != null) a.setCategorie(categorie);
        String motif = premierNonVide(texte(f, "motif"), texte(f, "motif_suggere"), r == null ? null : texte(r, "motif"));
        if (motif != null) a.setMotif(motif);
        String u = texte(f, "urgence");
        if (u != null) a.setUrgence(u);
        String raisonnement = premierNonVide(texte(f, "raisonnement"), r == null ? null : texte(r, "raisonnement"));
        a.setRaisonnementClassification(raisonnement != null ? raisonnement
                : classification.length() > 0 ? classification.toString().trim() : a.getRaisonnementClassification());
        String raisonnementUrgence = texte(f, "raisonnement_urgence");
        a.setRaisonnementUrgence(raisonnementUrgence != null ? raisonnementUrgence
                : urgence.length() > 0 ? urgence.toString().trim() : a.getRaisonnementUrgence());
        String correlation = texte(f, "correlation_id");
        if (correlation != null) a.setCorrelationId(correlation);
    }

    private static String texte(JsonNode n, String champ) {
        JsonNode v = n.get(champ);
        if (v == null || v.isNull()) return null;
        String s = v.isTextual() ? v.asText() : v.toString();
        return s.isBlank() ? null : s;
    }

    private static String premier(JsonNode n, String... champs) {
        for (String c : champs) {
            JsonNode v = n.get(c);
            if (v != null && v.isTextual()) return v.asText();
        }
        return "";
    }

    private static String premierNonVide(String... valeurs) {
        for (String v : valeurs) if (v != null && !v.isBlank()) return v;
        return null;
    }
}
