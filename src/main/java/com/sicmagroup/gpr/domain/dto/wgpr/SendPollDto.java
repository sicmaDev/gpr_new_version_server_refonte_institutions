package com.sicmagroup.gpr.domain.dto.wgpr;

import lombok.Data;

import java.util.List;

@Data
public class SendPollDto {
    private String       to;
    private String       question;
    private List<String> options;
}