package com.sicmagroup.gpr.domain.dto.whatsapp;

import java.time.LocalDateTime;
import java.util.List;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class InboxDto {
    private Long id;
	private String code;
	private String phone;
    private Long firstMessage;
	private List<InboxMessageReponse> messages;

    private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
