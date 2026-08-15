package com.sicmagroup.gpr.domain.dto.wgpr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageResponseDto {

    private Long id;

    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("from_number")
    private String fromNumber;

    @JsonProperty("from_name")
    private String fromName;

    /** Contenu texte du message */
    private String content;

    /** Type WhatsApp : chat | image | video | audio | ptt | document | sticker */
    private String type;

    @JsonProperty("media_path")
    private String mediaPath;

    private Long timestamp;

    private boolean read;

    /** true si le message a été envoyé par le bot (wa_message_id commence par "true_") */
    private boolean sent;

    private String status;

    @JsonProperty("complaint_id")
    private Long complaintId;

    @JsonProperty("created_at")
    private String createdAt;
}
