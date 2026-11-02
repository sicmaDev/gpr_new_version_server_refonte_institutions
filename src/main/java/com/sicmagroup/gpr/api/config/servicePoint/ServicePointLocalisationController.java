package com.sicmagroup.gpr.api.config.servicePoint;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.model.ServicePointLocalisation;
import com.sicmagroup.gpr.repository.ServicePointLocalisationRepository;
import com.sicmagroup.gpr.service.report.carte.LocalisationRegles;

import lombok.RequiredArgsConstructor;

// Localisation des points de service (ville, quartier, latitude, longitude) pour la carte du rapport Global.
// Sous /api/v1/config/** : réservé, comme la configuration des points de service, à l'habilitation H12.
@RestController
@RequestMapping("/api/v1/config/service_point_localisation")
@RequiredArgsConstructor
public class ServicePointLocalisationController {
	private final ServicePointLocalisationRepository repository;

	@GetMapping("/list")
	public ResponseEntity<ApiResponseDto> list() {
		List<ServicePointLocalisation> all = repository.findAll();
		return ResponseEntity.ok(ApiResponseDto.builder().status(true).content(all).build());
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponseDto> update(@PathVariable Long id, @RequestBody ServicePointLocalisation demande) {
		Optional<ServicePointLocalisation> existant = repository.findById(id);
		if (existant.isEmpty()) {
			return ResponseEntity.ok(ApiResponseDto.builder().status(false).content("Point de service introuvable").build());
		}
		String erreur = LocalisationRegles.verifier(demande);
		if (erreur != null) {
			return ResponseEntity.ok(ApiResponseDto.builder().status(false).content(erreur).build());
		}
		ServicePointLocalisation loc = existant.get();
		loc.setVille(texte(demande.getVille()));
		loc.setQuartier(texte(demande.getQuartier()));
		loc.setLatitude(demande.getLatitude());
		loc.setLongitude(demande.getLongitude());
		return ResponseEntity.ok(ApiResponseDto.builder().status(true).content(repository.save(loc)).build());
	}

	private static String texte(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}
}
