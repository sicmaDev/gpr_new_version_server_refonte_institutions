package com.sicmagroup.gpr.domain.model;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.enumeration.MessageChannel;
import com.sicmagroup.gpr.domain.enumeration.SmsStatus;
import com.sicmagroup.gpr.domain.enumeration.SmsType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Archive d'un SMS envoyé à un client. Le numéro et le texte sont des données personnelles : ils sont chiffrés
 * en base comme les autres colonnes sensibles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gps_sms_log")
public class SmsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Canal d'envoi : SMS, MAIL ou WHATSAPP. Vide pour les anciennes lignes (tous des SMS).
    @Enumerated(EnumType.STRING)
    @Column(name = "channel")
    private MessageChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SmsType type;

    @Column(name = "claim_id")
    private Long claimId;

    @Column(name = "suggest_id")
    private Long suggestId;

    @Column(name = "numero_destinataire", nullable = false, columnDefinition = "TEXT")
    private String numeroDestinataire;

    @Lob
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SmsStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "send_at")
    private LocalDateTime sendAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", columnDefinition = "boolean default false")
    private boolean isDeleted;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
