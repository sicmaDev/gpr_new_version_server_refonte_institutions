package com.sicmagroup.gpr.domain.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
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
@Table(name = "gps_product")
public class Product {
    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String libelle;
	@Lob
    @Column(columnDefinition = "TEXT")
	private String description;
    private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;
	@Column(unique = true)
	private String uuid;
	@Column(columnDefinition = "boolean default false")
	private boolean isDeleted;

	@OneToMany(mappedBy = "product")
    private List<Claim> claims;

	@OneToMany(mappedBy = "produit")
    private List<Suggestion> suggestions;
}
