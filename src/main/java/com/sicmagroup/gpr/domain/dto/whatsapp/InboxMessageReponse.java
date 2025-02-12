package com.sicmagroup.gpr.domain.dto.whatsapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class InboxMessageReponse {
    private Long id;  
    private String chatId;
	private String senderId;
	private String senderName;
	private String date;
	private String senderPhone;
	private String profile;
	private String type;
	private String content;
	private String message_id;
	private String message_unique_id;
    private String createdAt;
	private String updatedAt;
}
