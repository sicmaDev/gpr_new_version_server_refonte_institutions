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
 * transmis ; Pilote et DE = tout ; agent = ses dossiers).
 */
@Service
@RequiredArgsConstructor
public class SlaPerimeter {

    private final UserRepository userRepository;
    private final ServicePointServiceImpl servicePointService;

    /**
     * all : voit tout. spIds : points de service visibles (jamais vide, pour la requête). uid : utilisateur.
     * collectorUid : identifiant à comparer au collecteur (-1 pour un RA : il ne voit pas ce qu'il a saisi
     * hors de son périmètre).
     */
    public record Scope(boolean all, List<Long> spIds, Long uid, Long collectorUid) {
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
            return new Scope(false, List.of(-1L), -1L, -1L);
        }
        Role role = user.getAdditionalrole();
        if (role == Role.PILOTE || role == Role.DE) {
            return new Scope(true, List.of(-1L), user.getId(), user.getId());
        }
        if (user.isRa() && user.getServicePoint() != null) {
            ServicePoint own = user.getServicePoint();
            List<Long> ids = new ArrayList<>();
            ids.add(own.getId());
            for (ServicePoint sp : servicePointService.getByDirectionId(own.getId())) {
                ids.add(sp.getId());
            }
            return new Scope(false, ids, user.getId(), -1L);
        }
        return new Scope(false, List.of(-1L), user.getId(), user.getId());
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
        return claim.getCollector() != null && claim.getCollector().getId().equals(scope.collectorUid());
    }
}
