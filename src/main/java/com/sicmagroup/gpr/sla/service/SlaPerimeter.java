package com.sicmagroup.gpr.sla.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.service.servicePoint.ServicePointServiceImpl;

import lombok.RequiredArgsConstructor;

/**
 * Périmètre de visibilité d'un utilisateur. Le SLA ne crée aucune règle nouvelle : il reprend celle des
 * listes de plaintes (RA = son agence et les agences qui lui sont rattachées, plus ce qui lui est affecté ou
 * transmis ; Pilote, DE et H6 = tout ; agent = ses dossiers dans son agence, ou affectés/transmis/invités).
 */
@Service
@RequiredArgsConstructor
public class SlaPerimeter {

    private final UserRepository userRepository;
    private final ServicePointServiceImpl servicePointService;

    /**
     * all : voit tout. spIds : points de service visibles (jamais vide, pour la requête). uid : utilisateur.
     * collectorUid : identifiant à comparer au collecteur (-1 pour un RA : il ne voit pas ce qu'il a saisi
     * hors de son périmètre). collectorSpId : agence de l'agent collecteur. guestClaimIds : sessions où il est invité.
     */
    public record Scope(boolean all, List<Long> spIds, Long uid, Long collectorUid, Long collectorSpId,
            List<Long> guestClaimIds) {
        public Scope(boolean all, List<Long> spIds, Long uid, Long collectorUid) {
            this(all, spIds, uid, collectorUid, null, List.of());
        }
    }

    /** L'utilisateur connecté, ou null. */
    public User currentUser() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails details) {
                return userRepository.findByEmailAndIsDeleted(details.getUsername(), false).orElse(null);
            }
        } catch (Exception e) {
            // pas d'utilisateur connecté (tâche planifiée...)
        }
        return null;
    }

    public Scope scopeOf(User user) {
        if (user == null) {
            return new Scope(false, List.of(-1L), -1L, -1L, null, List.of());
        }
        Role role = user.getAdditionalrole();
        if (role == Role.PILOTE || role == Role.DE) {
            return new Scope(true, List.of(-1L), user.getId(), user.getId(), null, List.of());
        }
        List<Long> guestIds = new ArrayList<>();
        if (user.getChatsGuest() != null) {
            for (var chat : user.getChatsGuest()) {
                if (chat != null && chat.getClaim() != null && chat.getClaim().getId() != null) {
                    guestIds.add(chat.getClaim().getId());
                }
            }
        }
        if (user.isRa() && user.getServicePoint() != null) {
            ServicePoint own = user.getServicePoint();
            List<Long> ids = new ArrayList<>();
            ids.add(own.getId());
            List<ServicePoint> direct = servicePointService.getByDirectionId(own.getId());
            if (direct != null) {
                for (ServicePoint sp : direct) {
                    if (sp.getId() != null && !ids.contains(sp.getId())) {
                        ids.add(sp.getId());
                    }
                }
            }
            List<ServicePoint> descendants = servicePointService.getAllDescendants(own.getId());
            if (descendants != null) {
                for (ServicePoint sp : descendants) {
                    if (sp.getId() != null && !ids.contains(sp.getId())) {
                        ids.add(sp.getId());
                    }
                }
            }
            return new Scope(false, ids, user.getId(), -1L, null, guestIds);
        }
        if (user.getPoste() != null && user.getPoste().getHabilitations() != null && user.canAffectTreatment()) {
            return new Scope(true, List.of(-1L), user.getId(), user.getId(), null, List.of());
        }
        Long ownSpId = user.getServicePoint() != null ? user.getServicePoint().getId() : null;
        return new Scope(false, List.of(-1L), user.getId(), user.getId(), ownSpId, guestIds);
    }

    /** Vrai si la plainte est dans le périmètre (même règle que la requête de liste). */
    public boolean canSee(Scope scope, Claim claim) {
        if (scope.all()) {
            return true;
        }
        Long uid = scope.uid();
        if (claim.getServicePoint() != null && scope.spIds().contains(claim.getServicePoint().getId())) {
            return true;
        }
        if (claim.getTreatmentAffectedTo() != null && claim.getTreatmentAffectedTo().getId().equals(uid)) {
            return true;
        }
        if (claim.getTransmittedTo() != null && claim.getTransmittedTo().getId().equals(uid)) {
            return true;
        }
        if (claim.getId() != null && scope.guestClaimIds() != null && scope.guestClaimIds().contains(claim.getId())) {
            return true;
        }
        if (claim.getCollector() != null && claim.getCollector().getId().equals(scope.collectorUid())) {
            if (scope.collectorSpId() != null && claim.getServicePoint() != null
                    && !scope.collectorSpId().equals(claim.getServicePoint().getId())) {
                return false;
            }
            return true;
        }
        return false;
    }
}
