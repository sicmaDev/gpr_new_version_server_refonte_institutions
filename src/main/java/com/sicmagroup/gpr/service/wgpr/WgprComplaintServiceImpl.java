package com.sicmagroup.gpr.service.wgpr;

import com.sicmagroup.gpr.domain.dto.wgpr.*;
import com.sicmagroup.gpr.domain.model.*;
import com.sicmagroup.gpr.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WgprComplaintServiceImpl implements WgprComplaintService {

    private final WgprMessageRepository     messageRepository;
    private final WgprWhatsappBridgeService bridgeService;

    @Override
    @Transactional
    public void markMessageRead(Long messageId) {
        messageRepository.findById(messageId).ifPresent(m -> {
            m.setRead(true);
            messageRepository.save(m);
        });
    }

    @Override
    @Transactional
    public void markMessagesRead(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        ids.forEach(this::markMessageRead);
    }

    @Override
    public List<MessageResponseDto> getAllMessages() {
        // Filtrer par numéro GPR connecté — évite le mélange lors d'un changement de numéro.
        // Comparaison normalisée sur les chiffres uniquement : le JID renvoyé par Node peut
        // varier de format (ex. suffixe @c.us vs @s.whatsapp.net selon la version du service),
        // une égalité de chaîne stricte ferait alors disparaître tous les messages à tort.
        String currentNumber           = bridgeService.getConnectedNumber();
        String normalizedCurrentNumber = normalizePhoneDigits(currentNumber);

        List<WgprMessage> allMessages = messageRepository.findAll();
        List<WgprMessage> all = normalizedCurrentNumber.isEmpty()
                ? allMessages // fallback si Node.js inaccessible
                : allMessages.stream()
                        .filter(m -> normalizedCurrentNumber.equals(normalizePhoneDigits(m.getConnectedNumber())))
                        .collect(Collectors.toList());

        return all.stream()
                .map(m -> toMessageResponse(m, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markMessagesAsConverted(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        ids.forEach(id -> messageRepository.findById(id).ifPresent(m -> {
            m.setStatus("converted");
            messageRepository.save(m);
        }));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /** Extrait uniquement les chiffres d'un JID/numéro pour comparer sans dépendre du format
     *  (suffixe @s.whatsapp.net vs @c.us, espaces, etc.). */
    private String normalizePhoneDigits(String raw) {
        return raw == null ? "" : raw.replaceAll("[^0-9]", "");
    }

    private MessageResponseDto toMessageResponse(WgprMessage message, Long complaintId) {
        String waId = message.getWaMessageId();
        return MessageResponseDto.builder()
                .id(message.getId())
                .messageId(waId)
                .fromNumber(message.getFromNumber())
                .fromName(message.getFromName())
                .content(message.getBody())
                .type(message.getWaType() != null ? message.getWaType() : "chat")
                .mediaPath(message.getMediaPath())
                .timestamp(message.getTimestamp())
                .read(message.isRead())
                // Toujours false : le microservice Node n'enregistre jamais les messages sortants
                // (fromMe) en base, donc toute ligne de wgpr_messages est par construction un
                // message reçu. L'ancienne dérivation via un préfixe "true_" sur l'ID était un
                // reliquat de convention whatsapp-web.js sans équivalent réel côté Baileys.
                .sent(false)
                .status(message.getStatus())
                .complaintId(complaintId)
                .createdAt(message.getCreatedAt() != null ? message.getCreatedAt().toString() : null)
                .build();
    }
}
