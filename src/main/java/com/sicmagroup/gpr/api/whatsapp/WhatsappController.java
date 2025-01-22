package com.sicmagroup.gpr.api.whatsapp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.api.claim.ClaimController;
import com.sicmagroup.gpr.api.denunciation.DenunciationController;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.dto.whatsapp.InboxDto;
import com.sicmagroup.gpr.domain.dto.whatsapp.InboxMessageReponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.model.Inbox;
import com.sicmagroup.gpr.domain.model.InboxMessage;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.domain.model.chat.Chat;
import com.sicmagroup.gpr.repository.InboxMessageRepository;
import com.sicmagroup.gpr.repository.InboxRepository;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.botkey.BotKeyServiceImpl;
import com.sicmagroup.gpr.service.media.MediaServiceImpl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/whatsapp")
@RequiredArgsConstructor
public class WhatsappController {

   
    private final InboxMessageRepository inboxMessageRepository;
    private final InboxRepository inboxRepository;
     private final ModelMapper modelMapper;

    @GetMapping(value = "/list")
    public ResponseEntity<ApiResponseDto> getAll() {
        ApiResponseDto apiResponseDto = ApiResponseDto.builder().build();
        List<Inbox> inboxs = inboxRepository.findAll();

        apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(inboxs.stream().map(this::convertToDto).collect(Collectors.toList()))
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }
    @GetMapping(value = "/messages/{id}")
    public ResponseEntity<ApiResponseDto> getAllMessages(@PathVariable Long id) {
        ApiResponseDto apiResponseDto = ApiResponseDto.builder().build();
        List<InboxMessage> messages = inboxMessageRepository.findByInboxId(id);

        apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(messages)
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }

     private InboxDto convertToDto(Inbox inbox) {
        InboxDto inboxDto = modelMapper.map(inbox, InboxDto.class);
        if(inbox.getMessages() != null && !inbox.getMessages().isEmpty()){
            inboxDto.setMessages(inbox.getMessages().stream().map(this::convertToResponse).collect(Collectors.toList()));
        } 

        
        return inboxDto;
    }
    private InboxMessageReponse convertToResponse(InboxMessage inboxMessage) {
        InboxMessageReponse inboxMessageResponse = modelMapper.map(inboxMessage, InboxMessageReponse.class);
        return inboxMessageResponse;
    };

    
}
