package com.sicmagroup.gpr.sla.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sicmagroup.gpr.domain.enumeration.ClaimEventType;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.GravityLevel;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;
import com.sicmagroup.gpr.sla.domain.ClaimSla;
import com.sicmagroup.gpr.sla.domain.SlaCycleKind;
import com.sicmagroup.gpr.sla.domain.SlaOwnerLevel;
import com.sicmagroup.gpr.sla.domain.SlaPhase;
import com.sicmagroup.gpr.sla.domain.SlaPolicy;
import com.sicmagroup.gpr.sla.repository.ClaimSlaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Moteur SLA : tient à jour le compteur de chaque plainte quand son cycle de vie change (événements
 * claim-events). Il ne traite jamais une plainte à la place des humains : il ne fait que mesurer.
 * Les rappels, alertes et remontées sont envoyés par {@link SlaScheduler}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlaEngine {

    private final ClaimSlaRepository repository;
    private final ClaimRepository claimRepository;
    private final SuggestionRepository suggestionRepository;
    private final SlaPolicyService policyService;
    private final BusinessTime businessTime;
    private final SlaConfig config;
    private final SlaHierarchy hierarchy;

    // ---------------------------------------------------------------------------------------------
    // Point d'entrée : un événement du cycle de vie d'une plainte
    // ---------------------------------------------------------------------------------------------

    /** Appelé après l'écriture d'un événement dans claim-events (hors transaction de la plainte). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEvent(Long claimId, ClaimType type, ClaimEventType event) {
        if (!config.enabled() || claimId == null || isSlaOwnEvent(event)) {
            return;
        }
        if (type == ClaimType.SUGGESTION) {
            onSuggestionEvent(claimId, event);
            return;
        }
        Claim claim = claimRepository.findById(claimId).orElse(null);
        if (claim == null) {
            return;
        }
        ClaimType realType = claim.getType();
        if (claim.isDeleted()) {
            cancelAll(realType, claimId);
            return;
        }
        // conversion réclamation <-> dénonciation : l'ancien compteur est remplacé
        if (event == ClaimEventType.CONVERTED) {
            cancelAll(type == realType ? opposite(realType) : type, claimId);
        }
        // tant que la reprise n'est pas faite, un compteur créé pour une plainte ancienne ne déclenche aucune alerte
        boolean beforeBackfill = !config.getBoolean("sla.backfill_done", false);
        ClaimSla sla = current(realType, claimId).orElseGet(() -> open(claim, beforeBackfill));
        apply(sla, claim, event, LocalDateTime.now());
        recompute(sla);
        repository.save(sla);
        SlaSummaryCache.clear();
    }

    /** Événements écrits par le SLA lui-même : ils ne changent pas le compteur (déjà à jour). */
    private static boolean isSlaOwnEvent(ClaimEventType event) {
        return switch (event) {
            case SLA_REMINDER, SLA_BREACH, SLA_STUCK, SLA_REGULATORY_WARNING, SLA_REGULATORY_BREACH,
                    WAITING_CUSTOMER, SLA_RESUMED, SLA_JUSTIFIED, CONTACT_ATTEMPT, CLOSED_UNREACHABLE -> true;
            default -> false;
        };
    }

    private ClaimType opposite(ClaimType t) {
        return t == ClaimType.CLAIM ? ClaimType.DENUNCIACION : ClaimType.CLAIM;
    }

    public Optional<ClaimSla> current(ClaimType type, Long claimId) {
        return repository.findFirstByTargetTypeAndClaimIdAndPhaseNotOrderByCycleDesc(type, claimId,
                SlaPhase.CANCELLED);
    }

    private void cancelAll(ClaimType type, Long claimId) {
        for (ClaimSla s : repository.findByTargetTypeAndClaimId(type, claimId)) {
            if (s.getPhase() != SlaPhase.CANCELLED) {
                s.setPhase(SlaPhase.CANCELLED);
                s.setNextCheckAt(null);
                s.setUpdatedAt(LocalDateTime.now());
                repository.save(s);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Transitions
    // ---------------------------------------------------------------------------------------------

    private void apply(ClaimSla s, Claim claim, ClaimEventType event, LocalDateTime now) {
        switch (event) {
            case SAVED -> {
                if (s.getRegisteredAt() == null) {
                    s.setRegisteredAt(now);
                    SlaPolicy policy = policyFor(claim);
                    s.setTakeoverDueAt(businessTime.add(s.isBusinessTime(), now, takeoverMinutes(claim, policy)));
                }
                humanAction(s, now);
            }
            case AFFECTED -> {
                takeover(s, now);
                setOwner(s, claim.getTreatmentAffectedTo(), claim);
                humanAction(s, now);
            }
            case TRANSMITTED -> {
                takeover(s, now);
                setOwner(s, claim.getTransmittedTo(), claim);
                humanAction(s, now);
            }
            case AUTO_TRANSMITTED -> {
                // remontée automatique : ce n'est pas une action humaine
                setOwner(s, claim.getTransmittedTo(), claim);
            }
            case SOLUTION_PROPOSED -> {
                takeover(s, now);
                if (s.getPhase() == SlaPhase.SUSPENDED && claim.getStatus() != ClaimStatus.LITIGATION
                        && claim.getStatus() != ClaimStatus.CLASSED) {
                    s.setPhase(SlaPhase.OPEN);
                }
                humanAction(s, now);
            }
            case SESSION_STARTED -> {
                takeover(s, now);
                humanAction(s, now);
            }
            case REJECTED -> {
                // solution désapprouvée : la plainte redevient à traiter
                s.setResolvedAt(null);
                s.setClosureDueAt(null);
                s.setBreachNotified(false);
                s.setBreachNotifiedAt(null);
                s.setReminder1Sent(false);
                s.setReminder2Sent(false);
                if (s.getPhase() == SlaPhase.DONE) {
                    s.setPhase(SlaPhase.OPEN);
                    s.setClosedAt(null);
                }
                humanAction(s, now);
            }
            case APPROVED -> resolve(s, claim, now);
            case SATISFIED -> {
                s.setPhase(SlaPhase.DONE);
                s.setClosedAt(now);
                s.setNextEscalationAt(null);
            }
            case UNSATISFIED, PARTIAL_SATISFIED -> reopenAfterMeasure(s, claim, now);
            case CLASSED, LITIGATION -> {
                s.setPhase(SlaPhase.SUSPENDED);
                s.setNextEscalationAt(null);
            }
            default -> {
                // les autres événements (e-mails, SMS, ajout de contenu) ne changent pas le SLA
            }
        }
        syncClaimData(s, claim);
        s.setUpdatedAt(now);
    }

    /** Recopie dans le compteur les identifiants utiles aux listes (point de service, personnes concernées). */
    public void syncClaimData(ClaimSla s, Claim claim) {
        s.setServicePointId(claim.getServicePoint() == null ? null : claim.getServicePoint().getId());
        s.setCollectorId(claim.getCollector() == null ? null : claim.getCollector().getId());
        s.setAffectedToId(claim.getTreatmentAffectedTo() == null ? null : claim.getTreatmentAffectedTo().getId());
        s.setTransmittedToId(claim.getTransmittedTo() == null ? null : claim.getTransmittedTo().getId());
        s.setClaimDeleted(claim.isDeleted());
    }

    private void takeover(ClaimSla s, LocalDateTime now) {
        if (s.getTakeoverAt() == null) {
            s.setTakeoverAt(now);
        }
    }

    /** Une action humaine arrête l'escalade en cours ; si le dossier est encore en retard, un nouveau délai de grâce démarre. */
    public void humanAction(ClaimSla s, LocalDateTime now) {
        s.setLastHumanActionAt(now);
        LocalDateTime due = currentDue(s);
        if (s.getPhase() == SlaPhase.OPEN && due != null && now.isAfter(due) && config.autoEscalation()) {
            s.setNextEscalationAt(businessTime.add(s.isBusinessTime(), now, s.getGraceMinutes()));
        } else {
            s.setNextEscalationAt(null);
        }
    }

    private void resolve(ClaimSla s, Claim claim, LocalDateTime now) {
        takeover(s, now);
        s.setResolvedAt(now);
        s.setNextEscalationAt(null);
        s.setLastHumanActionAt(now);
        // le délai de clôture démarre : nouveaux compteurs de rappel
        s.setReminder1Sent(false);
        s.setReminder2Sent(false);
        s.setBreachNotified(false);
        s.setBreachNotifiedAt(null);
        s.setStuckNotified(false);
        if (s.getPhase() == SlaPhase.SUSPENDED || s.getPhase() == SlaPhase.PAUSED) {
            s.setPhase(SlaPhase.OPEN);
        }
        if (claim.getType() == ClaimType.DENUNCIACION || s.getClosureMinutes() <= 0) {
            // dénonciation : TREAT vaut clôture
            s.setPhase(SlaPhase.DONE);
            s.setClosedAt(now);
            s.setClosureDueAt(null);
        } else {
            s.setClosureDueAt(businessTime.add(s.isBusinessTime(), now, s.getClosureMinutes()));
            // délai de clôture : le suivi revient aux personnes qui mesurent (agent au bas de l'échelle)
            s.setOwnerUserId(null);
            s.setOwnerLevel(SlaOwnerLevel.AGENT);
        }
        // approbation après la limite réglementaire : marquée hors délai
        if (s.getRegulatoryDueAt() != null && now.isAfter(s.getRegulatoryDueAt())) {
            s.setRegulatoryBreached(true);
            s.setRegulatoryBreachSent(true);
        }
    }

    /** Mesure « non satisfait » ou « partiellement satisfait » : le cycle est fermé, un nouveau cycle démarre. */
    private void reopenAfterMeasure(ClaimSla s, Claim claim, LocalDateTime now) {
        s.setPhase(SlaPhase.DONE);
        s.setClosedAt(now);
        s.setNextCheckAt(null);
        s.setNextEscalationAt(null);
        repository.save(s);

        SlaPolicy policy = policyFor(claim);
        boolean business = policy.isBusinessTime();
        ClaimSla next = baseBuilder(claim, SlaCycleKind.SATISFACTION, s.getCycle() + 1, policy, now).build();
        next.setReceivedAt(now); // le chronomètre du nouveau cycle démarre à la mesure
        next.setRegisteredAt(s.getRegisteredAt());
        next.setTakeoverAt(now);
        syncClaimData(next, claim);
        next.setResolutionMinutes(policy.getReopenMinutes() > 0 ? policy.getReopenMinutes()
                : policy.getResolutionMinutes());
        next.setResolutionDueAt(businessTime.add(business, now, next.getResolutionMinutes()));
        next.setRegulatoryDueAt(s.getRegulatoryDueAt());
        next.setRegulatoryBreached(s.isRegulatoryBreached());
        next.setRegulatoryWarningSent(true);
        next.setRegulatoryBreachSent(true);
        next.setOwnerUserId(s.getOwnerUserId());
        next.setOwnerLevel(s.getOwnerLevel());
        next.setLastHumanActionAt(now);
        recompute(next);
        repository.save(next);
        // l'appelant va enregistrer s une seconde fois, sans conséquence
    }

    // ---------------------------------------------------------------------------------------------
    // Création d'un compteur
    // ---------------------------------------------------------------------------------------------

    /** Délai de prise en charge : celui de l'objet (en heures), s'il est renseigné, sinon celui de la politique. */
    private static long takeoverMinutes(Claim claim, SlaPolicy policy) {
        Objet objet = claim.getObjet();
        if (objet != null && objet.getTakeoverHours() != null && objet.getTakeoverHours() > 0) {
            return objet.getTakeoverHours() * 60L;
        }
        return policy.getTakeoverMinutes();
    }

    private SlaPolicy policyFor(Claim claim) {
        Objet objet = claim.getObjet();
        GravityLevel risk = objet == null ? null : objet.getRisqueLevel();
        return policyService.policyFor(claim.getType(), risk);
    }

    private ClaimSla.ClaimSlaBuilder baseBuilder(Claim claim, SlaCycleKind kind, int cycle, SlaPolicy policy,
            LocalDateTime now) {
        return ClaimSla.builder()
                .targetType(claim.getType())
                .claimId(claim.getId())
                .cycle(cycle)
                .cycleKind(kind)
                .phase(SlaPhase.OPEN)
                .closureMinutes(policy.getClosureMinutes())
                .reminder1Pct(policy.getReminder1Pct())
                .reminder2Pct(policy.getReminder2Pct())
                .graceMinutes(policy.getGraceMinutes())
                .businessTime(policy.isBusinessTime())
                .createdAt(now)
                .updatedAt(now);
    }

    /**
     * Ouvre le compteur d'une plainte à partir de son état actuel. Si retroactive est vrai (reprise de
     * plaintes déjà ouvertes), aucune alerte n'est déclenchée pour ce qui est déjà dépassé.
     */
    @Transactional
    public ClaimSla open(Claim claim, boolean retroactive) {
        LocalDateTime now = LocalDateTime.now();
        ClaimType type = claim.getType();
        SlaPolicy policy = policyFor(claim);
        boolean business = policy.isBusinessTime();
        Objet objet = claim.getObjet();

        LocalDateTime received = claim.getReceiptDateTime() != null ? claim.getReceiptDateTime()
                : claim.getCreatedAt() != null ? claim.getCreatedAt() : now;
        int cycle = repository.findByTargetTypeAndClaimId(type, claim.getId()).size() + 1;

        ClaimSla s = baseBuilder(claim, SlaCycleKind.MAIN, cycle, policy, now).build();
        s.setReceivedAt(received);
        int days = objet == null ? 0 : objet.getProcessingTime();
        // le délai de l'objet, s'il est renseigné, prime sur la politique
        s.setResolutionMinutes(days > 0 ? (int) (days * (business ? businessTime.workdayMinutes() : 1440))
                : policy.getResolutionMinutes());
        s.setResolutionDueAt(businessTime.add(business, received, s.getResolutionMinutes()));
        s.setRegulatoryDueAt(received.plusDays(config.regulatoryDays()));

        if (claim.getStatus() != ClaimStatus.TEMP_SAVED) {
            LocalDateTime registered = claim.getCreatedAt() != null ? claim.getCreatedAt() : now;
            s.setRegisteredAt(registered);
            s.setTakeoverDueAt(businessTime.add(business, registered, takeoverMinutes(claim, policy)));
        }

        User owner = claim.getTreatmentAffectedTo() != null ? claim.getTreatmentAffectedTo()
                : claim.getTransmittedTo();
        if (owner != null) {
            s.setTakeoverAt(claim.getAffectedAt() != null ? claim.getAffectedAt() : now);
        }
        setOwner(s, owner, claim);
        syncClaimData(s, claim);
        s.setLastHumanActionAt(now);

        applyCurrentStatus(s, claim, business, now);

        if (retroactive) {
            suppressPast(s, now);
        } else {
            suppressPastReminders(s, now);
        }
        recompute(s);
        return repository.save(s);
    }

    /**
     * Plainte qui arrive en retard (saisie hors ligne puis synchronisée, robot...) : les rappels déjà passés ne
     * sont pas envoyés d'un coup ; un dépassement éventuel reste signalé une seule fois.
     */
    private void suppressPastReminders(ClaimSla s, LocalDateTime now) {
        if (currentDue(s) == null) {
            return;
        }
        LocalDateTime r1 = reminderAt(s, s.getReminder1Pct());
        LocalDateTime r2 = reminderAt(s, s.getReminder2Pct());
        if (r1 != null && !now.isBefore(r1)) {
            s.setReminder1Sent(true);
        }
        if (r2 != null && !now.isBefore(r2)) {
            s.setReminder2Sent(true);
        }
    }

    /** Aligne un compteur neuf sur le statut actuel de la plainte (utile pour la reprise). */
    private void applyCurrentStatus(ClaimSla s, Claim claim, boolean business, LocalDateTime now) {
        ClaimStatus status = claim.getStatus();
        if (status == null) {
            return;
        }
        LocalDateTime ref = claim.getUpdatedAt() != null ? claim.getUpdatedAt() : now;
        switch (status) {
            case TREAT -> {
                s.setResolvedAt(ref);
                if (claim.getType() == ClaimType.DENUNCIACION || s.getClosureMinutes() <= 0) {
                    s.setPhase(SlaPhase.DONE);
                    s.setClosedAt(ref);
                } else {
                    s.setClosureDueAt(businessTime.add(business, ref, s.getClosureMinutes()));
                }
            }
            case SATISFIED -> {
                s.setResolvedAt(ref);
                s.setPhase(SlaPhase.DONE);
                s.setClosedAt(ref);
            }
            case UNSATISFIED, PARTIAL_SATISFIED -> {
                // nouveau cycle « assurance satisfaction » : repart de la dernière mise à jour
                s.setCycleKind(SlaCycleKind.SATISFACTION);
                int reopen = policyFor(claim).getReopenMinutes();
                s.setResolutionMinutes(reopen > 0 ? reopen : s.getResolutionMinutes());
                s.setResolutionDueAt(businessTime.add(business, ref, s.getResolutionMinutes()));
            }
            case CLASSED, LITIGATION -> s.setPhase(SlaPhase.SUSPENDED);
            case WAITING_CUSTOMER -> {
                s.setPhase(SlaPhase.PAUSED);
                s.setPausedAt(ref);
            }
            default -> {
            }
        }
    }

    /** Reprise : ce qui est déjà dépassé ne déclenche ni rappel, ni alerte, ni remontée. */
    private void suppressPast(ClaimSla s, LocalDateTime now) {
        LocalDateTime due = currentDue(s);
        if (due != null) {
            LocalDateTime r1 = reminderAt(s, s.getReminder1Pct());
            LocalDateTime r2 = reminderAt(s, s.getReminder2Pct());
            if (r1 != null && !now.isBefore(r1)) {
                s.setReminder1Sent(true);
            }
            if (r2 != null && !now.isBefore(r2)) {
                s.setReminder2Sent(true);
            }
            if (now.isAfter(due)) {
                s.setBreachNotified(true);
                s.setBreachNotifiedAt(now);
                s.setStuckNotified(true);
            }
        }
        if (s.getTakeoverDueAt() != null && now.isAfter(s.getTakeoverDueAt())) {
            s.setTakeoverBreachNotified(true);
        }
        if (s.getRegulatoryDueAt() != null) {
            if (!now.isBefore(s.getRegulatoryDueAt().minusDays(config.regulatoryWarningDays()))) {
                s.setRegulatoryWarningSent(true);
            }
            if (now.isAfter(s.getRegulatoryDueAt())) {
                s.setRegulatoryBreachSent(true);
                s.setRegulatoryBreached(s.getResolvedAt() == null);
            }
        }
        s.setNextEscalationAt(null);
    }

    private void setOwner(ClaimSla s, User owner, Claim claim) {
        s.setOwnerUserId(owner == null ? null : owner.getId());
        s.setOwnerLevel(owner == null ? SlaOwnerLevel.RA : hierarchy.levelOf(owner, claim));
    }

    // ---------------------------------------------------------------------------------------------
    // Échéances et prochain contrôle
    // ---------------------------------------------------------------------------------------------

    /** Échéance en vigueur : résolution tant que la solution n'est pas approuvée, puis clôture. */
    public static LocalDateTime currentDue(ClaimSla s) {
        return s.getResolvedAt() == null ? s.getResolutionDueAt() : s.getClosureDueAt();
    }

    /** Date à laquelle un seuil (en %) du délai en vigueur est atteint. */
    public LocalDateTime reminderAt(ClaimSla s, int pct) {
        if (s.getResolvedAt() == null) {
            if (s.getReceivedAt() == null) {
                return null;
            }
            long base = (long) s.getResolutionMinutes() * pct / 100 + s.getPausedMinutes();
            return businessTime.add(s.isBusinessTime(), s.getReceivedAt(), base);
        }
        if (s.getClosureDueAt() == null) {
            return null;
        }
        long base = (long) s.getClosureMinutes() * pct / 100;
        return businessTime.add(s.isBusinessTime(), s.getResolvedAt(), base);
    }

    /** Recalcule la prochaine date à laquelle la tâche planifiée doit regarder ce compteur. */
    public void recompute(ClaimSla s) {
        s.setDueAt(currentDue(s));
        if (s.getPhase() != SlaPhase.OPEN && s.getPhase() != SlaPhase.PAUSED) {
            s.setNextCheckAt(null);
            return;
        }
        boolean open = s.getPhase() == SlaPhase.OPEN;
        LocalDateTime[] next = { null };
        java.util.function.Consumer<LocalDateTime> consider = t -> {
            if (t != null && (next[0] == null || t.isBefore(next[0]))) {
                next[0] = t;
            }
        };
        if (open && s.getTakeoverAt() == null && s.getTakeoverDueAt() != null && !s.isTakeoverBreachNotified()) {
            consider.accept(s.getTakeoverDueAt());
        }
        LocalDateTime due = currentDue(s);
        if (open && due != null) {
            if (!s.isReminder1Sent()) {
                consider.accept(reminderAt(s, s.getReminder1Pct()));
            }
            if (!s.isReminder2Sent()) {
                consider.accept(reminderAt(s, s.getReminder2Pct()));
            }
            if (!s.isBreachNotified()) {
                consider.accept(due);
            }
            if (s.getNextEscalationAt() != null) {
                consider.accept(s.getNextEscalationAt());
            }
        }
        if (s.getRegulatoryDueAt() != null && s.getResolvedAt() == null) {
            if (!s.isRegulatoryWarningSent()) {
                consider.accept(s.getRegulatoryDueAt().minusDays(config.regulatoryWarningDays()));
            }
            if (!s.isRegulatoryBreachSent()) {
                consider.accept(s.getRegulatoryDueAt());
            }
        }
        s.setNextCheckAt(next[0]);
    }

    // ---------------------------------------------------------------------------------------------
    // Suggestions : un seul délai de réponse, sans escalade
    // ---------------------------------------------------------------------------------------------

    private void onSuggestionEvent(Long id, ClaimEventType event) {
        Suggestion suggestion = suggestionRepository.findById(id).orElse(null);
        if (suggestion == null) {
            return;
        }
        ClaimSla s = current(ClaimType.SUGGESTION, id).orElseGet(() -> openSuggestion(suggestion, false));
        LocalDateTime now = LocalDateTime.now();
        if (event == ClaimEventType.APPROVED || event == ClaimEventType.REJECTED) {
            s.setResolvedAt(now);
            s.setPhase(SlaPhase.DONE);
            s.setClosedAt(now);
        }
        s.setUpdatedAt(now);
        recompute(s);
        repository.save(s);
    }

    @Transactional
    public ClaimSla openSuggestion(Suggestion suggestion, boolean retroactive) {
        LocalDateTime now = LocalDateTime.now();
        SlaPolicy policy = policyService.policyFor(ClaimType.SUGGESTION, GravityLevel.MOYEN);
        LocalDateTime received = suggestion.getReceiptDateTime() != null ? suggestion.getReceiptDateTime()
                : suggestion.getCreatedAt() != null ? suggestion.getCreatedAt() : now;
        ClaimSla s = ClaimSla.builder()
                .targetType(ClaimType.SUGGESTION)
                .claimId(suggestion.getId())
                .cycle(1)
                .cycleKind(SlaCycleKind.MAIN)
                .phase(SlaPhase.OPEN)
                .receivedAt(received)
                .registeredAt(received)
                .resolutionMinutes(policy.getResolutionMinutes())
                .reminder1Pct(policy.getReminder1Pct())
                .reminder2Pct(policy.getReminder2Pct())
                .graceMinutes(policy.getGraceMinutes())
                .businessTime(policy.isBusinessTime())
                .ownerLevel(SlaOwnerLevel.RA)
                .servicePointId(suggestion.getServiceIndexe() == null ? null : suggestion.getServiceIndexe().getId())
                .collectorId(suggestion.getCollecteur() == null ? null : suggestion.getCollecteur().getId())
                .affectedToId(suggestion.getTraiteur() == null ? null : suggestion.getTraiteur().getId())
                .claimDeleted(suggestion.isDeleted())
                .createdAt(now)
                .updatedAt(now)
                .build();
        s.setResolutionDueAt(businessTime.add(s.isBusinessTime(), received, s.getResolutionMinutes()));
        if (suggestion.getStatus() == ClaimStatus.TREAT) {
            s.setResolvedAt(suggestion.getTreatAt() != null ? suggestion.getTreatAt() : now);
            s.setPhase(SlaPhase.DONE);
            s.setClosedAt(s.getResolvedAt());
        } else if (suggestion.getStatus() == ClaimStatus.TEMP_SAVED) {
            s.setRegisteredAt(null);
        }
        if (retroactive) {
            suppressPast(s, now);
        }
        recompute(s);
        return repository.save(s);
    }

    /** Aligne les compteurs sur les suppressions et restaurations faites depuis le dernier passage. */
    @Transactional
    public void syncDeletions() {
        repository.markDeleted();
        repository.markRestored();
    }

    // ---------------------------------------------------------------------------------------------
    // Reprise et rattrapage : crée les compteurs manquants (plaintes existantes, hors ligne, robot...)
    // ---------------------------------------------------------------------------------------------

    /** Crée jusqu'à batch compteurs manquants. Renvoie le nombre de compteurs créés. */
    @Transactional
    public int reconcileMissing(int batch, boolean retroactive) {
        int created = 0;
        List<Long> claimIds = repository.findClaimIdsWithoutSla(
                List.of(ClaimType.CLAIM, ClaimType.DENUNCIACION), PageRequest.of(0, batch));
        for (Long id : claimIds) {
            Claim claim = claimRepository.findById(id).orElse(null);
            if (claim != null && !claim.isDeleted()) {
                open(claim, retroactive);
                created++;
            }
        }
        List<Long> suggestionIds = repository.findSuggestionIdsWithoutSla(PageRequest.of(0, batch));
        for (Long id : suggestionIds) {
            Suggestion suggestion = suggestionRepository.findById(id).orElse(null);
            if (suggestion != null && !suggestion.isDeleted()) {
                openSuggestion(suggestion, retroactive);
                created++;
            }
        }
        return created;
    }
}
