package com.sicmagroup.gpr.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDateTime;
import java.util.List;

import com.sicmagroup.gpr.domain.enumeration.ServicePointEnum;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
@Entity
@Table(name = "gps_service_point")
public class ServicePoint {


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(unique = true)
	private String uuid;
	@Column(unique = true)
	private String libelle;
	@Lob
    @Column(columnDefinition = "TEXT")
	private String description;
	@Enumerated(EnumType.STRING)
	private ServicePointEnum type;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;
	@Column(columnDefinition = "boolean default false")
	private boolean isPrincipalAgence;
	@Column(columnDefinition = "boolean default false")
	private boolean isDeleted;

	@OneToMany(mappedBy = "servicePoint")
	private List<User> users;

	@OneToMany(mappedBy = "servicePoint")
    private List<Claim> claims;

	@OneToMany(mappedBy = "serviceIndexe")
	private List<Suggestion> suggestions;

	
}