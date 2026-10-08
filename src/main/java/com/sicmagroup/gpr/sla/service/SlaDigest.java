package com.sicmagroup.gpr.sla.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.sla.domain.ClaimSla;
import com.sicmagroup.gpr.sla.domain.SlaEvent;
import com.sicmagroup.gpr.sla.domain.SlaPhase;
import com.sicmagroup.gpr.sla.repository.ClaimSlaRepository;
import com.sicmagroup.gpr.sla.repository.SlaEventRepository;
import com.sicmagroup.gpr.sla.service.SlaMessages.Line;
import com.sicmagroup.gpr.sla.service.SlaNotifier.Recipients;

import lombok.RequiredArgsConstructor;

/**
 * Récapitulatif quotidien : les rappels (50 % et 75 %) sont regroupés en un seul message par destinataire,
 * à l'heure « sla.digest_time ». Les dossiers bloqués au dernier niveau sont rappelés chaque jour au Pilote et
 * au DE. Un seul envoi par jour, même si plusieurs instances du serveur tournent (clé de déduplication unique).
 */
@Service
@RequiredArgsConstructor
public class SlaDigest {

    private final SlaEventRepository eventRepository;
    private final UserRepository userRepository;
    private final ClaimSlaRepository slaRepository;
    private final ClaimRepository claimRepository;
    private final SlaNotifier notifier;
    private final SlaHierarchy hierarchy;
    private final SlaConfig config;

    /** Envoie le récapitulatif du jour s'il est l'heure et s'il n'a pas déjà été envoyé. */
    @Transactional
    public boolean sendIfDue(LocalDateTime now) {
        if (!config.enabled() || now.toLocalTime().isBefore(config.digestTime())) {
            return false;
        }
        String runKey = "DIGEST_RUN:" + LocalDate.from(now);
        if (eventRepository.existsByDedupKey(runKey)) {
            return false;
        }
        eventRepository.save(SlaEvent.builder().eventType("DIGEST_RUN").dedupKey(runKey).createdAt(now).build());

        // 1. rappels en attente, regroupés par destinataire
        Map<Long, List<SlaEvent>> byUser = new LinkedHashMap<>();
        for (SlaEvent e : eventRepository.findByDigestPendingTrueOrderByRecipientUserIdAscCreatedAtAsc()) {
            byUser.computeIfAbsent(e.getRecipientUserId(), k -> new ArrayList<>()).add(e);
        }
        for (Map.Entry<Long, List<SlaEvent>> entry : byUser.entrySet()) {
            User user = userRepository.findById(entry.getKey()).orElse(null);
            List<Line> lines = new ArrayList<>();
            for (SlaEvent e : entry.getValue()) {
                lines.add(SlaScanner.decode(e.getDetail()));
                e.setDigestPending(false);
                e.setDigestSentAt(now);
            }
            eventRepository.saveAll(entry.getValue());
            if (user != null) {
                notifier.mail(Recipients.of(List.of(user), List.of()),
                        "GPR - Récapitulatif quotidien des délais", greeting -> SlaMessages.digest(greeting, lines));
            }
        }

        // 2. dossiers bloqués au dernier niveau : rappel quotidien au Pilote et au DE
        List<Line> stuck = new ArrayList<>();
        for (ClaimSla s : slaRepository.findByPhaseAndStuckNotifiedTrue(SlaPhase.OPEN)) {
            Claim claim = claimRepository.findById(s.getClaimId()).orElse(null);
            if (claim != null && !claim.isDeleted()) {
                stuck.add(SlaNotifier.line(claim, SlaEngine.currentDue(s), "Bloqué au dernier niveau"));
            }
        }
        if (!stuck.isEmpty()) {
            Recipients rec = Recipients.of(hierarchy.pilotes(), hierarchy.des());
            notifier.mail(rec, "GPR - Dossiers bloqués", greeting -> SlaMessages.stuck(greeting, stuck));
        }
        return true;
    }
}
