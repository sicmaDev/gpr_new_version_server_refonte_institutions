package com.sicmagroup.gpr.sla.domain;

/** État affiché d'une plainte vis-à-vis du SLA. */
public enum SlaState {
    EN_COURS,
    A_RISQUE,
    DEPASSE,
    SUSPENDU,
    RESPECTE,
    /** Échéance réglementaire dépassée : définitif. */
    HORS_DELAI,
    ANNULE
}
