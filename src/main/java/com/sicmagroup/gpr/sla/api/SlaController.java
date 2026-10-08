package com.sicmagroup.gpr.sla.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.sla.dto.SlaInfo;
import com.sicmagroup.gpr.sla.service.SlaConfig;
import com.sicmagroup.gpr.sla.service.SlaEngine;
import com.sicmagroup.gpr.sla.service.SlaPerimeter;
import com.sicmagroup.gpr.sla.service.SlaQueryService;
import com.sicmagroup.gpr.sla.service.SlaStatsService;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;

/**
 * API de lecture du SLA. Tout est limité au périmètre de l'utilisateur connecté (même règle que les listes
 * de plaintes). Quand le SLA est désactivé, les réponses sont vides et l'indiquent.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sla")
public class SlaController {

    private final SlaQueryService queryService;
    private final SlaPerimeter perimeter;
    private final SlaConfig config;
    private final SlaEngine engine;
    private final SlaStatsService statsService;

    static ResponseEntity<ApiResponseDto> ok(Object content) {
        return ResponseEntity.ok(ApiResponseDto.builder().status(true).content(content).build());
    }

    static ResponseEntity<ApiResponseDto> error(HttpStatus status, String title, String message) {
        return ResponseEntity.status(status).body(ApiResponseDto.builder().status(false)
                .content(ErrorResponse.builder().title(title).message(message).build()).build());
    }

    private static ClaimType parseType(String type) {
        if (type == null || type.isBlank() || "ALL".equalsIgnoreCase(type)) {
            return null;
        }
        return ClaimType.valueOf(type.trim().toUpperCase());
    }

    /** Le SLA est-il actif ? Sert au frontend pour afficher ou non les écrans SLA. */
    @GetMapping("/status")
    public ResponseEntity<ApiResponseDto> status() {
        return ok(Map.of("enabled", config.enabled()));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponseDto> summary(@RequestParam(required = false) String type) {
        if (!config.enabled()) {
            return ok(Map.of("enabled", false));
        }
        User user = perimeter.currentUser();
        return ok(queryService.summary(user, parseType(type)));
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponseDto> items(@RequestParam(required = false) String type,
            @RequestParam(required = false) String state, @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        if (!config.enabled()) {
            return ok(Map.of("enabled", false, "items", java.util.List.of(), "total", 0));
        }
        try {
            return ok(queryService.items(perimeter.currentUser(), state, parseType(type), q, page, size));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, "Paramètre invalide", e.getMessage());
        }
    }

    /** Nombre de plaintes dans un état (par défaut : en retard) : alimente le badge du menu. */
    @GetMapping("/count")
    public ResponseEntity<ApiResponseDto> count(@RequestParam(defaultValue = "DEPASSE") String state,
            @RequestParam(required = false) String type) {
        if (!config.enabled()) {
            return ok(Map.of("count", 0, "enabled", false));
        }
        long n = queryService.count(perimeter.currentUser(), state, parseType(type));
        return ok(Map.of("count", n, "enabled", true));
    }

    /** Indicateurs et rapports de respect des délais (périmètre de l'utilisateur) : rapports, Pilote, DE. */
    @GetMapping("/stats")
    @RolesAllowed({ "H11", "PILOTE", "DE" })
    public ResponseEntity<ApiResponseDto> stats(@RequestParam(required = false) String type,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        if (!config.enabled()) {
            return ok(Map.of("enabled", false));
        }
        try {
            return ok(statsService.stats(perimeter.currentUser(), parseType(type),
                    from == null || from.isBlank() ? null : java.time.LocalDate.parse(from),
                    to == null || to.isBlank() ? null : java.time.LocalDate.parse(to)));
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
            return error(HttpStatus.BAD_REQUEST, "Paramètre invalide", e.getMessage());
        }
    }

    /** Détail du SLA d'une plainte (404 si elle est hors du périmètre de l'utilisateur). */
    @GetMapping("/claim/{type}/{id}")
    public ResponseEntity<ApiResponseDto> forClaim(@PathVariable String type, @PathVariable Long id) {
        if (!config.enabled()) {
            return ok(Map.of("enabled", false));
        }
        SlaInfo info = queryService.forClaim(perimeter.currentUser(), parseType(type), id, engine);
        if (info == null) {
            return error(HttpStatus.NOT_FOUND, "NOT FOUND", "Aucun suivi SLA pour cette plainte");
        }
        return ok(info);
    }
}
