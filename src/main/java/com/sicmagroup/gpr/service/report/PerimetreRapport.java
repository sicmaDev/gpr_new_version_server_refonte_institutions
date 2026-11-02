package com.sicmagroup.gpr.service.report;

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

// Périmètre d'agences du rapport Global, partagé par ReportController (rapport) et la carte.
// Ni RA ni élevé (H6/PILOTE/DE) ne doit voir, sur le Rapport Global (filtré ou non), que son point
// de service et ceux qui lui sont rattachés (fils, petits-fils, etc., parcours récursif comme pour
// l'écran Mesure de satisfaction). Le frontend traite RA et simple titulaire de H11 de la même façon
// (isElevatedUser = H6 || PILOTE || DE, indépendant de isRa()).
@Component
@RequiredArgsConstructor
public class PerimetreRapport {
	private final AuthenticationServiceImpl authService;
	private final ServicePointServiceImpl spServiceImpl;

	// H6, PILOTE ou DE : tout le périmètre.
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

	// Un agent simple (H11, ni RA ni élevé) ne voit que sa PROPRE agence ; les agences rattachées
	// ne sont ajoutées que pour un RA. C'est la seule différence entre les deux profils non élevés.
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

	// null = aucune restriction (élevé, ou pas d'utilisateur connecté) ; sinon les agences imposées,
	// qui remplacent toujours celles envoyées par le client.
	public List<Long> agencesImposeesOuNull() {
		User user = utilisateurConnecteOuNull();
		if (user == null || estEleve(user)) {
			return null;
		}
		return agencesDuPerimetre(user);
	}
}
