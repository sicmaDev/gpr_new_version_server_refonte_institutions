package com.sicmagroup.gpr.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

import com.sicmagroup.gpr.domain.converter.JsonMapConverter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gps_rapport_template")
public class Template {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
     @Column(columnDefinition = "json")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> valeurs;

    private String title;

    private boolean isDefault;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Auteur du modèle (identifiant et nom seulement : l'entité User n'est jamais renvoyée au navigateur).
    // Null pour les modèles créés avant cette règle : ils ne sont visibles que par Pilote, DE et H14.
    private Long authorId;

    private String authorName;

    // Choix d'un RA : modèle visible par les agents de son point de service et des points rattachés.
    @Column(columnDefinition = "boolean default false")
    private Boolean sharedWithAgents;

    // Calculé pour l'utilisateur connecté (modifier / supprimer autorisé), jamais enregistré.
    @Transient
    private Boolean canEdit;
}
