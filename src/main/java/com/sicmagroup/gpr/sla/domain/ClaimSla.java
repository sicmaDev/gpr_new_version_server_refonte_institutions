package com.sicmagroup.gpr.sla.domain;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.converter.EncryptedStringConverter;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Compteur SLA d'une plainte (un par plainte et par cycle). Il ne contient jamais de donnée d'identité du
 * client : uniquement des identifiants, des dates et des états. Les échéances sont figées à l'ouverture.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gps_claim_sla", indexes = {
        @Index(name = "idx_claim_sla_target", columnList = "target_type,claim_id"),
        @Index(name = "idx_claim_sla_check", columnList = "phase,next_check_at"),
        // tri des listes par échéance. Volontairement SANS claim_deleted ni phase en tête : un index qui commence
        // par une colonne presque toujours identique pousse la base à lire les lignes une par une pour les totaux
        // (mesuré : 4,2 s au lieu de 0,7 s sur 50 000 compteurs).
        @Index(name = "idx_claim_sla_due_at", columnList = "due_at") })
public class ClaimSla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20)
    private ClaimType targetType;

    @Column(name = "claim_id")
    private Long claimId;

    private int cycle;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle_kind", length = 20)
    private SlaCycleKind cycleKind;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SlaPhase phase;

    // --- Dates de référence et échéances (figées à l'ouverture) ---
    private LocalDateTime receivedAt;
    private LocalDateTime registeredAt;

    private LocalDateTime takeoverDueAt;
    private LocalDateTime takeoverAt;

    @Column(name = "resolution_due_at")
    private LocalDateTime resolutionDueAt;
    private LocalDateTime resolvedAt;

    private LocalDateTime closureDueAt;
    private LocalDateTime closedAt;

    private LocalDateTime regulatoryDueAt;
    private boolean regulatoryBreached;

    // --- Durées utilisées pour cet enregistrement (copiées de la politique, pour les rappels) ---
    private int resolutionMinutes;
    private int closureMinutes;
    private int reminder1Pct;
    private int reminder2Pct;
    private int graceMinutes;
    private boolean businessTime;

    // --- Pause « en attente du client » ---
    private LocalDateTime pausedAt;
    private long pausedMinutes;

    // --- Responsable actuel ---
    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SlaOwnerLevel ownerLevel;

    private int escalationCount;
    private LocalDateTime lastHumanActionAt;
    private LocalDateTime nextEscalationAt;

    // --- Compteurs d'alertes (une alerte n'est envoyée qu'une seule fois) ---
    private boolean takeoverBreachNotified;
    private boolean reminder1Sent;
    private boolean reminder2Sent;
    private boolean breachNotified;
    private LocalDateTime breachNotifiedAt;
    private boolean regulatoryWarningSent;
    private boolean regulatoryBreachSent;
    private boolean stuckNotified;

    @Column(name = "next_check_at")
    private LocalDateTime nextCheckAt;

    // --- Copie de données de la plainte : les listes et indicateurs ne lisent ainsi que cette table ---
    /** Échéance en vigueur (résolution, puis clôture) : recalculée à chaque changement. */
    @Column(name = "due_at")
    private LocalDateTime dueAt;
    @Column(name = "service_point_id")
    private Long servicePointId;
    @Column(name = "collector_id")
    private Long collectorId;
    @Column(name = "affected_to_id")
    private Long affectedToId;
    @Column(name = "transmitted_to_id")
    private Long transmittedToId;
    /** La plainte est supprimée (suppression douce) : hors listes et indicateurs. */
    @Column(name = "claim_deleted")
    private boolean claimDeleted;

    // --- Justification d'un retard ---
    private Long breachReasonId;
    @Lob
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "MEDIUMTEXT")
    private String breachComment;
    private Long justifiedById;
    private LocalDateTime justifiedAt;

    /** Clôture « client injoignable » (ni satisfait ni insatisfait). */
    private boolean closedUnreachable;

    @Version
    private Long version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
