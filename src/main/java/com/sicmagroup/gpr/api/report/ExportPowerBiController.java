package com.sicmagroup.gpr.api.report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.service.report.carte.CarteAgence;
import com.sicmagroup.gpr.service.report.carte.CarteRapportService;
import com.sicmagroup.gpr.service.report.powerbi.DemandeExportPowerBi;
import com.sicmagroup.gpr.service.report.powerbi.RapportPowerBi;

import lombok.RequiredArgsConstructor;

// Export Power BI du rapport Global (nouvel endpoint, protégé comme /api/v1/report/** : H11, PILOTE, DE).
// Les chiffres viennent du rapport existant appelé tel quel (/global ou /filtered : mêmes calculs, même périmètre
// d'agences) et de la carte (même service que /report/carte). Le fichier .pbit renvoyé contient ces données agrégées,
// saisies dans ses requêtes Power Query : il s'ouvre sans connexion à GPR.
@RestController
@RequestMapping("/api/v1/report/powerbi")
@RequiredArgsConstructor
public class ExportPowerBiController {
	private final ReportController rapport;
	private final CarteRapportService carteService;
	private final ObjectMapper objectMapper;

	@PostMapping
	@SuppressWarnings("unchecked")
	public ResponseEntity<Object> exporter(@RequestBody DemandeExportPowerBi demande) {
		// La carte reçoit le filtre tel que la page l'envoie : le rapport, lui, modifie le filtre qu'il reçoit.
		FilterRequest filtreCarte = FilterRequest.copieDe(demande.getFiltre());
		ResponseEntity<Object> reponse = demande.getFiltre() == null ? rapport.generatReport() : rapport.generateFilterReport(FilterRequest.copieDe(demande.getFiltre()));
		Object corps = reponse.getBody();
		// /global renvoie le rapport dans un ApiResponseDto (status true) ; /filtered le renvoie directement.
		if (corps instanceof ApiResponseDto dto && dto.isStatus() && dto.getContent() instanceof ReportGlobalResponse contenu) corps = contenu;
		if (!(corps instanceof ReportGlobalResponse)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(corps); // licence inactive ou invalide : même réponse que le rapport
		}
		Map<String, Object> donnees = objectMapper.convertValue(corps, Map.class);
		List<CarteAgence> agences = carteService.agences(filtreCarte);

		byte[] fichier = new RapportPowerBi(donnees, agences, demande).construire();
		String nom = "Rapport_GPR_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pbit";
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nom + "\"")
				.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(fichier);
	}
}
