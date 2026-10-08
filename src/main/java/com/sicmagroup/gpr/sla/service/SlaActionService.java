package com.sicmagroup.gpr.sla.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sicmagroup.gpr.domain.enumeration.ClaimEventType;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.service.claimEvent.ClaimEventService;
import com.sicmagroup.gpr.sla.domain.ClaimSla;
import com.sicmagroup.gpr.sla.domain.ContactAttempt;
import com.sicmagroup.gpr.sla.domain.SlaBreachReason;
import com.sicmagroup.gpr.sla.domain.SlaPhase;
import com.sicmagroup.gpr.sla.repository.ClaimSlaRepository;
import com.sicmagroup.gpr.sla.repository.ContactAttemptRepository;
import com.sicmagroup.gpr.sla.repository.SlaBreachReasonRepository;
import com.sicmagroup.gpr.sla.repository.SlaEventRepository;

import lombok.RequiredArgsConstructor;

/**
 * Actions humaines liées au SLA : attente du client, reprise, justification d'un retard, tentatives de
 * contact, clôture « client injoignable ». Chaque action est limitée au périmètre de l'utilisateur, tracée
 * dans l'historique de la plainte, et compte comme une action humaine (elle arrête l'escalade en cours).
 */
@Service
@RequiredArgsConstructor
public class SlaActionService {

    /** Erreur métier affichable à l'utilisateur. */
    public static class SlaActionException extends RuntimeException {
        public SlaActionException(String message) {
            super(message);
        }
    }

    private final ClaimSlaRepository repository;
    private final ClaimRepository claimRepository;
    private final ContactAttemptRepository attemptRepository;
    private final SlaBreachReasonRepository reasonRepository;
    private final SlaPerimeter perimeter;
    private final SlaEngine engine;
    private final SlaConfig config;
    private final BusinessTime businessTime;
    private final ClaimEventService claimEventService;

    private Claim visibleClaim(User user, ClaimType type, Long id) {
        Claim claim = claimRepository.findById(id).orElse(null);
        if (claim == null || claim.getType() != type || claim.isDeleted()
                || !perimeter.canSee(perimeter.scopeOf(user), claim)) {
            throw new SlaActionException("Plainte introuvable");
        }
        return claim;
    }

    private ClaimSla counter(ClaimType type, Long id) {
        if (!config.enabled()) {
            throw new SlaActionException("Le suivi des délais n'est pas activé");
        }
        return engine.current(type, id).orElseThrow(() -> new SlaActionException("Aucun suivi SLA pour cette plainte"));
    }

    private void log(Claim claim, ClaimEventType event, User user, String metadata) {
        SlaSummaryCache.clear(); // l'action change les chiffres du résumé
        claimEventService.log(claim.getId(), claim.getCodeClient(), claim.getType(), event,
                user.getFirstandlastname(), user.getEmail(), metadata);
    }

    // --- Attente du client -------------------------------------------------------------------------

    @Transactional
    public ClaimSla waitCustomer(User user, ClaimType type, Long id, String reason) {
        Claim claim = visibleClaim(user, type, id);
        ClaimSla s = counter(type, id);
        if (type == ClaimType.SUGGESTION) {
            throw new SlaActionException("Non applicable à une suggestion");
        }
        if (s.getPhase() != SlaPhase.OPEN || s.getResolvedAt() != null) {
            throw new SlaActionException("Cette plainte ne peut pas être mise en attente du client");
        }
        LocalDateTime now = LocalDateTime.now();
        s.setPhase(SlaPhase.PAUSED);
        s.setPausedAt(now);
        s.setLastHumanActionAt(now);
        s.setNextEscalationAt(null);
        s.setUpdatedAt(now);
        engine.recompute(s);
        repository.save(s);
        log(claim, ClaimEventType.WAITING_CUSTOMER, user, reason == null ? "" : reason);
        return s;
    }

    @Transactional
    public ClaimSla resume(User user, ClaimType type, Long id) {
        Claim claim = visibleClaim(user, type, id);
        ClaimSla s = counter(type, id);
        if (s.getPhase() != SlaPhase.PAUSED || s.getPausedAt() == null) {
            throw new SlaActionException("Cette plainte n'est pas en attente du client");
        }
        LocalDateTime now = LocalDateTime.now();
        long paused = businessTime.between(s.isBusinessTime(), s.getPausedAt(), now);
        // les échéances internes reculent de la durée de la pause ; l'échéance réglementaire ne bouge pas
        s.setPausedMinutes(s.getPausedMinutes() + paused);
        s.setResolutionDueAt(businessTime.add(s.isBusinessTime(), s.getResolutionDueAt(), paused));
        if (s.getTakeoverAt() == null && s.getTakeoverDueAt() != null) {
            s.setTakeoverDueAt(businessTime.add(s.isBusinessTime(), s.getTakeoverDueAt(), paused));
        }
        s.setPausedAt(null);
        s.setPhase(SlaPhase.OPEN);
        // les seuils sont recalculés avec le nouveau délai
        LocalDateTime due = SlaEngine.currentDue(s);
        s.setReminder1Sent(isPast(engine.reminderAt(s, s.getReminder1Pct()), now));
        s.setReminder2Sent(isPast(engine.reminderAt(s, s.getReminder2Pct()), now));
        s.setBreachNotified(due != null && now.isAfter(due));
        engine.humanAction(s, now);
        s.setUpdatedAt(now);
        engine.recompute(s);
        repository.save(s);
        log(claim, ClaimEventType.SLA_RESUMED, user, paused + " min");
        return s;
    }

    private static boolean isPast(LocalDateTime t, LocalDateTime now) {
        return t != null && !now.isBefore(t);
    }

    // --- Justification d'un retard ------------------------------------------------------------------

    @Transactional
    public ClaimSla justify(User user, ClaimType type, Long id, Long reasonId, String comment) {
        Claim claim = visibleClaim(user, type, id);
        ClaimSla s = counter(type, id);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = SlaEngine.currentDue(s);
        boolean late = due != null && (s.getResolvedAt() != null && s.getPhase() == SlaPhase.DONE
                ? s.getResolvedAt().isAfter(s.getResolutionDueAt()) : now.isAfter(due));
        if (!late && !s.isRegulatoryBreached()) {
            throw new SlaActionException("Cette plainte n'est pas en retard");
        }
        SlaBreachReason reason = reasonRepository.findById(reasonId)
                .filter(r -> r.isActive() && "RETARD".equals(r.getKind()))
                .orElseThrow(() -> new SlaActionException("Motif de retard invalide"));
        s.setBreachReasonId(reason.getId());
        s.setBreachComment(comment);
        s.setJustifiedById(user.getId());
        s.setJustifiedAt(now);
        if (s.getPhase() == SlaPhase.OPEN) {
            engine.humanAction(s, now);
        }
        s.setUpdatedAt(now);
        engine.recompute(s);
        repository.save(s);
        log(claim, ClaimEventType.SLA_JUSTIFIED, user, reason.getLibelle());
        return s;
    }

    // --- Message d'attente au client ----------------------------------------------------------------------------

    private final SlaNotifier notifier;
    private final SlaEventRepository eventRepository;

    /** Envoie le message d'attente (réglage « proposé ») : seulement pour une réclamation en retard, une fois. */
    @Transactional
    public void sendWaitingMessage(User user, Long claimId) {
        Claim claim = visibleClaim(user, ClaimType.CLAIM, claimId);
        ClaimSla s = counter(ClaimType.CLAIM, claimId);
        if ("NONE".equals(config.customerWaitingMessage())) {
            throw new SlaActionException("L'envoi d'un message d'attente n'est pas activé");
        }
        LocalDateTime due = SlaEngine.currentDue(s);
        LocalDateTime now = LocalDateTime.now();
        if (s.getPhase() != SlaPhase.OPEN || due == null || !now.isAfter(due)) {
            throw new SlaActionException("Cette réclamation n'est pas en retard");
        }
        String dedup = s.getId() + ":CLIENT_MSG:" + (due.atZone(java.time.ZoneId.systemDefault()).toEpochSecond() / 60);
        if (eventRepository.existsByDedupKey(dedup)) {
            throw new SlaActionException("Un message d'attente a déjà été envoyé pour ce retard");
        }
        if (!notifier.clientWaitingMessage(claim)) {
            throw new SlaActionException("Aucun e-mail ni téléphone renseigné pour ce client");
        }
        eventRepository.save(com.sicmagroup.gpr.sla.domain.SlaEvent.builder().claimSlaId(s.getId()).claimId(claimId)
                .targetType(ClaimType.CLAIM).eventType("CLIENT_WAITING_MESSAGE").dedupKey(dedup)
                .detail("Message d'attente").createdAt(now).build());
        log(claim, ClaimEventType.MAIL_SENT_CLIENT, user, "Message d'attente (retard)");
    }

    // --- Tentatives de contact et clôture « client injoignable » -----------------------------------------

    @Transactional
    public ContactAttempt addContactAttempt(User user, Long claimId, String channel, boolean reached, String comment) {
        Claim claim = visibleClaim(user, ClaimType.CLAIM, claimId);
        ClaimSla s = counter(ClaimType.CLAIM, claimId);
        if (s.getResolvedAt() == null) {
            throw new SlaActionException("La mesure de satisfaction ne concerne que les réclamations traitées");
        }
        if (!List.of("APPEL", "SMS", "WHATSAPP", "EMAIL", "VISITE").contains(channel)) {
            throw new SlaActionException("Canal de contact invalide");
        }
        ContactAttempt attempt = attemptRepository.save(ContactAttempt.builder()
                .claimId(claimId).channel(channel).reached(reached).comment(comment).userId(user.getId())
                .createdAt(LocalDateTime.now()).build());
        log(claim, ClaimEventType.CONTACT_ATTEMPT, user, channel + (reached ? " (joint)" : " (non joint)"));
        return attempt;
    }

    public List<ContactAttempt> contactAttempts(User user, Long claimId) {
        visibleClaim(user, ClaimType.CLAIM, claimId);
        return attemptRepository.findByClaimIdOrderByCreatedAtDesc(claimId);
    }

    /** Clôture sans mesure : réservée au Pilote, après le nombre de tentatives sans succès paramétré. */
    @Transactional
    public ClaimSla closeUnreachable(User user, Long claimId, Long reasonId) {
        if (user.getAdditionalrole() != Role.PILOTE) {
            throw new SlaActionException("Seul le Pilote peut clôturer une plainte comme « client injoignable »");
        }
        Claim claim = visibleClaim(user, ClaimType.CLAIM, claimId);
        ClaimSla s = counter(ClaimType.CLAIM, claimId);
        if (s.getResolvedAt() == null || s.getPhase() != SlaPhase.OPEN) {
            throw new SlaActionException("Cette plainte n'attend pas de mesure de satisfaction");
        }
        LocalDateTime now = LocalDateTime.now();
        long failed = attemptRepository.countByClaimIdAndReachedFalseAndCreatedAtAfter(claimId,
                now.minusDays(config.unreachableDays()));
        if (failed < config.unreachableAttempts()) {
            throw new SlaActionException("Il faut au moins " + config.unreachableAttempts()
                    + " tentative(s) sans succès sur " + config.unreachableDays() + " jours (actuellement " + failed
                    + ")");
        }
        String reasonLabel = "Client injoignable";
        if (reasonId != null) {
            SlaBreachReason reason = reasonRepository.findById(reasonId)
                    .filter(r -> r.isActive() && "NON_MESURE".equals(r.getKind()))
                    .orElseThrow(() -> new SlaActionException("Motif invalide"));
            s.setBreachReasonId(reason.getId());
            reasonLabel = reason.getLibelle();
        }
        s.setPhase(SlaPhase.DONE);
        s.setClosedAt(now);
        s.setClosedUnreachable(true);
        s.setNextEscalationAt(null);
        s.setUpdatedAt(now);
        engine.recompute(s);
        repository.save(s);
        log(claim, ClaimEventType.CLOSED_UNREACHABLE, user, reasonLabel);
        return s;
    }
}
