package com.sicmagroup.gpr.sla.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Motif de retard (ou de clôture « non mesurée »), configurable. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gps_sla_breach_reason")
public class SlaBreachReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String libelle;

    /** RETARD (justification d'un retard) ou NON_MESURE (clôture sans mesure de satisfaction). */
    @Column(length = 20)
    private String kind;

    private boolean active;
}
