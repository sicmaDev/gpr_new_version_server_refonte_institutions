package com.sicmagroup.gpr.sla.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.dto.ClaimDto;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.sla.domain.ClaimSla;
import com.sicmagroup.gpr.sla.domain.SlaPhase;
import com.sicmagroup.gpr.sla.domain.SlaState;
import com.sicmagroup.gpr.sla.dto.SlaInfo;
import com.sicmagroup.gpr.sla.repository.ClaimSlaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Transforme les compteurs SLA en bloc « sla » lisible : état, échéance, minutes restantes, responsable.
 * Le serveur est la seule source de vérité : tous les écrans lisent ce calcul.
 */
@Service
@RequiredArgsConstructor
public class SlaInfoService {

    private final ClaimSlaRepository repository;
    private final UserRepository userRepository;
    private final BusinessTime businessTime;
    private final SlaConfig config;

    /** État affiché d'un compteur à l'instant now. */
    public SlaState stateOf(ClaimSla s, LocalDateTime now) {
        SlaPhase phase = s.getPhase();
        if (phase == null || phase == SlaPhase.CANCELLED) {
            return SlaState.ANNULE;
        }
        if (s.isRegulatoryBreached()) {
            return SlaState.HORS_DELAI;
        }
        switch (phase) {
            case SUSPENDED, PAUSED:
                return SlaState.SUSPENDU;
            case DONE:
                boolean late = s.getResolvedAt() != null && s.getResolutionDueAt() != null
                        && s.getResolvedAt().isAfter(s.getResolutionDueAt());
                return late ? SlaState.DEPASSE : SlaState.RESPECTE;
            default:
                break;
        }
        LocalDateTime due = SlaEngine.currentDue(s);
        if (due != null && now.isAfter(due)) {
            return SlaState.DEPASSE;
        }
        return s.isReminder2Sent() || consumedPct(s, now) >= s.getReminder2Pct() ? SlaState.A_RISQUE
                : SlaState.EN_COURS;
    }

    /** Pourcentage du délai en vigueur déjà consommé (0 à 100). */
    public int consumedPct(ClaimSla s, LocalDateTime now) {
        LocalDateTime start = s.getResolvedAt() == null ? s.getReceivedAt() : s.getResolvedAt();
        LocalDateTime due = SlaEngine.currentDue(s);
        if (start == null || due == null) {
            return 0;
        }
        long total = businessTime.between(s.isBusinessTime(), start, due);
        if (total <= 0) {
            return 100;
        }
        LocalDateTime ref = s.getPhase() == SlaPhase.PAUSED && s.getPausedAt() != null ? s.getPausedAt() : now;
        long elapsed = businessTime.between(s.isBusinessTime(), start, ref);
        return (int) Math.max(0, Math.min(100, elapsed * 100 / total));
    }

    public SlaInfo toInfo(ClaimSla s, Map<Long, String> ownerNames) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = SlaEngine.currentDue(s);
        boolean open = s.getPhase() == SlaPhase.OPEN || s.getPhase() == SlaPhase.PAUSED;
        LocalDateTime ref = s.getPhase() == SlaPhase.PAUSED && s.getPausedAt() != null ? s.getPausedAt() : now;
        // la prise en charge est suivie à part ; l'échéance affichée est celle de la résolution, puis de la clôture
        String delay = s.getResolvedAt() != null ? "CLOTURE" : "RESOLUTION";
        return SlaInfo.builder()
                .state(stateOf(s, now))
                .phase(s.getPhase())
                .paused(s.getPhase() == SlaPhase.PAUSED)
                .delay(delay)
                .cycleKind(s.getCycleKind())
                .dueAt(due)
                .regulatoryDueAt(s.getRegulatoryDueAt())
                .remainingMinutes(open && due != null ? businessTime.between(s.isBusinessTime(), ref, due) : null)
                .minutesPerDay(s.isBusinessTime() ? (int) businessTime.workdayMinutes() : 1440)
                .consumedPct(open ? consumedPct(s, now) : null)
                .ownerId(s.getOwnerUserId())
                .ownerName(s.getOwnerUserId() == null ? null : ownerNames.get(s.getOwnerUserId()))
                .ownerLevel(s.getOwnerLevel())
                .escalationLevel(s.getEscalationCount())
                .regulatoryBreached(s.isRegulatoryBreached())
                .justified(s.getBreachReasonId() != null)
                .autoEscalated(s.getEscalationCount() > 0)
                .customerMessage(config.customerWaitingMessage())
                .build();
    }

    public Map<Long, String> ownerNames(Collection<ClaimSla> counters) {
        Set<Long> ids = new HashSet<>();
        for (ClaimSla s : counters) {
            if (s.getOwnerUserId() != null) {
                ids.add(s.getOwnerUserId());
            }
        }
        Map<Long, String> names = new HashMap<>();
        if (!ids.isEmpty()) {
            for (User u : userRepository.findAllById(ids)) {
                names.put(u.getId(), u.getFirstandlastname());
            }
        }
        return names;
    }

    /** Renseigne le bloc sla de chaque plainte d'une liste (une seule requête pour tout le lot). */
    public void attach(List<ClaimDto> dtos) {
        if (dtos == null || dtos.isEmpty() || !config.enabled()) {
            return;
        }
        Map<ClaimType, Set<Long>> idsByType = new HashMap<>();
        for (ClaimDto d : dtos) {
            if (d.getId() != null && d.getType() != null) {
                idsByType.computeIfAbsent(d.getType(), k -> new HashSet<>()).add(d.getId());
            }
        }
        Map<String, ClaimSla> latest = new HashMap<>();
        List<ClaimSla> all = new java.util.ArrayList<>();
        for (Map.Entry<ClaimType, Set<Long>> e : idsByType.entrySet()) {
            for (ClaimSla s : repository.findByTargetTypeAndClaimIdInAndPhaseNot(e.getKey(), e.getValue(),
                    SlaPhase.CANCELLED)) {
                all.add(s);
                String key = s.getTargetType() + ":" + s.getClaimId();
                ClaimSla known = latest.get(key);
                if (known == null || known.getCycle() < s.getCycle()) {
                    latest.put(key, s);
                }
            }
        }
        Map<Long, String> names = ownerNames(latest.values());
        for (ClaimDto d : dtos) {
            ClaimSla s = latest.get(d.getType() + ":" + d.getId());
            if (s != null) {
                d.setSla(toInfo(s, names));
            }
        }
    }
}
