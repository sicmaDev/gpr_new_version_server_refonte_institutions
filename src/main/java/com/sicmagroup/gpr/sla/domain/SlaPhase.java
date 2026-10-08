package com.sicmagroup.gpr.sla.domain;

/** Phase stockée du compteur SLA. L'état affiché (SlaState) en est déduit au moment de la lecture. */
public enum SlaPhase {
    /** Le chronomètre tourne. */
    OPEN,
    /** En attente du client : le délai interne est en pause (l'échéance réglementaire continue). */
    PAUSED,
    /** Plainte classée ou en contentieux : hors indicateurs. */
    SUSPENDED,
    /** Cycle terminé (résolu et, pour une réclamation, mesuré). */
    DONE,
    /** Compteur remplacé (conversion réclamation <-> dénonciation) ou plainte supprimée. */
    CANCELLED
}
