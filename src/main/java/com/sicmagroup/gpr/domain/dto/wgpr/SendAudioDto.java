package com.sicmagroup.gpr.domain.dto.wgpr;

import lombok.Data;

@Data
public class SendAudioDto {
    private String to;
    private String audioBase64;
    private String mimeType;
}
