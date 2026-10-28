package com.sicmagroup.gpr.service.smsLog;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.dto.SmsLogDto;
import com.sicmagroup.gpr.domain.enumeration.MessageChannel;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.SmsLog;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.SmsLogRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SmsLogServiceImpl implements SmsLogService {

    private final SmsLogRepository smsLogRepository;
    private final ClaimRepository claimRepository;
    private final SuggestionRepository suggestionRepository;

    @Override
    public List<SmsLogDto> getAll() {
        return toDtos(smsLogRepository.findByIsDeletedFalseOrderByCreatedAtDesc());
    }

    @Override
    public Optional<SmsLogDto> getById(Long id) {
        return smsLogRepository.findById(id).map(log -> toDtos(List.of(log)).get(0));
    }

    @Override
    public SmsLog save(SmsLog smsLog) {
        return smsLogRepository.save(smsLog);
    }

    /** Ajoute à chaque SMS le code client de la plainte ou de la suggestion liée, en deux requêtes au plus. */
    private List<SmsLogDto> toDtos(List<SmsLog> smsLogs) {
        Set<Long> claimIds = smsLogs.stream().map(SmsLog::getClaimId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> suggestIds = smsLogs.stream().map(SmsLog::getSuggestId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> claimCodes = claimIds.isEmpty() ? Map.of()
                : claimRepository.findAllById(claimIds).stream()
                        .collect(Collectors.toMap(Claim::getId, c -> displayCode(c.getCodeClient(), c.getCode())));
        Map<Long, String> suggestCodes = suggestIds.isEmpty() ? Map.of()
                : suggestionRepository.findAllById(suggestIds).stream()
                        .collect(Collectors.toMap(Suggestion::getId, s -> displayCode(s.getCodeClient(), s.getCode())));

        return smsLogs.stream().map(l -> SmsLogDto.builder()
                .id(l.getId())
                .channel(l.getChannel() != null ? l.getChannel() : MessageChannel.SMS)
                .type(l.getType())
                .claimId(l.getClaimId())
                .claimCode(l.getClaimId() != null ? claimCodes.get(l.getClaimId()) : null)
                .suggestId(l.getSuggestId())
                .suggestCode(l.getSuggestId() != null ? suggestCodes.get(l.getSuggestId()) : null)
                .numeroDestinataire(l.getNumeroDestinataire())
                .content(l.getContent())
                .status(l.getStatus())
                .createdAt(l.getCreatedAt())
                .sendAt(l.getSendAt())
                .updatedAt(l.getUpdatedAt())
                .isDeleted(l.isDeleted())
                .build()).toList();
    }

    /** Code client, sinon code technique, jamais null : Collectors.toMap refuse les valeurs nulles. */
    private static String displayCode(String codeClient, String code) {
        if (codeClient != null && !codeClient.isBlank()) return codeClient;
        return code != null ? code : "";
    }
}
