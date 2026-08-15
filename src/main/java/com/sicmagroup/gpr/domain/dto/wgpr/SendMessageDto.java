package com.sicmagroup.gpr.domain.dto.wgpr;

import lombok.Data;

@Data
public class SendMessageDto {
    private String to;
    private String message;
}