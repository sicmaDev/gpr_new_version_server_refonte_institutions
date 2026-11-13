package com.sicmagroup.gpr.service.ia;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Mémoire courte des analyses IA faites par le serveur, rangées par numéro d'analyse (correlation_id).
 * À l'enregistrement d'un dossier, le serveur y reprend lui-même le résultat de l'IA au lieu de
 * croire ce que le navigateur envoie. Une analyse n'est rendue qu'à la personne qui l'a demandée.
 * Rien n'est écrit sur disque : la mémoire est vidée au redémarrage.
 */
@Component
public class AnalysesIaRecentes {

    static final Duration DUREE_DE_VIE = Duration.ofHours(6);
    static final int TAILLE_MAX = 5000;

    private record Entree(AnalyseIa analyse, String proprietaire, Instant date) {
    }

    private final Map<String, Entree> analyses = new ConcurrentHashMap<>();

    public void enregistrer(AnalyseIa analyse, String proprietaire) {
        if (analyse == null || analyse.getCorrelationId() == null || proprietaire == null) return;
        analyses.put(analyse.getCorrelationId(), new Entree(analyse, proprietaire, Instant.now()));
        if (analyses.size() > TAILLE_MAX) nettoyer();
    }

    public Optional<AnalyseIa> recuperer(String correlationId, String proprietaire) {
        if (correlationId == null || proprietaire == null) return Optional.empty();
        Entree e = analyses.get(correlationId);
        if (e == null || !e.proprietaire().equals(proprietaire)) return Optional.empty();
        if (e.date().plus(DUREE_DE_VIE).isBefore(Instant.now())) {
            analyses.remove(correlationId);
            return Optional.empty();
        }
        return Optional.of(e.analyse());
    }

    int taille() {
        return analyses.size();
    }

    private synchronized void nettoyer() {
        Instant limite = Instant.now().minus(DUREE_DE_VIE);
        analyses.entrySet().removeIf(x -> x.getValue().date().isBefore(limite));
        int surplus = analyses.size() - TAILLE_MAX;
        if (surplus > 0) {
            analyses.entrySet().stream()
                    .sorted(Comparator.comparing(x -> x.getValue().date()))
                    .limit(surplus)
                    .map(Map.Entry::getKey)
                    .toList()
                    .forEach(analyses::remove);
        }
    }
}
