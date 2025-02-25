package com.sicmagroup.gpr.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sicmagroup.gpr.domain.enumeration.GravityLevel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gps_objet",uniqueConstraints = {
	@UniqueConstraint(
		name="libelle_unique_objet",
		columnNames = "libelle"
	)
})
public class Objet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true,name = "libelle")
    private String libelle;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    private GravityLevel risqueLevel;
    private int processingTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    @Column(columnDefinition = "boolean default false")
    private boolean isDeleted;
    @OneToMany(mappedBy = "objet")
    private List<Claim> claims;

    @OneToMany(mappedBy = "objet")
    private List<ExistingSolution> existingSolutions;

    @ManyToOne
    private CategorieObjet categorie;
     
    @Column(nullable = false, unique = true,updatable = false)
	private String uuid;
    @PrePersist
    public void generateUuidIfNull() {
        if (this.uuid == null) {
            this.uuid = "obj-" + UUID.randomUUID().toString().substring(0, 5);
        }
    }
    
}
