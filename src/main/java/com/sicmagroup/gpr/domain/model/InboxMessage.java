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

	private String chat_id;
	private String sender_id;
	private String sender_name;
	private String date;
	private String sender_phone;
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
