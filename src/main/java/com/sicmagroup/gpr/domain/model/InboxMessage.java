package com.sicmagroup.gpr.domain.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data 
@AllArgsConstructor 
@NoArgsConstructor
@Builder
@Table(name = "gps_inbox_messages")
public class InboxMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String chatId;
	private String senderId;
	private String senderName;
	private String date;
	private String senderPhone;
	
	@ManyToOne
	@JoinColumn(name="inbox_id",nullable = true)
	private Inbox inbox;

	

	@Lob
    @Column(nullable = true)
	private String profile;

	@Column(columnDefinition = "varchar(255) default 'chat'")
	private String type;

    @Lob
    @Column(columnDefinition = "TEXT")
	private String content;
	private String message_id;

	@Lob
    @Column(columnDefinition = "TEXT",nullable = true)
	private String message_unique_id;

    private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
