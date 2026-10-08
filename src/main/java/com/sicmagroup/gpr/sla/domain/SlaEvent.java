package com.sicmagroup.gpr.sla.domain;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registre technique des alertes et remontées. La clé de déduplication est unique : une alerte donnée ne
 * peut être enregistrée (donc envoyée) qu'une seule fois, même si deux instances du serveur tournent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gps_sla_event", uniqueConstraints = @UniqueConstraint(name = "uk_sla_event_dedup", columnNames = "dedup_key"), indexes = {
        @Index(name = "idx_sla_event_digest", columnList = "digest_pending,recipient_user_id") })
public class SlaEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long claimSlaId;
    private Long claimId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ClaimType targetType;

    @Column(length = 40)
    private String eventType;

    @Column(name = "dedup_key", length = 120)
    private String dedupKey;

    /** Destinataire de la notification (une ligne par destinataire pour le récapitulatif). */
    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Column(length = 500)
    private String detail;

    @Column(name = "digest_pending")
    private boolean digestPending;
    private LocalDateTime digestSentAt;

    private LocalDateTime createdAt;
}
