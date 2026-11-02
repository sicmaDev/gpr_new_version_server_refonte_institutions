package com.sicmagroup.gpr.domain.model;

import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Localisation d'un point de service pour la cartographie du rapport Global.
// Même table que ServicePoint (gps_service_point) : cette entité ne porte que les 4 colonnes ajoutées
// (ddl-auto=update les crée) et ne sert qu'à les lire et les mettre à jour. ServicePoint n'est pas modifié,
// donc ses enregistrements existants ne touchent jamais à ces colonnes. Jamais d'insertion par cette entité.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicUpdate
@Table(name = "gps_service_point")
public class ServicePointLocalisation {
	@Id
	private Long id;
	@Column(insertable = false, updatable = false)
	private String libelle;
	// Même nom logique que ServicePoint.isDeleted : Hibernate refuse deux noms pour la même colonne.
	@Column(insertable = false, updatable = false)
	private boolean isDeleted;
	private String ville;
	private String quartier;
	private Double latitude;
	private Double longitude;
}
