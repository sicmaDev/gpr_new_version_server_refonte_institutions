package com.sicmagroup.gpr.service.ia;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Résultat d'une analyse IA d'un texte de plainte, tel que renvoyé par le Core IA
 * (valeurs brutes du Core : urgence FAIBLE/MOYEN/GRAVE, sentiment negatif/neutre/positif…).
 * Gardé par le serveur pour être enregistré avec le dossier, sans passer par le navigateur.
 */
@Data
public class AnalyseIa {
    private String correlationId;
    private String resume;
    private String sentiment;
    private List<String> motsCles = new ArrayList<>();
    private String urgence;
    private String categorie;
    private String motif;
    private String raisonnementClassification;
    private String raisonnementUrgence;

    /** Vrai si le Core a renvoyé au moins une catégorie ou un motif (analyse arrivée au bout). */
    public boolean estComplete() {
        return (categorie != null && !categorie.isBlank()) || (motif != null && !motif.isBlank());
    }
}
