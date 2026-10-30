package com.sicmagroup.gpr.service.report.carte;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.servicePoint.ServicePointServiceImpl;

import lombok.RequiredArgsConstructor;

// Périmètre d'agences du rapport Global pour la carte : MÊMES règles que ReportController
// (isElevatedForReport, computeScopedServicePointIds, enforceRaScope). Ces méthodes y sont privées et
// ReportController ne devait pas être modifié : la logique est reprise à l'identique, en réutilisant
// ServicePointServiceImpl.getAllDescendants. À factoriser le jour où ReportController pourra être touché.
@Component
@RequiredArgsConstructor
public class PerimetreRapport {
	private final AuthenticationServiceImpl authService;
	private final ServicePointServiceImpl spServiceImpl;

	// H6, PILOTE ou DE : tout le périmètre (copie de ReportController.isElevatedForReport).
	public boolean estEleve(User user) {
		boolean hasH6 = user.getPoste() != null && user.getPoste().getHabilitations() != null
				&& Arrays.stream(user.getPoste().getHabilitations().split(","))
						.map(String::trim)
						.anyMatch(h -> h.equals("H6"));
		Role role = user.getAdditionalrole();
		return hasH6 || role == Role.PILOTE || role == Role.DE;
	}

	public User utilisateurConnecteOuNull() {
		try {
			UserDetails details = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			return authService.getByEmail(details.getUsername());
		} catch (Exception e) {
			return null;
		}
	}

	// Sa propre agence ; un RA y ajoute toutes les agences rattachées (copie de computeScopedServicePointIds).
	public List<Long> agencesDuPerimetre(User user) {
		ServicePoint servicePoint = user.getServicePoint();
		List<Long> ids = new ArrayList<>();
		ids.add(servicePoint.getId());
		if (user.isRa()) {
			for (ServicePoint descendant : spServiceImpl.getAllDescendants(servicePoint.getId())) {
				ids.add(descendant.getId());
			}
		}
		return ids;
	}

	// null = aucune restriction (élevé) ; sinon la liste imposée, comme enforceRaScope / buildRaScopedFilterOrNull.
	public List<Long> agencesImposeesOuNull() {
		User user = utilisateurConnecteOuNull();
		if (user == null || estEleve(user)) {
			return null;
		}
		return agencesDuPerimetre(user);
	}
}
