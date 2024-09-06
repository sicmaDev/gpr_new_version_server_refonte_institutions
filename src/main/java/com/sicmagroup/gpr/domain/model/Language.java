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
@Table(name = "gps_language",uniqueConstraints = {
	@UniqueConstraint(
		name="libelle_unique",
		columnNames = "libelle"
	)
})
public class Language {
    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(unique = true)
	private String libelle;
	@Lob
	@Column(columnDefinition = "TEXT")
	private String description;
    private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;
	@Column(columnDefinition = "boolean default false")
	private boolean isDeleted;   

	@OneToMany(mappedBy = "language")
    private List<Claim> claims;

	@OneToMany(mappedBy = "langue")
    private List<Suggestion> suggestions;
}
