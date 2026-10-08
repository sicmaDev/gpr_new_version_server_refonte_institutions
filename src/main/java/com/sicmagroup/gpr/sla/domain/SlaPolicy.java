package com.sicmagroup.gpr.sla.domain;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.GravityLevel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Politique de délais par type de plainte et niveau de risque.
 * Les durées sont en minutes. En mode « ouvré », ce sont des minutes ouvrées (jour de travail du calendrier),
 * sinon des minutes calendaires.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gps_sla_policy", uniqueConstraints = @UniqueConstraint(columnNames = { "claim_type", "risk_level" }))
public class SlaPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type")
    private ClaimType claimType;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private GravityLevel riskLevel;

    @Column(name = "business_time")
    private boolean businessTime;

    /** Délai de prise en charge (première action après l'enregistrement). */
    private int takeoverMinutes;
    /** Délai de résolution (réception -> solution approuvée). */
    private int resolutionMinutes;
    /** Délai de clôture (solution approuvée -> mesure de satisfaction). Ignoré pour les dénonciations. */
    private int closureMinutes;
    /** Délai d'un nouveau cycle après une mesure « non satisfait » ou « partiellement satisfait ». */
    private int reopenMinutes;

    private int reminder1Pct;
    private int reminder2Pct;
    /** Délai de grâce avant la remontée automatique, après le dépassement. */
    private int graceMinutes;
    /** Objectif de conformité (pourcentage de plaintes dans les délais), pour les rapports. */
    private int complianceTarget;

    private boolean active;
}
