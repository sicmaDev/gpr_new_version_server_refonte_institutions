package com.sicmagroup.gpr.api.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {
    private String type;
    private String chatId;
    private String body;
    private String notifyName;
    private String from;
    private Boolean fromMe;
    private Boolean isGroupMsg;
    private Long timestamp;
    private Object mediaData;

}
