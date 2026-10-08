package com.sicmagroup.gpr.sla.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.sla.domain.ContactAttempt;
import com.sicmagroup.gpr.sla.dto.SlaInfo;
import com.sicmagroup.gpr.sla.service.SlaActionService;
import com.sicmagroup.gpr.sla.service.SlaActionService.SlaActionException;
import com.sicmagroup.gpr.sla.service.SlaEngine;
import com.sicmagroup.gpr.sla.service.SlaInfoService;
import com.sicmagroup.gpr.sla.service.SlaPerimeter;

import lombok.RequiredArgsConstructor;

/** Actions humaines du SLA sur une plainte : attente du client, reprise, justification, contact du client. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sla/claim")
public class SlaActionController {

    public record ReasonRequest(String reason) {
    }

    public record JustificationRequest(Long reasonId, String comment) {
    }

    public record ContactRequest(String channel, boolean reached, String comment) {
    }

    public record UnreachableRequest(Long reasonId) {
    }

    private final SlaActionService actions;
    private final SlaPerimeter perimeter;
    private final SlaInfoService infoService;
    private final SlaEngine engine;

    private interface Action {
        SlaInfo run(User user) throws SlaActionException;
    }

    private ResponseEntity<ApiResponseDto> run(Action action) {
        User user = perimeter.currentUser();
        if (user == null) {
            return SlaController.error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Utilisateur introuvable");
        }
        try {
            return SlaController.ok(action.run(user));
        } catch (SlaActionException | IllegalArgumentException e) {
            return SlaController.error(HttpStatus.BAD_REQUEST, "SLA", e.getMessage());
        }
    }

    private SlaInfo info(com.sicmagroup.gpr.sla.domain.ClaimSla s) {
        return infoService.toInfo(s, infoService.ownerNames(List.of(s)));
    }

    @PostMapping("/{type}/{id}/wait-customer")
    public ResponseEntity<ApiResponseDto> waitCustomer(@PathVariable String type, @PathVariable Long id,
            @RequestBody(required = false) ReasonRequest body) {
        return run(user -> info(actions.waitCustomer(user, ClaimType.valueOf(type.toUpperCase()), id,
                body == null ? null : body.reason())));
    }

    @PostMapping("/{type}/{id}/resume")
    public ResponseEntity<ApiResponseDto> resume(@PathVariable String type, @PathVariable Long id) {
        return run(user -> info(actions.resume(user, ClaimType.valueOf(type.toUpperCase()), id)));
    }

    @PostMapping("/{type}/{id}/justification")
    public ResponseEntity<ApiResponseDto> justify(@PathVariable String type, @PathVariable Long id,
            @RequestBody JustificationRequest body) {
        return run(user -> info(actions.justify(user, ClaimType.valueOf(type.toUpperCase()), id, body.reasonId(),
                body.comment())));
    }

    @PostMapping("/CLAIM/{id}/contact-attempts")
    public ResponseEntity<ApiResponseDto> addAttempt(@PathVariable Long id, @RequestBody ContactRequest body) {
        User user = perimeter.currentUser();
        if (user == null) {
            return SlaController.error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Utilisateur introuvable");
        }
        try {
            ContactAttempt a = actions.addContactAttempt(user, id, body.channel(), body.reached(), body.comment());
            return SlaController.ok(Map.of("id", a.getId(), "channel", a.getChannel(), "reached", a.isReached()));
        } catch (SlaActionException e) {
            return SlaController.error(HttpStatus.BAD_REQUEST, "SLA", e.getMessage());
        }
    }

    @GetMapping("/CLAIM/{id}/contact-attempts")
    public ResponseEntity<ApiResponseDto> attempts(@PathVariable Long id) {
        User user = perimeter.currentUser();
        if (user == null) {
            return SlaController.error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Utilisateur introuvable");
        }
        try {
            return SlaController.ok(actions.contactAttempts(user, id).stream()
                    .map(a -> Map.of("id", a.getId(), "channel", a.getChannel(), "reached", a.isReached(),
                            "createdAt", String.valueOf(a.getCreatedAt())))
                    .toList());
        } catch (SlaActionException e) {
            return SlaController.error(HttpStatus.NOT_FOUND, "NOT FOUND", e.getMessage());
        }
    }

    @PostMapping("/CLAIM/{id}/waiting-message")
    public ResponseEntity<ApiResponseDto> waitingMessage(@PathVariable Long id) {
        User user = perimeter.currentUser();
        if (user == null) {
            return SlaController.error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Utilisateur introuvable");
        }
        try {
            actions.sendWaitingMessage(user, id);
            return SlaController.ok(Map.of("sent", true));
        } catch (SlaActionException e) {
            return SlaController.error(HttpStatus.BAD_REQUEST, "SLA", e.getMessage());
        }
    }

    @PostMapping("/CLAIM/{id}/close-unreachable")
    public ResponseEntity<ApiResponseDto> closeUnreachable(@PathVariable Long id,
            @RequestBody(required = false) UnreachableRequest body) {
        return run(user -> info(actions.closeUnreachable(user, id, body == null ? null : body.reasonId())));
    }
}
