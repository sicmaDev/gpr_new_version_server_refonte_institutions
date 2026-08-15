package com.sicmagroup.gpr.service.wgpr;

import com.sicmagroup.gpr.domain.dto.wgpr.MessageResponseDto;

import java.util.List;

public interface WgprComplaintService {

    void markMessageRead(Long messageId);

    void markMessagesRead(List<Long> ids);

    List<MessageResponseDto> getAllMessages();

    void markMessagesAsConverted(List<Long> ids);
}
