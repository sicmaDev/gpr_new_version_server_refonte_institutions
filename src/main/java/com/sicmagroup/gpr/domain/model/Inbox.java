package com.sicmagroup.gpr.domain.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "gps_inboxs")
// Désérialisé directement depuis le JSON du frontend (ClaimRequest.inboxWhatsapp) — voir
// InboxMessage pour le détail du problème que ça pose sans ignoreUnknown.
@JsonIgnoreProperties(ignoreUnknown = true)
public class Inbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @Column(unique = true)
	private String code;
	private String phone;
	@Column(name = "first_message")
	private Long firstMessage;

	@OneToMany(mappedBy = "inbox", cascade = CascadeType.ALL,orphanRemoval = true)
	private List<InboxMessage> messages;



    private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
