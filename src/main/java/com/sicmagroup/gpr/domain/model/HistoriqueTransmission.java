package com.sicmagroup.gpr.domain.model;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;

import jakarta.persistence.Column;
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
@Table(name = "gps_historique_transmissions")
public class HistoriqueTransmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long claimId;
    private String codePlainte;

    @Enumerated(EnumType.STRING)
    private ClaimType typePlainte;

    private Long transmisParId;
    private String transmisParNom;

    private Long transmisAId;
    private String transmisANom;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String commentaire;

    private LocalDateTime dateTransmission;
}
