package com.sicmagroup.gpr.api.webhook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.api.claim.ClaimController;
import com.sicmagroup.gpr.api.claim.ClaimRequest;
import com.sicmagroup.gpr.api.claim.SaveRequest;
import com.sicmagroup.gpr.api.denunciation.DenunciationController;
import com.sicmagroup.gpr.api.suggestion.SuggestionAddRequest;
import com.sicmagroup.gpr.api.suggestion.SuggestionRequest;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ClaimDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.dto.LicenceControl;
import com.sicmagroup.gpr.domain.dto.SuggestionDto;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.ConfigExportEnum;
import com.sicmagroup.gpr.domain.model.Inbox;
import com.sicmagroup.gpr.domain.model.InboxMessage;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.chat.Chat;
import com.sicmagroup.gpr.repository.InboxMessageRepository;
import com.sicmagroup.gpr.repository.InboxRepository;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.botkey.BotKeyServiceImpl;
import com.sicmagroup.gpr.service.media.MediaServiceImpl;
import com.sicmagroup.gpr.utils.Utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
public class WebhookController {
    private final BotKeyServiceImpl service;
    private final AuthenticationServiceImpl authService;
    private final DenunciationController denunciationController;
    private final ClaimController claimController;
    private final InboxMessageRepository inboxMessageRepository;
    private final InboxRepository inboxRepository;
    private final MediaServiceImpl mediaServiceImpl;

    @PostMapping("/save")
    public ResponseEntity<ApiResponseDto> saveMessage(HttpServletRequest request,
            @RequestBody Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        ApiResponseDto apiResponseDto;
        try {
            System.out.println("SAVE");

            // Boolean isAuth = service.checkApiKeyBoolean(request);
            // if (isAuth == false) {
            // throw new Exception("Vous n'etes pas authentifier");
            // }

            if (data != null && !data.get("from").equals("status@broadcast")) {
                String event = (String) data.get("event");
                boolean isGroupMsg = (boolean) data.get("isGroupMsg");
                InboxMessage messageArray = null;
                System.out.println(">>>>>");
                System.out.println((String) data.get("event"));
                System.out.println(">>>>>");

                if ("onmessage".equals(event) && !isGroupMsg) {
                    messageArray = formatData(data, (String) data.get("body"), (String) data.get("sender.pushname"));
                } else if ("onselfmessage".equals(event)) {

                    System.out.println((String) data.get("notifyName"));
                    System.out.println(">>>>>");
                    messageArray = formatData(data, (String) data.get("body"), (String) data.get("notifyName"));
                }

                if (messageArray != null) {
                    traitementMessage(data, messageArray);
                }
            }
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(response)
                    .build();

            response.put("message", "Webhook received");
            return ResponseEntity.ok(apiResponseDto);

        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
    }

    private void traitementMessage(Map<String, Object> data, InboxMessage inboxMessage) {
        try {
            String type = (String) data.get("type");
            String content = inboxMessage.getContent();
            InboxMessage inboxMessageSaved = new InboxMessage();

            Optional<Inbox> inbox = inboxRepository.findByCode(inboxMessage.getChatId());
            if (inbox.isEmpty()) {
                System.out.println("IS NEW CHAT");
                Inbox inbox2 = new Inbox();
                inbox2.setCode(inboxMessage.getChatId());
                inbox2.setPhone(inboxMessage.getChatId().split("@")[0]);
                inbox2.setFirstMessage(inboxMessageSaved.getId());
                inbox2 = inboxRepository.save(inbox2);
                inboxMessage.setInbox(inbox2);

            }else{
                inboxMessage.setInbox(inbox.get());
            }



            if ("chat".equals(type)) {

                inboxMessageSaved = inboxMessageRepository.save(inboxMessage);
            } else if ("image".equals(type)) {
                String fileName = saveBase64File(inboxMessage.getContent(), "image/png");
                inboxMessage.setContent(fileName);
                inboxMessageSaved = inboxMessageRepository.save(inboxMessage);
            } else if ("video".equals(type)) {
                String fileName = saveBase64File(inboxMessage.getContent(), "video/mp4");
                inboxMessage.setContent(fileName);
                inboxMessageSaved = inboxMessageRepository.save(inboxMessage);
            } else if ("document".equals(type)) {

                String fileName = saveBase64File(inboxMessage.getContent(), "document/pdf");
                inboxMessage.setContent(fileName);
                inboxMessageSaved = inboxMessageRepository.save(inboxMessage);
            } else if ("audio".equals(type)) {
                String fileName = saveBase64File(inboxMessage.getContent(), "audio/mp3");
                inboxMessage.setContent(fileName);
                inboxMessageSaved = inboxMessageRepository.save(inboxMessage);

            } else if ("ptt".equals(type)) {
                String fileName = saveBase64File(inboxMessage.getContent(), "audio/mp3");
                inboxMessage.setContent(fileName);
                inboxMessageSaved = inboxMessageRepository.save(inboxMessage);
            } else if ("sticker".equals(type)) {
                String fileName = saveBase64File(inboxMessage.getContent(), "image/webp");
                inboxMessage.setContent(fileName);
                inboxMessageSaved = inboxMessageRepository.save(inboxMessage);
            }

        } catch (Exception e) {

        }
    }

    private InboxMessage formatData(Map<String, Object> data, String body, String senderName) {

        String chatId = (String) data.get("chatId");
        String messageId = (String) data.get("id");
        String type = (String) data.get("type");

        InboxMessage message = new InboxMessage();
        message.setChatId(chatId);
        message.setSenderId((String) data.get("from"));
        message.setDate(System.currentTimeMillis() + " ");
        message.setSenderName(senderName);
        message.setSenderPhone(chatId.split("@")[0]);
        message.setContent(body);
        message.setType(type);
        message.setMessage_id(messageId);

        return message;
    }

    private String saveBase64File(String content, String type) {
        Media media = mediaServiceImpl.storeFileWhatsapp(content, type);

        return media.getName(); // Remplacez par le chemin réel ou l'URL du fichier
    }

}
