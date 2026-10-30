package com.sicmagroup.gpr.api.report;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.LicenceControl;
import com.sicmagroup.gpr.service.report.carte.CarteRapportService;
import com.sicmagroup.gpr.utils.Utils;

import lombok.RequiredArgsConstructor;

// Cartographie du rapport Global (nouvel endpoint, protégé comme /api/v1/report/** : H11, PILOTE, DE).
// Corps : le même FilterRequest que /report/filtered, ou rien quand la page est ouverte sans filtre.
@RestController
@RequestMapping("/api/v1/report/carte")
@RequiredArgsConstructor
public class CarteRapportController {
	private final CarteRapportService carteService;

	// Adresse du fond de carte : variable d'environnement GPR_CARTE_TUILES_URL, ou propriété gpr.carte.tuiles-url.
	// Par défaut le serveur public OpenStreetMap, réservé à la phase pilote (voir NOTES_CONSTATS.md du frontend).
	@Value("${GPR_CARTE_TUILES_URL:${gpr.carte.tuiles-url:https://tile.openstreetmap.org/{z}/{x}/{y}.png}}")
	private String tuilesUrl;

	@Value("${GPR_CARTE_ATTRIBUTION:${gpr.carte.attribution:© contributeurs OpenStreetMap}}")
	private String attribution;

	@PostMapping
	public ResponseEntity<Object> carte(@RequestBody(required = false) FilterRequest request) {
		ApiResponseDto licence = Utils.verifyLicence();
		if (!(licence.isStatus() && licence.getContent() instanceof LicenceControl lc && lc.isActif())) {
			return ResponseEntity.ok(licence);
		}
		Map<String, Object> reponse = new LinkedHashMap<>();
		reponse.put("tuilesUrl", tuilesUrl);
		reponse.put("attribution", attribution);
		reponse.put("agences", carteService.agences(request));
		return ResponseEntity.ok(reponse);
	}
}
