package com.sicmagroup.gpr.sla.domain;

public enum SlaCycleKind {
    /** Cycle principal : de la réception à la solution approuvée, puis à la mesure de satisfaction. */
    MAIN,
    /** Nouveau cycle ouvert après une mesure « non satisfait » ou « partiellement satisfait ». */
    SATISFACTION
}
