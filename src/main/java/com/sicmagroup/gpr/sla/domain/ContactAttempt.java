package com.sicmagroup.gpr.sla.domain;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.converter.EncryptedStringConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Tentative de contact du client pour mesurer sa satisfaction (réclamations traitées). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gps_contact_attempt", indexes = @Index(name = "idx_contact_claim", columnList = "claim_id"))
public class ContactAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id")
    private Long claimId;

    /** APPEL, SMS, WHATSAPP, EMAIL ou VISITE. */
    @Column(length = 20)
    private String channel;

    private boolean reached;

    @Lob
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "MEDIUMTEXT")
    private String comment;

    private Long userId;
    private LocalDateTime createdAt;
}
