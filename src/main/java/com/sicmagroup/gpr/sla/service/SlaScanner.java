package com.sicmagroup.gpr.sla.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sicmagroup.gpr.domain.enumeration.ClaimEventType;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.HistoriqueTransmission;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.HistoriqueTransmissionRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.service.claimEvent.ClaimEventService;
import com.sicmagroup.gpr.sla.domain.ClaimSla;
import com.sicmagroup.gpr.sla.domain.SlaEvent;
import com.sicmagroup.gpr.sla.domain.SlaOwnerLevel;
import com.sicmagroup.gpr.sla.domain.SlaPhase;
import com.sicmagroup.gpr.sla.repository.ClaimSlaRepository;
import com.sicmagroup.gpr.sla.repository.SlaEventRepository;
import com.sicmagroup.gpr.sla.service.SlaMessages.Line;
import com.sicmagroup.gpr.sla.service.SlaNotifier.Recipients;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Traitement d'un compteur SLA arrivé à échéance de contrôle : rappels à 50 % et 75 %, alerte de dépassement,
 * remontée automatique au niveau supérieur, échéance réglementaire. Chaque compteur est traité dans sa propre
 * transaction. Une alerte donnée n'est enregistrée (donc envoyée) qu'une seule fois : la clé de déduplication
 * est unique en base, même si deux instances du serveur tournent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlaScanner {

    static final String SYSTEM = "Système GPR";

    private final ClaimSlaRepository repository;
    private final SlaEventRepository eventRepository;
    private final ClaimRepository claimRepository;
    private final SuggestionRepository suggestionRepository;
    private final UserRepository userRepository;
    private final HistoriqueTransmissionRepository transmissionRepository;
    private final ClaimEventService claimEventService;
    private final SlaEngine engine;
    private final SlaConfig config;
    private final BusinessTime businessTime;
    private final SlaHierarchy hierarchy;
    private final SlaNotifier notifier;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(Long slaId) {
        ClaimSla s = repository.findById(slaId).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (s == null || s.getNextCheckAt() == null || s.getNextCheckAt().isAfter(now)
                || (s.getPhase() != SlaPhase.OPEN && s.getPhase() != SlaPhase.PAUSED)) {
            return;
        }
        if (s.getTargetType() == ClaimType.SUGGESTION) {
            processSuggestion(s, now);
        } else {
            Claim claim = claimRepository.findById(s.getClaimId()).orElse(null);
            if (claim == null || claim.isDeleted()) {
                s.setPhase(SlaPhase.CANCELLED);
                s.setNextCheckAt(null);
                repository.save(s);
                return;
            }
            User owner = s.getOwnerUserId() == null ? null : userRepository.findById(s.getOwnerUserId()).orElse(null);
            if (s.getPhase() == SlaPhase.OPEN) {
                takeoverBreach(s, claim, now);
                reminders(s, claim, owner, now);
                breach(s, claim, owner, now);
                escalation(s, claim, owner, now);
            }
            regulatory(s, claim, owner, now);
            engine.syncClaimData(s, claim);
        }
        s.setUpdatedAt(now);
        engine.recompute(s);
        repository.save(s);
        SlaSummaryCache.clear();
    }

    // ---------------------------------------------------------------------------------------------

    private static long epochMinutes(LocalDateTime d) {
        return d == null ? 0 : d.atZone(ZoneId.systemDefault()).toEpochSecond() / 60;
    }

    /** Enregistre l'alerte (une seule fois) et la trace dans l'historique de la plainte. */
    private boolean emit(ClaimSla s, Claim claim, ClaimEventType type, String key, String metadata) {
        String dedup = s.getId() + ":" + type + ":" + key;
        if (eventRepository.existsByDedupKey(dedup)) {
            return false;
        }
        eventRepository.save(SlaEvent.builder()
                .claimSlaId(s.getId())
                .claimId(claim.getId())
                .targetType(claim.getType())
                .eventType(type.name())
                .dedupKey(dedup)
                .detail(metadata)
                .createdAt(LocalDateTime.now())
                .build());
        claimEventService.log(claim.getId(), claim.getCodeClient(), claim.getType(), type, SYSTEM, null, metadata);
        return true;
    }

    private Recipients recipients(Claim claim, User owner, boolean closure) {
        if (closure) {
            List<User> cc = new ArrayList<>();
            User ra = hierarchy.agencyRa(claim);
            if (ra != null) {
                cc.add(ra);
            }
            cc.addAll(hierarchy.des());
            return Recipients.of(hierarchy.measurers(claim), cc);
        }
        return notifier.ownerAndRa(claim, owner);
    }

    private String lateText(LocalDateTime due, LocalDateTime now) {
        long hours = Duration.between(due, now).toHours();
        return "Retard de " + hours / 24 + " jr(s) " + hours % 24 + " h";
    }

    // --- Prise en charge ---------------------------------------------------------------------------

    private void takeoverBreach(ClaimSla s, Claim claim, LocalDateTime now) {
        if (s.getTakeoverAt() != null || s.getTakeoverDueAt() == null || s.isTakeoverBreachNotified()
                || !now.isAfter(s.getTakeoverDueAt())) {
            return;
        }
        s.setTakeoverBreachNotified(true);
        if (emit(s, claim, ClaimEventType.SLA_BREACH, "TAKEOVER", "PRISE_EN_CHARGE|" + SlaMessages.date(s.getTakeoverDueAt()))) {
            Line line = SlaNotifier.line(claim, s.getTakeoverDueAt(), "Non prise en charge");
            notifier.mail(notifier.ownerAndRa(claim, null), "GPR - Plainte non prise en charge",
                    greeting -> SlaMessages.takeoverBreach(greeting, line));
        }
    }

    // --- Rappels à 50 % et 75 % ---------------------------------------------------------------------

    private void reminders(ClaimSla s, Claim claim, User owner, LocalDateTime now) {
        LocalDateTime due = SlaEngine.currentDue(s);
        if (due == null) {
            return;
        }
        boolean r1 = !s.isReminder1Sent() && isReached(engine.reminderAt(s, s.getReminder1Pct()), now);
        boolean r2 = !s.isReminder2Sent() && isReached(engine.reminderAt(s, s.getReminder2Pct()), now);
        if (!r1 && !r2) {
            return;
        }
        // un seul message, au seuil le plus avancé
        int pct = r2 ? s.getReminder2Pct() : s.getReminder1Pct();
        if (r1 || r2) {
            s.setReminder1Sent(true);
        }
        if (r2) {
            s.setReminder2Sent(true);
        }
        if (now.isAfter(due)) {
            return; // déjà dépassé : l'alerte de dépassement suffit
        }
        boolean closure = s.getResolvedAt() != null;
        String key = pct + ":" + epochMinutes(due);
        if (!emit(s, claim, ClaimEventType.SLA_REMINDER, key, pct + "% du délai écoulé|" + SlaMessages.date(due))) {
            return;
        }
        Line line = SlaNotifier.line(claim, due, pct + " % écoulé");
        Recipients rec = recipients(claim, owner, closure);
        if (config.dailyDigest()) {
            for (User u : mergedRecipients(rec)) {
                String dedup = s.getId() + ":DIGEST:" + key + ":" + u.getId();
                if (!eventRepository.existsByDedupKey(dedup)) {
                    eventRepository.save(SlaEvent.builder()
                            .claimSlaId(s.getId()).claimId(claim.getId()).targetType(claim.getType())
                            .eventType("DIGEST_ITEM").dedupKey(dedup).recipientUserId(u.getId())
                            .detail(encode(line)).digestPending(true).createdAt(LocalDateTime.now()).build());
                }
            }
        } else {
            notifier.mail(rec, SlaMessages.subjectReminder(pct), greeting -> SlaMessages.reminder(greeting, line, pct));
        }
    }

    private static boolean isReached(LocalDateTime t, LocalDateTime now) {
        return t != null && !now.isBefore(t);
    }

    private static List<User> mergedRecipients(Recipients rec) {
        List<User> all = new ArrayList<>(rec.to());
        all.addAll(rec.cc());
        return all;
    }

    // --- Dépassement ---------------------------------------------------------------------------------

    private void breach(ClaimSla s, Claim claim, User owner, LocalDateTime now) {
        LocalDateTime due = SlaEngine.currentDue(s);
        if (due == null || s.isBreachNotified() || !now.isAfter(due)) {
            return;
        }
        s.setBreachNotified(true);
        s.setBreachNotifiedAt(now);
        s.setReminder1Sent(true);
        s.setReminder2Sent(true);
        boolean closure = s.getResolvedAt() != null;
        String detail = lateText(due, now);
        if (emit(s, claim, ClaimEventType.SLA_BREACH, (closure ? "CLO" : "RES") + ":" + epochMinutes(due),
                detail + "|" + SlaMessages.date(due))) {
            Line line = SlaNotifier.line(claim, due, detail);
            Recipients rec = recipients(claim, owner, closure);
            notifier.mail(rec, "GPR - Délai dépassé", greeting -> SlaMessages.breach(greeting, line));
            notifier.sms(rec.to(), SlaMessages.sms("délai dépassé", line));
        }
        // message d'attente au client (réclamations seulement, et seulement si l'institution l'a choisi)
        if (!closure && claim.getType() == ClaimType.CLAIM && "AUTO".equals(config.customerWaitingMessage())) {
            String dedup = s.getId() + ":CLIENT_MSG:" + epochMinutes(due);
            if (!eventRepository.existsByDedupKey(dedup) && notifier.clientWaitingMessage(claim)) {
                eventRepository.save(SlaEvent.builder().claimSlaId(s.getId()).claimId(claim.getId())
                        .targetType(claim.getType()).eventType("CLIENT_WAITING_MESSAGE").dedupKey(dedup)
                        .detail("Message d'attente").createdAt(now).build());
                claimEventService.log(claim.getId(), claim.getCodeClient(), claim.getType(),
                        ClaimEventType.MAIL_SENT_CLIENT, SYSTEM, null, "Message d'attente (retard)");
            }
        }
        // la remontée automatique démarre : délai de grâce
        if (config.autoEscalation()) {
            s.setNextEscalationAt(businessTime.add(s.isBusinessTime(), now, s.getGraceMinutes()));
        }
    }

    // --- Remontée automatique ------------------------------------------------------------------------

    private void escalation(ClaimSla s, Claim claim, User owner, LocalDateTime now) {
        if (!config.autoEscalation() || s.getNextEscalationAt() == null || now.isBefore(s.getNextEscalationAt())
                || !s.isBreachNotified()) {
            return;
        }
        LocalDateTime due = SlaEngine.currentDue(s);
        if (due == null || !now.isAfter(due)) {
            s.setNextEscalationAt(null);
            return;
        }
        SlaOwnerLevel level = s.getOwnerLevel() == null ? SlaOwnerLevel.AGENT : s.getOwnerLevel();
        SlaHierarchy.Target target = hierarchy.nextTarget(claim, level, s.getOwnerUserId());
        if (target == null) {
            // dernier niveau atteint : plus de transmission, récapitulatif aux responsables
            s.setNextEscalationAt(null);
            if (!s.isStuckNotified()) {
                s.setStuckNotified(true);
                if (emit(s, claim, ClaimEventType.SLA_STUCK, "STUCK:" + epochMinutes(due), lateText(due, now))) {
                    Line line = SlaNotifier.line(claim, due, lateText(due, now));
                    List<User> to = new ArrayList<>(hierarchy.pilotes());
                    Recipients rec = Recipients.of(to, hierarchy.des());
                    notifier.mail(rec, "GPR - Dossier bloqué au dernier niveau",
                            greeting -> SlaMessages.stuck(greeting, List.of(line)));
                }
            }
            return;
        }
        User from = owner;
        String fromName = from == null ? "(non affectée)" : from.getFirstandlastname();
        int unanswered = (s.isReminder1Sent() ? 1 : 0) + (s.isReminder2Sent() ? 1 : 0) + 1;

        // transmission : même effet que l'action « Transmettre » existante, avec « Système GPR » comme auteur
        claim.setTransmitted(true);
        claim.setTransmittedTo(target.user());
        claim.setTransmittedBy(null);
        claim.setTransmissionComment("Remontée automatique : échéance dépassée (" + SlaMessages.date(due) + ")");
        claim.setUpdatedAt(now);
        claimRepository.save(claim);
        transmissionRepository.save(HistoriqueTransmission.builder()
                .claimId(claim.getId())
                .codePlainte(claim.getType() == ClaimType.DENUNCIACION ? claim.getCode() : claim.getCodeClient())
                .typePlainte(claim.getType())
                .transmisParNom(SYSTEM)
                .transmisAId(target.user().getId())
                .transmisANom(target.user().getFirstandlastname())
                .commentaire(claim.getTransmissionComment())
                .dateTransmission(now)
                .build());

        s.setEscalationCount(s.getEscalationCount() + 1);
        s.setOwnerUserId(target.user().getId());
        s.setOwnerLevel(target.level());
        // le délai de grâce du niveau suivant démarre ; le chronomètre du client n'est jamais remis à zéro
        s.setNextEscalationAt(target.level() == SlaOwnerLevel.DE ? null
                : businessTime.add(s.isBusinessTime(), now, s.getGraceMinutes()));

        String metadata = fromName + "|" + level + "|" + target.user().getFirstandlastname() + "|" + target.level()
                + "|" + SlaMessages.date(due) + "|" + unanswered;
        if (emit(s, claim, ClaimEventType.AUTO_TRANSMITTED, "ESC:" + s.getEscalationCount(), metadata)) {
            Line line = SlaNotifier.line(claim, due, lateText(due, now));
            Recipients rec = Recipients.of(List.of(target.user()), target.copy());
            notifier.mail(rec, "GPR - Plainte remontée automatiquement",
                    greeting -> SlaMessages.escalation(greeting, line, fromName, target.level().name(), unanswered));
            notifier.sms(List.of(target.user()), SlaMessages.sms("remontée automatique", line));
        }
    }

    // --- Échéance réglementaire (jamais mise en pause) --------------------------------------------------

    private void regulatory(ClaimSla s, Claim claim, User owner, LocalDateTime now) {
        LocalDateTime reg = s.getRegulatoryDueAt();
        if (reg == null || s.getResolvedAt() != null) {
            return;
        }
        if (!s.isRegulatoryWarningSent() && !now.isBefore(reg.minusDays(config.regulatoryWarningDays()))) {
            s.setRegulatoryWarningSent(true);
            if (!now.isAfter(reg) && emit(s, claim, ClaimEventType.SLA_REGULATORY_WARNING, "W", SlaMessages.date(reg))) {
                Line line = SlaNotifier.line(claim, reg, "Limite réglementaire");
                int days = config.regulatoryWarningDays();
                notifier.mail(notifier.regulatory(claim, owner), "GPR - Limite réglementaire proche",
                        greeting -> SlaMessages.regulatoryWarning(greeting, line, days));
            }
        }
        if (!s.isRegulatoryBreachSent() && now.isAfter(reg)) {
            s.setRegulatoryBreachSent(true);
            s.setRegulatoryBreached(true);
            if (emit(s, claim, ClaimEventType.SLA_REGULATORY_BREACH, "B", SlaMessages.date(reg))) {
                Line line = SlaNotifier.line(claim, reg, "Hors délai réglementaire");
                notifier.mail(notifier.regulatory(claim, owner), "GPR - Hors délai réglementaire",
                        greeting -> SlaMessages.regulatoryBreach(greeting, line));
            }
        }
    }

    // --- Suggestions : un délai de réponse, sans escalade ---------------------------------------------------

    private void processSuggestion(ClaimSla s, LocalDateTime now) {
        Suggestion suggestion = suggestionRepository.findById(s.getClaimId()).orElse(null);
        if (suggestion == null || suggestion.isDeleted()) {
            s.setPhase(SlaPhase.CANCELLED);
            s.setNextCheckAt(null);
            repository.save(s);
            return;
        }
        LocalDateTime due = s.getResolutionDueAt();
        boolean r1 = !s.isReminder1Sent() && isReached(engine.reminderAt(s, s.getReminder1Pct()), now);
        boolean r2 = !s.isReminder2Sent() && isReached(engine.reminderAt(s, s.getReminder2Pct()), now);
        if (r1 || r2) {
            s.setReminder1Sent(true);
            if (r2) {
                s.setReminder2Sent(true);
            }
        }
        boolean late = due != null && now.isAfter(due) && !s.isBreachNotified();
        if (late) {
            s.setBreachNotified(true);
            s.setBreachNotifiedAt(now);
            User ra = suggestion.getServiceIndexe() == null ? null
                    : userRepository.findRaByServicePointId(suggestion.getServiceIndexe().getId()).orElse(null);
            String key = "SUG-BREACH:" + epochMinutes(due);
            String dedup = s.getId() + ":" + ClaimEventType.SLA_BREACH + ":" + key;
            if (ra != null && !eventRepository.existsByDedupKey(dedup)) {
                eventRepository.save(SlaEvent.builder().claimSlaId(s.getId()).claimId(suggestion.getId())
                        .targetType(ClaimType.SUGGESTION).eventType(ClaimEventType.SLA_BREACH.name()).dedupKey(dedup)
                        .detail(lateText(due, now)).createdAt(now).build());
                Line line = new Line(ClaimType.SUGGESTION, suggestion.getCodeClient(), null,
                        suggestion.getServiceIndexe().getLibelle(), due, lateText(due, now));
                notifier.mail(Recipients.of(List.of(ra), List.of()), "GPR - Suggestion sans réponse dans le délai",
                        greeting -> SlaMessages.breach(greeting, line));
            }
        }
    }

    // --- Récapitulatif quotidien : (dé)codage d'une ligne --------------------------------------------------

    static String encode(Line l) {
        return String.join("|", l.type().name(), nz(l.code()), nz(l.objet()), nz(l.agence()),
                l.dueAt() == null ? "" : l.dueAt().toString(), nz(l.detail()));
    }

    static Line decode(String s) {
        String[] p = s.split("\\|", -1);
        return new Line(ClaimType.valueOf(p[0]), p[1], p[2], p[3], p[4].isEmpty() ? null : LocalDateTime.parse(p[4]),
                p[5]);
    }

    private static String nz(String v) {
        return v == null ? "" : v.replace("|", "/");
    }
}
