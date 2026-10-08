package com.sicmagroup.gpr.sla.dto;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.sla.domain.SlaCycleKind;
import com.sicmagroup.gpr.sla.domain.SlaOwnerLevel;
import com.sicmagroup.gpr.sla.domain.SlaPhase;
import com.sicmagroup.gpr.sla.domain.SlaState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bloc « sla » renvoyé avec chaque plainte. Le serveur est la seule source de vérité : le navigateur ne
 * calcule aucune date. Aucune donnée d'identité du client n'y figure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaInfo {
    private SlaState state;
    private SlaPhase phase;
    private boolean paused;
    /** Délai en cours de suivi : PRISE_EN_CHARGE, RESOLUTION ou CLOTURE. */
    private String delay;
    private SlaCycleKind cycleKind;
    private LocalDateTime dueAt;
    private LocalDateTime regulatoryDueAt;
    /** Minutes (ouvrées ou calendaires selon la politique) avant l'échéance ; négatif si dépassée. */
    private Long remainingMinutes;
    /** Minutes d'un jour dans le calcul de cette plainte (jour ouvré ou 1440) : pour afficher « 2 j 3 h ». */
    private Integer minutesPerDay;
    private Integer consumedPct;
    private Long ownerId;
    private String ownerName;
    private SlaOwnerLevel ownerLevel;
    private int escalationLevel;
    private boolean regulatoryBreached;
    private boolean justified;
    private boolean autoEscalated;
    /** Réglage « message d'attente au client » : NONE, MANUAL ou AUTO (réclamations seulement). */
    private String customerMessage;
    /** Nom de la personne chez qui le dossier était avant la dernière remontée automatique (détail d'une plainte). */
    private String escalatedFrom;
    private LocalDateTime escalatedAt;
}
