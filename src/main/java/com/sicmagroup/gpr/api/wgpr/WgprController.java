package com.sicmagroup.gpr.api.wgpr;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.wgpr.*;
import com.sicmagroup.gpr.domain.enumeration.SatisfactionStatus;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ClaimAudio;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ClaimAudioRepository;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.claim.ClaimService;
import com.sicmagroup.gpr.service.solution.SolutionServiceImpl;
import com.sicmagroup.gpr.service.wgpr.WgprComplaintService;
import com.sicmagroup.gpr.service.wgpr.WgprWhatsappBridgeService;
import com.sicmagroup.gpr.utils.Constante;
import com.sicmagroup.gpr.utils.crypto.FileEncryptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/whatgpr")
@RequiredArgsConstructor
public class WgprController {

    private final WgprWhatsappBridgeService  bridgeService;
    private final WgprComplaintService       complaintService;
    private final ClaimService               claimService;
    private final SolutionServiceImpl        solutionServiceImpl;
    private final AuthenticationServiceImpl  authService;
    private final ClaimAudioRepository       claimAudioRepository;

    @Value("${whatgpr.node.url:http://localhost:3001}")
    private String nodeBaseUrl;

    @Value("${whatgpr.internal.secret}")
    private String internalSecret;

    // ══════════════════════════════════════════════════════════════════════════════
    // Connexion WhatsApp  →  /api/whatgpr/whatsapp/*
    // ══════════════════════════════════════════════════════════════════════════════

    /** GET /api/whatgpr/whatsapp/qr → { qr: "base64_png_data" } */
    @GetMapping("/whatsapp/qr")
    public ResponseEntity<?> getQrCode() {
        try {
            String qr = bridgeService.getQrCode();
            if (qr == null) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "QR non disponible", "status", bridgeService.getStatus()));
            }
            // Le frontend attend { qr: "iVBORw0..." } sans le préfixe data:image
            if (qr.startsWith("data:image/png;base64,")) {
                qr = qr.substring("data:image/png;base64,".length());
            }
            return ResponseEntity.ok(Map.of("qr", qr));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(errorBody(e));
        }
    }

    /** GET /api/whatgpr/whatsapp/status → { status: "connected|disconnected|..." } */
    @GetMapping("/whatsapp/status")
    public ResponseEntity<?> getStatus() {
        try {
            return ResponseEntity.ok(Map.of("status", bridgeService.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("status", "disconnected"));
        }
    }

    /** POST /api/whatgpr/whatsapp/force-restart — déblocage après veille PC */
    @PostMapping("/whatsapp/force-restart")
    public ResponseEntity<ApiResponseDto> forceRestart() {
        try {
            bridgeService.forceRestart();
            return ok("Reconnexion forcée");
        } catch (Exception e) {
            return err(e.getMessage());
        }
    }

    /** POST /api/whatgpr/whatsapp/disconnect */
    @PostMapping("/whatsapp/disconnect")
    public ResponseEntity<ApiResponseDto> disconnect() {
        try {
            bridgeService.disconnect();
            return ok("Déconnecté avec succès");
        } catch (Exception e) {
            return err(e.getMessage());
        }
    }

    /** POST /api/whatgpr/send */
    @PostMapping("/send")
    public ResponseEntity<ApiResponseDto> sendMessage(@RequestBody SendMessageDto dto) {
        try {
            bridgeService.sendMessage(dto.getTo(), dto.getMessage());
            return ok("Message envoyé");
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /** POST /api/whatgpr/send-audio */
    @PostMapping("/send-audio")
    public ResponseEntity<ApiResponseDto> sendAudio(@RequestBody SendAudioDto dto) {
        try {
            bridgeService.sendAudio(dto.getTo(), dto.getAudioBase64(), dto.getMimeType());
            return ok("Audio envoyé");
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /** POST /api/whatgpr/poll */
    @PostMapping("/poll")
    public ResponseEntity<ApiResponseDto> sendPoll(@RequestBody SendPollDto dto) {
        try {
            String msgId = bridgeService.sendPoll(dto.getTo(), dto.getQuestion(), dto.getOptions());
            return ok(msgId);
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /** GET /api/whatgpr/events → SSE stream */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        return bridgeService.streamEvents();
    }

    /**
     * GET /api/whatgpr/claims/needing-pilot-comment
     * Proxy vers Node.js — retourne les claim_ids nécessitant un commentaire pilote.
     */
    @GetMapping("/claims/needing-pilot-comment")
    public ResponseEntity<?> getClaimsNeedingPilotComment() {
        try {
            org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
            ResponseEntity<java.util.List> resp = rt.getForEntity(
                nodeBaseUrl + "/internal/claims-needing-comment", java.util.List.class);
            return ResponseEntity.ok(resp.getBody() != null ? resp.getBody() : java.util.Collections.emptyList());
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Messages
    // ══════════════════════════════════════════════════════════════════════════════

    /** GET /api/whatgpr/messages[?filter=all] */
    @GetMapping("/messages")
    public ResponseEntity<ApiResponseDto> getMessages(
            @RequestParam(required = false, defaultValue = "all") String filter) {
        try {
            return ok(complaintService.getAllMessages());
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /** PATCH /api/whatgpr/messages/{id}/read */
    @RequestMapping(value = "/messages/{id}/read", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<ApiResponseDto> markRead(@PathVariable Long id) {
        try {
            complaintService.markMessageRead(id);
            return ok("Message marqué comme lu");
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /** PATCH /api/whatgpr/messages/read — marquage en lot ({ "ids": [...] }) */
    @RequestMapping(value = "/messages/read", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<ApiResponseDto> markReadBatch(@RequestBody Map<String, List<Long>> body) {
        try {
            complaintService.markMessagesRead(body.get("ids"));
            return ok("Messages marqués comme lus");
        } catch (Exception e) { return err(e.getMessage()); }
    }

    /**
     * POST /api/whatgpr/messages/mark-converted
     * Marque les messages comme convertis après création d'une plainte via les formulaires existants.
     * Même comportement que l'ancien système qui supprimait les messages après conversion.
     */
    @PostMapping("/messages/mark-converted")
    public ResponseEntity<ApiResponseDto> markConverted(@RequestBody Map<String, List<Long>> body) {
        try {
            complaintService.markMessagesAsConverted(body.get("ids"));
            return ok("Messages marqués comme convertis");
        } catch (Exception e) { return err(e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Proxy médias  →  /api/whatgpr/uploads/{filename}
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/whatgpr/uploads/{filename}
     * Proxy transparent vers http://localhost:3001/internal/uploads/{filename}
     */
    @GetMapping("/uploads/{filename}")
    public ResponseEntity<byte[]> serveMedia(@PathVariable String filename) {
        try {
            URL url = new URL(nodeBaseUrl + "/internal/uploads/" + filename);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            String contentType = conn.getContentType();
            try (InputStream is = conn.getInputStream()) {
                byte[] data = is.readAllBytes();
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(
                                contentType != null ? contentType : "application/octet-stream"))
                        .body(data);
            }
        } catch (Exception e) {
            log.warn("[WhatGPR] Média introuvable : {}", filename);
            return ResponseEntity.notFound().build();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Satisfaction client depuis WhatsApp
    // ══════════════════════════════════════════════════════════════════════════════

    /** POST /api/whatgpr/send-with-survey-audio — envoie une note vocale + poll au client WhatsApp */
    @PostMapping("/send-with-survey-audio")
    public ResponseEntity<ApiResponseDto> sendWithSurveyAudio(@RequestBody Map<String, Object> dto) {
        try {
            String to           = (String) dto.get("to");
            String audioBase64  = (String) dto.get("audioBase64");
            String mimeType     = (String) dto.getOrDefault("mimeType", "audio/ogg; codecs=opus");
            String audioBase642 = (String) dto.get("audioBase642"); // demande commentaire insatisfait
            String audioBase643 = (String) dto.get("audioBase643"); // remerciement satisfait
            String audioBase644 = (String) dto.get("audioBase644"); // réponse après commentaire
            Long   claimId      = Long.valueOf(dto.get("claimId").toString());
            Long   solutionId   = Long.valueOf(dto.get("solutionId").toString());
            Long   measurerId   = Long.valueOf(dto.get("measurerId").toString());
            String surveyAudioBase64 = (String) dto.get("surveyAudioBase64"); // note vocale facultative présentant le sondage

            bridgeService.sendSolutionAudioWithSurvey(to, audioBase64, mimeType,
                    audioBase642, audioBase643, audioBase644, claimId, solutionId, measurerId, surveyAudioBase64);
            return ok("Note vocale et enquête de satisfaction envoyées");
        } catch (Exception e) {
            return err(e.getMessage());
        }
    }

    /** POST /api/whatgpr/send-with-survey — envoie la solution + le poll au client WhatsApp */
    @PostMapping("/send-with-survey")
    public ResponseEntity<ApiResponseDto> sendWithSurvey(@RequestBody SendWithSurveyDto dto) {
        try {
            bridgeService.sendSolutionWithSurvey(dto.getTo(), dto.getMessage(),
                    dto.getClaimId(), dto.getSolutionId(), dto.getMeasurerId(),
                    dto.getSurveyAudioBase64(), dto.getSurveyAudioMimeType());
            return ok("Solution et enquête de satisfaction envoyées");
        } catch (Exception e) {
            return err(e.getMessage());
        }
    }

    /**
     * POST /api/whatgpr/internal/measure-with-audio — appelé par Node.js quand le client envoie un audio comme commentaire.
     * Sauvegarde l'audio comme ClaimAudio extra puis mesure la réclamation avec commentaire [WhatsApp-Audio].
     */
    @PostMapping("/internal/measure-with-audio")
    public ResponseEntity<?> internalMeasureWithAudio(
            @RequestBody WhatsappMeasureAudioDto dto,
            @RequestHeader(value = "X-WhatGPR-Secret", required = false) String secret) {

        if (secret == null || !secret.equals(internalSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Non autorisé"));
        }
        try {
            Claim    claim    = claimService.getById(dto.getClaimId());
            Solution solution = solutionServiceImpl.getById(dto.getSolutionId());
            User     measurer = authService.getById(dto.getMeasurerId());

            // Décoder et sauvegarder l'audio
            String base64Data = dto.getAudioBase64();
            if (base64Data != null && base64Data.contains(",")) base64Data = base64Data.split(",")[1];
            if (base64Data != null && !base64Data.isBlank()) {
                byte[] audioBytes = Base64.getDecoder().decode(base64Data);
                String audioDir   = Constante.DEVMODE ? Constante.TEST_PATH_AUDIO : Constante.PROD_PATH_AUDIO;
                Path   dir        = Paths.get(audioDir).toAbsolutePath().normalize();
                Files.createDirectories(dir);
                String claimRef  = claim.getCodeClient() != null ? claim.getCodeClient() : String.valueOf(dto.getClaimId());
                String fileName  = "[WhatsApp] Commentaire vocal - " + claimRef + ".ogg";
                Path   filePath  = dir.resolve(fileName);
                FileEncryptor.write(audioBytes, filePath);

                ClaimAudio audio = ClaimAudio.builder()
                        .claim(claim).name(fileName).size((long) audioBytes.length)
                        .path(filePath.toAbsolutePath().toString())
                        .is_extra(true).build();
                claimAudioRepository.save(audio);
            }

            SatisfactionStatus status = switch (dto.getSatisfactionStatus()) {
                case "PARTIAL"     -> SatisfactionStatus.PARTIAL;
                case "UNSATISFIED" -> SatisfactionStatus.UNSATISFIED;
                default            -> SatisfactionStatus.SATISFIED;
            };
            // [WhatsApp-Audio] = commentaire audio depuis WhatsApp, détecté par le frontend
            claimService.measureClaim(claim, solution, measurer, status, "[WhatsApp-Audio]");
            log.info("[WhatGPR] Réclamation {} mesurée avec audio WhatsApp : {}", dto.getClaimId(), status);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("[WhatGPR] Erreur measure-with-audio claimId={}", dto.getClaimId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(e));
        }
    }

    /**
     * POST /api/whatgpr/internal/measure — appelé par Node.js quand le client vote.
     * Protégé par X-WhatGPR-Secret (pas de JWT car appelé serveur→serveur).
     */
    @PostMapping("/internal/measure")
    public ResponseEntity<?> internalMeasure(
            @RequestBody WhatsappMeasureDto dto,
            @RequestHeader(value = "X-WhatGPR-Secret", required = false) String secret) {

        if (secret == null || !secret.equals(internalSecret)) {
            log.warn("[WhatGPR] Tentative d'accès interne non autorisée");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Non autorisé"));
        }
        try {
            Claim    claim    = claimService.getById(dto.getClaimId());
            Solution solution = solutionServiceImpl.getById(dto.getSolutionId());
            User     measurer = authService.getById(dto.getMeasurerId());

            SatisfactionStatus status = switch (dto.getSatisfactionStatus()) {
                case "PARTIAL"      -> SatisfactionStatus.PARTIAL;
                case "UNSATISFIED"  -> SatisfactionStatus.UNSATISFIED;
                default             -> SatisfactionStatus.SATISFIED;
            };

            // [WhatsApp] = marqueur détecté par le frontend pour afficher "depuis WhatsApp"
            // dans l'historique au lieu du nom de l'agent
            String commentaire = "[WhatsApp]" +
                    (dto.getCommentaire() != null && !dto.getCommentaire().isBlank()
                            ? " " + dto.getCommentaire() : "");

            claimService.measureClaim(claim, solution, measurer, status, commentaire);
            log.info("[WhatGPR] Réclamation {} mesurée depuis WhatsApp : {}", dto.getClaimId(), status);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("[WhatGPR] Erreur mesure WhatsApp claimId={}", dto.getClaimId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(e));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private ResponseEntity<ApiResponseDto> ok(Object content) {
        return ResponseEntity.ok(ApiResponseDto.builder().status(true).content(content).build());
    }

    private ResponseEntity<ApiResponseDto> err(String message) {
        return ResponseEntity.ok(ApiResponseDto.builder().status(false).content(message).build());
    }

    // Map.of() lève une NullPointerException si une valeur est null (ex: exceptions
    // sans message comme NotFoundException) — on garantit donc toujours une valeur non-null.
    private Map<String, String> errorBody(Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return Map.of("error", message);
    }
}
