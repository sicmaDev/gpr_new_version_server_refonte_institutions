package com.sicmagroup.gpr.sla.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ServicePointRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.sla.domain.SlaOwnerLevel;

import lombok.RequiredArgsConstructor;

/**
 * Hiérarchie de remontée : agent -> RA de l'agence -> direction de rattachement -> Pilote -> DE.
 * Un niveau absent (agence sans RA, agence sans direction...) est sauté.
 */
@Service
@RequiredArgsConstructor
public class SlaHierarchy {

    private static final int MAX_DEPTH = 10;

    private final UserRepository userRepository;
    private final ServicePointRepository servicePointRepository;

    /** Cible d'une remontée : le nouveau responsable, son niveau, et les personnes à mettre en copie. */
    public record Target(User user, SlaOwnerLevel level, List<User> copy) {
    }

    /** Niveau d'un utilisateur par rapport à une plainte. */
    public SlaOwnerLevel levelOf(User user, Claim claim) {
        if (user == null) {
            return SlaOwnerLevel.AGENT;
        }
        if (user.getAdditionalrole() == Role.DE) {
            return SlaOwnerLevel.DE;
        }
        if (user.getAdditionalrole() == Role.PILOTE) {
            return SlaOwnerLevel.PILOTE;
        }
        if (user.isRa()) {
            ServicePoint claimSp = claim == null ? null : claim.getServicePoint();
            boolean sameAgency = claimSp != null && user.getServicePoint() != null
                    && claimSp.getId().equals(user.getServicePoint().getId());
            return sameAgency ? SlaOwnerLevel.RA : SlaOwnerLevel.DIRECTION;
        }
        return SlaOwnerLevel.AGENT;
    }

    /** RA du point de service de la plainte (premier niveau au-dessus de l'agent). */
    public User agencyRa(Claim claim) {
        ServicePoint sp = claim == null ? null : claim.getServicePoint();
        return sp == null ? null : userRepository.findRaByServicePointId(sp.getId()).orElse(null);
    }

    /** RA de la direction de rattachement du point de service de la plainte. */
    public User directionRa(Claim claim) {
        ServicePoint sp = claim == null ? null : claim.getServicePoint();
        Set<Long> seen = new HashSet<>();
        for (int depth = 0; sp != null && sp.getDirection_id() != null && depth < MAX_DEPTH; depth++) {
            if (!seen.add(sp.getId())) {
                return null; // boucle dans la hiérarchie : on s'arrête
            }
            ServicePoint parent = servicePointRepository.findById(sp.getDirection_id()).orElse(null);
            if (parent == null) {
                return null;
            }
            User ra = userRepository.findRaByServicePointId(parent.getId()).orElse(null);
            if (ra != null) {
                return ra;
            }
            sp = parent; // direction sans RA : on monte d'un cran
        }
        return null;
    }

    /** Personnes qui peuvent mesurer la satisfaction : habilitation H5 du point de service, et les Pilotes. */
    public List<User> measurers(Claim claim) {
        List<User> out = new ArrayList<>();
        ServicePoint sp = claim == null ? null : claim.getServicePoint();
        if (sp != null) {
            for (User u : userRepository.findByServicePoint(sp)) {
                if (!u.isDeleted() && u.getPoste() != null && u.getPoste().getHabilitations() != null
                        && u.canMeasureClaim()) {
                    out.add(u);
                }
            }
        }
        out.addAll(pilotes());
        return out;
    }

    public List<User> pilotes() {
        return userRepository.findByAdditionalroleInAndIsDeleted(List.of(Role.PILOTE), false);
    }

    public List<User> des() {
        return userRepository.findByAdditionalroleInAndIsDeleted(List.of(Role.DE), false);
    }

    /**
     * Prochain responsable : le premier niveau strictement supérieur à currentLevel qui a un utilisateur
     * différent du responsable actuel. Renvoie null s'il n'y a plus de niveau (dernier niveau atteint).
     */
    public Target nextTarget(Claim claim, SlaOwnerLevel currentLevel, Long currentOwnerId) {
        List<Target> ladder = new ArrayList<>();
        User ra = agencyRa(claim);
        if (ra != null) {
            ladder.add(new Target(ra, SlaOwnerLevel.RA, List.of()));
        }
        User direction = directionRa(claim);
        if (direction != null && (ra == null || !direction.getId().equals(ra.getId()))) {
            ladder.add(new Target(direction, SlaOwnerLevel.DIRECTION, List.of()));
        }
        List<User> pilotes = pilotes();
        if (!pilotes.isEmpty()) {
            ladder.add(new Target(pilotes.get(0), SlaOwnerLevel.PILOTE, pilotes));
        }
        List<User> des = des();
        if (!des.isEmpty()) {
            ladder.add(new Target(des.get(0), SlaOwnerLevel.DE, des));
        }
        for (int i = 0; i < ladder.size(); i++) {
            Target t = ladder.get(i);
            boolean higher = t.level().ordinal() > currentLevel.ordinal();
            boolean otherPerson = currentOwnerId == null || !currentOwnerId.equals(t.user().getId());
            if (higher && otherPerson) {
                // le niveau d'après est en copie
                List<User> copy = new ArrayList<>();
                if (i + 1 < ladder.size()) {
                    copy.add(ladder.get(i + 1).user());
                }
                return new Target(t.user(), t.level(), copy);
            }
        }
        return null;
    }
}
