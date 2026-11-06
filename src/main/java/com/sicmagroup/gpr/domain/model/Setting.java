package com.sicmagroup.gpr.domain.model;

import com.sicmagroup.gpr.utils.Constante;
import com.sicmagroup.gpr.utils.crypto.FieldEncryptor;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.ToString;


@Data 
@AllArgsConstructor 
@NoArgsConstructor
@Builder
@Entity
@Table(name = "gps_setting")
public class Setting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String libelle;

    // Réglages mail et SMS chiffrés en base (mots de passe que l'application doit relire) ; les autres en clair.
    @Lob
    @ToString.Exclude
    @Column(columnDefinition = "MEDIUMTEXT")
    private String value;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getValue() {
        return FieldEncryptor.current().decrypt(value);
    }

    @PrePersist
    @PreUpdate
    void chiffrerSiMailOuSms() {
        boolean sensible = Constante.MAIL_SLUG.equals(libelle) || Constante.SMS_SLUG.equals(libelle);
        if (sensible && !FieldEncryptor.isEncrypted(value)) {
            value = FieldEncryptor.current().encrypt(value);
        }
    }
}