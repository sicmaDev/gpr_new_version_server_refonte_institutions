package com.sicmagroup.gpr.domain.model;

import com.sicmagroup.gpr.domain.converter.EncryptedStringConverter;
import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
@Entity
@Table(name = "gps_historique_affectations")
public class HistoriqueAffectation {
    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
    private Long reclamationId;
    private String codePlainte;
    @Enumerated(EnumType.STRING)
    private ClaimType typePlainte;
    private String emailAgent;
    private String nomAgent;
    private String telephoneAgent;
    private LocalDateTime dateAffectation;
    private Integer delaiJours; 
    private LocalDateTime dateFinAffectation;
    @Lob
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "MEDIUMTEXT")
	private String contentMail;
    @Column(columnDefinition = "boolean default false") 
    private Boolean mailEnvoye;
    @Column(columnDefinition = "boolean default false")
    private Boolean smsEnvoye;
    private LocalDateTime lastNotification;  // Dernière relance
    private Long affecteurId;  
    private String emailAffecteur;
    private String nomAffecteur;
    private Long affectationPrecedenteId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
