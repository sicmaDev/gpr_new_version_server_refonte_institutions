package com.sicmagroup.gpr.service.claimEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.api.claim.ClaimEventResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimEventType;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.ClaimEvent;
import com.sicmagroup.gpr.repository.ClaimEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClaimEventServiceImpl implements ClaimEventService {

    private final ClaimEventRepository repository;

    @Override
    public void log(Long claimId, String claimCode, ClaimType claimType, ClaimEventType eventType,
                    String actorName, String actorEmail, String metadata) {
        ClaimEvent event = ClaimEvent.builder()
                .claimId(claimId)
                .claimCode(claimCode)
                .claimType(claimType)
                .eventType(eventType)
                .actorName(actorName)
                .actorEmail(actorEmail)
                .metadata(metadata)
                .createdAt(LocalDateTime.now())
                .build();
        repository.save(event);
    }

    @Override
    public List<ClaimEventResponse> getByClaimId(Long claimId) {
        return repository.findByClaimIdOrderByCreatedAtAsc(claimId)
                .stream()
                .map(e -> ClaimEventResponse.builder()
                        .id(e.getId())
                        .claimId(e.getClaimId())
                        .claimCode(e.getClaimCode())
                        .claimType(e.getClaimType())
                        .eventType(e.getEventType())
                        .actorName(e.getActorName())
                        .actorEmail(e.getActorEmail())
                        .metadata(e.getMetadata())
                        .createdAt(e.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
