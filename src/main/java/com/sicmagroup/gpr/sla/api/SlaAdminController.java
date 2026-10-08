package com.sicmagroup.gpr.sla.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.sla.domain.BusinessDay;
import com.sicmagroup.gpr.sla.domain.Holiday;
import com.sicmagroup.gpr.sla.domain.SlaBreachReason;
import com.sicmagroup.gpr.sla.domain.SlaPolicy;
import com.sicmagroup.gpr.sla.repository.BusinessDayRepository;
import com.sicmagroup.gpr.sla.repository.HolidayRepository;
import com.sicmagroup.gpr.sla.repository.SlaBreachReasonRepository;
import com.sicmagroup.gpr.sla.repository.SlaPolicyRepository;
import com.sicmagroup.gpr.sla.service.BusinessTime;
import com.sicmagroup.gpr.sla.service.SlaConfig;
import com.sicmagroup.gpr.sla.service.SlaPolicyService;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;

/**
 * Configuration du SLA : politiques de délais, calendrier ouvré, jours fériés, motifs, paramètres.
 * Écriture réservée à l'administrateur (H12). Seule la liste des motifs est lisible par tout agent connecté
 * (pour justifier un retard).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sla")
public class SlaAdminController {

    private final SlaPolicyRepository policyRepository;
    private final SlaPolicyService policyService;
    private final BusinessDayRepository dayRepository;
    private final HolidayRepository holidayRepository;
    private final SlaBreachReasonRepository reasonRepository;
    private final BusinessTime businessTime;
    private final SlaConfig config;

    private static ResponseEntity<ApiResponseDto> bad(String message) {
        return SlaController.error(HttpStatus.BAD_REQUEST, "Configuration SLA invalide", message);
    }

    // --- Politiques ---------------------------------------------------------------------------------

    @GetMapping("/policies")
    @RolesAllowed("H12")
    public ResponseEntity<ApiResponseDto> policies() {
        policyService.seedDefaults();
        return SlaController.ok(policyRepository.findAll());
    }

    @PutMapping("/policies")
    @RolesAllowed("H12")
    public ResponseEntity<ApiResponseDto> updatePolicies(@RequestBody List<SlaPolicy> policies) {
        for (SlaPolicy p : policies) {
            if (p.getClaimType() == null || p.getRiskLevel() == null) {
                return bad("Type de plainte et niveau de risque obligatoires");
            }
            if (p.getTakeoverMinutes() < 0 || p.getResolutionMinutes() <= 0 || p.getClosureMinutes() < 0
                    || p.getReopenMinutes() < 0 || p.getGraceMinutes() < 0) {
                return bad("Les durées doivent être positives (la résolution doit être d'au moins 1 minute)");
            }
            if (p.getReminder1Pct() < 1 || p.getReminder2Pct() > 99 || p.getReminder1Pct() >= p.getReminder2Pct()) {
                return bad("Les rappels doivent être entre 1 et 99 %, le premier avant le second");
            }
            if (p.getComplianceTarget() < 0 || p.getComplianceTarget() > 100) {
                return bad("L'objectif de conformité doit être entre 0 et 100 %");
            }
        }
        for (SlaPolicy p : policies) {
            SlaPolicy existing = policyRepository.findByClaimTypeAndRiskLevel(p.getClaimType(), p.getRiskLevel())
                    .orElse(null);
            if (existing != null) {
                p.setId(existing.getId());
            } else {
                p.setId(null);
            }
            policyRepository.save(p);
        }
        return SlaController.ok(policyRepository.findAll());
    }

    // --- Calendrier ouvré -----------------------------------------------------------------------------

    @GetMapping("/calendar")
    @RolesAllowed("H12")
    public ResponseEntity<ApiResponseDto> calendar() {
        policyService.seedDefaults();
        return SlaController.ok(dayRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(a.getDayOfWeek(), b.getDayOfWeek())).toList());
    }

    @PutMapping("/calendar")
    @RolesAllowed("H12")
    public ResponseEntity<ApiResponseDto> updateCalendar(@RequestBody List<BusinessDay> days) {
        for (BusinessDay d : days) {
            if (d.getDayOfWeek() < 1 || d.getDayOfWeek() > 7) {
                return bad("Jour de la semaine invalide (1 = lundi ... 7 = dimanche)");
            }
            if (d.isWorking() && (d.getStartTime() == null || d.getEndTime() == null
                    || !d.getEndTime().isAfter(d.getStartTime()))) {
                return bad("Un jour travaillé doit avoir une heure de fin après l'heure de début");
            }
        }
        for (BusinessDay d : days) {
            BusinessDay existing = dayRepository.findAll().stream()
                    .filter(x -> x.getDayOfWeek() == d.getDayOfWeek()).findFirst().orElse(null);
            d.setId(existing == null ? null : existing.getId());
            if (!d.isWorking()) {
                d.setStartTime(d.getStartTime() == null ? LocalTime.of(8, 0) : d.getStartTime());
                d.setEndTime(d.getEndTime() == null ? LocalTime.of(17, 0) : d.getEndTime());
            }
            dayRepository.save(d);
        }
        businessTime.invalidate();
        return calendar();
    }

    // --- Jours fériés ---------------------------------------------------------------------------------

    @GetMapping("/holidays")
    @RolesAllowed("H12")
    public ResponseEntity<ApiResponseDto> holidays() {
        return SlaController.ok(holidayRepository.findAll().stream()
                .sorted((a, b) -> a.getDate().compareTo(b.getDate())).toList());
    }

    public record HolidayRequest(LocalDate date, String label, boolean recurring) {
    }

    @PostMapping("/holidays")
    @RolesAllowed("H12")
    public ResponseEntity<ApiResponseDto> addHoliday(@RequestBody HolidayRequest body) {
        if (body.date() == null) {
            return bad("La date du jour férié est obligatoire");
        }
        Holiday h = holidayRepository.save(Holiday.builder().date(body.date()).label(body.label())
                .recurring(body.recurring()).build());
        businessTime.invalidate();
        return SlaController.ok(h);
    }

    @PutMapping("/holidays/{id}")
    @RolesAllowed("H12")
    public ResponseEntity<ApiResponseDto> updateHoliday(@PathVariable Long id, @RequestBody HolidayRequest body) {
        Holiday h = holidayRepository.findById(id).orElse(null);
        if (h == null) {
            return SlaController.error(HttpStatus.NOT_FOUND, "NOT FOUND", "Jour férié introuvable");
        }
        if (body.date() == null) {
            return bad("La date du jour férié est obligatoire");
        }
        h.setDate(body.date());
        h.setLabel(body.label());
        h.setRecurring(body.recurring());
        holidayRepository.save(h);
        businessTime.invalidate();
        return SlaController.ok(h);
    }

    @DeleteMapping("/holidays/{id}")
    @RolesAllowed("H12")
    public ResponseEntity<ApiResponseDto> deleteHoliday(@PathVariable Long id) {
        holidayRepository.deleteById(id);
        businessTime.invalidate();
        return SlaController.ok(Map.of("deleted", id));
    }

    // --- Motifs de retard et de clôture non mesurée ------------------------------------------------------------

    /** Lisible par tout agent connecté : sert à justifier un retard. */
    @GetMapping("/breach-reasons")
    public ResponseEntity<ApiResponseDto> reasons(@RequestParam(required = false) String kind) {
        policyService.seedDefaults();
        return SlaController.ok(reasonRepository.findByActiveTrue().stream()
                .filter(r -> kind == null || kind.equalsIgnoreCase(r.getKind())).toList());
    }

    @PostMapping("/breach-reasons")
    @RolesAllowed("H12")
    public ResponseEntity<ApiResponseDto> addReason(@RequestBody SlaBreachReason body) {
        if (body.getLibelle() == null || body.getLibelle().isBlank()
                || (!"RETARD".equals(body.getKind()) && !"NON_MESURE".equals(body.getKind()))) {
            return bad("Libellé obligatoire ; le type doit être RETARD ou NON_MESURE");
        }
        body.setId(null);
        body.setActive(true);
        try {
            return SlaController.ok(reasonRepository.save(body));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return bad("Un motif avec ce libellé existe déjà");
        }
    }

    /** Renomme un motif (le type ne change pas). */
    @PutMapping("/breach-reasons/{id}")
    @RolesAllowed("H12")
    public ResponseEntity<ApiResponseDto> updateReason(@PathVariable Long id, @RequestBody SlaBreachReason body) {
        SlaBreachReason r = reasonRepository.findById(id).orElse(null);
        if (r == null) {
            return SlaController.error(HttpStatus.NOT_FOUND, "NOT FOUND", "Motif introuvable");
        }
        if (body.getLibelle() == null || body.getLibelle().isBlank()) {
            return bad("Le libellé est obligatoire");
        }
        r.setLibelle(body.getLibelle().trim());
        try {
            return SlaController.ok(reasonRepository.save(r));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return bad("Un motif avec ce libellé existe déjà");
        }
    }

    /** Désactive le motif (il reste lié aux anciennes plaintes). */
    @DeleteMapping("/breach-reasons/{id}")
    @RolesAllowed("H12")
    public ResponseEntity<ApiResponseDto> deleteReason(@PathVariable Long id) {
        SlaBreachReason r = reasonRepository.findById(id).orElse(null);
        if (r == null) {
            return SlaController.error(HttpStatus.NOT_FOUND, "NOT FOUND", "Motif introuvable");
        }
        r.setActive(false);
        reasonRepository.save(r);
        return SlaController.ok(Map.of("deactivated", id));
    }

    // --- Paramètres généraux ----------------------------------------------------------------------------------

    @GetMapping("/settings")
    @RolesAllowed("H12")
    public ResponseEntity<ApiResponseDto> settings() {
        return SlaController.ok(config.all());
    }

    @PutMapping("/settings")
    @RolesAllowed("H12")
    public ResponseEntity<ApiResponseDto> updateSettings(@RequestBody Map<String, String> values) {
        Map<String, String> known = config.all();
        for (Map.Entry<String, String> e : values.entrySet()) {
            if (!known.containsKey(e.getKey())) {
                return bad("Paramètre inconnu : " + e.getKey());
            }
            String v = e.getValue() == null ? "" : e.getValue().trim();
            String error = validate(e.getKey(), v);
            if (error != null) {
                return bad(error);
            }
        }
        boolean wasEnabled = config.enabled();
        for (Map.Entry<String, String> e : values.entrySet()) {
            config.set(e.getKey(), e.getValue().trim());
        }
        // à la (ré)activation, une nouvelle reprise silencieuse des plaintes ouvertes est faite
        if (!wasEnabled && values.containsKey(SlaConfig.ENABLED) && "true".equalsIgnoreCase(values.get(SlaConfig.ENABLED).trim())) {
            config.set("sla.backfill_done", "false");
        }
        return SlaController.ok(config.all());
    }

    private static String validate(String key, String v) {
        switch (key) {
            case SlaConfig.ENABLED, SlaConfig.AUTO_ESCALATION, SlaConfig.DAILY_DIGEST:
                return "true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v) ? null : key + " doit valoir true ou false";
            case SlaConfig.REGULATORY_DAYS:
                return inRange(v, 1, 365) ? null : "Les jours réglementaires doivent être entre 1 et 365";
            case SlaConfig.REGULATORY_WARNING_DAYS:
                return inRange(v, 0, 60) ? null : "Le préavis réglementaire doit être entre 0 et 60 jours";
            case SlaConfig.SCAN_MINUTES:
                return inRange(v, 1, 1440) ? null : "L'intervalle de contrôle doit être entre 1 et 1440 minutes";
            case SlaConfig.UNREACHABLE_ATTEMPTS:
                return inRange(v, 1, 20) ? null : "Le nombre de tentatives doit être entre 1 et 20";
            case SlaConfig.UNREACHABLE_DAYS:
                return inRange(v, 1, 90) ? null : "La période doit être entre 1 et 90 jours";
            case SlaConfig.DIGEST_TIME:
                try {
                    LocalTime.parse(v);
                    return null;
                } catch (Exception e) {
                    return "L'heure du récapitulatif doit être au format HH:mm";
                }
            case SlaConfig.CUSTOMER_WAITING_MESSAGE:
                return List.of("NONE", "MANUAL", "AUTO").contains(v) ? null : "Valeur permise : NONE, MANUAL ou AUTO";
            case SlaConfig.TIMEZONE:
                try {
                    java.time.ZoneId.of(v);
                    return null;
                } catch (Exception e) {
                    return "Fuseau horaire inconnu";
                }
            default:
                return null;
        }
    }

    private static boolean inRange(String v, int min, int max) {
        try {
            int n = Integer.parseInt(v);
            return n >= min && n <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
