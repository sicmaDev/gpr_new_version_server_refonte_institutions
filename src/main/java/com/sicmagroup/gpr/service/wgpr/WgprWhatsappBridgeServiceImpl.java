package com.sicmagroup.gpr.service.wgpr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class WgprWhatsappBridgeServiceImpl implements WgprWhatsappBridgeService {

    // RestTemplate dédié (pas le bean partagé de l'appli) : timeouts courts pour
    // ne jamais laisser un Node.js lent (démarrage Chrome) bloquer des threads
    // Spring pendant le polling fréquent du frontend.
    private final RestTemplate restTemplate = buildRestTemplate();

    @Value("${whatgpr.node.url:http://localhost:3001}")
    private String nodeBaseUrl;

    // Pool borné : un flux SSE par onglet/utilisateur connecté à la page WhatsApp,
    // chacun bloqué en lecture pour la durée de la connexion. Un newCachedThreadPool()
    // pourrait croître sans limite si de nombreux onglets/reconnexions s'accumulent.
    private final ExecutorService sseExecutor = new ThreadPoolExecutor(
            4, 50, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(200),
            new ThreadPoolExecutor.CallerRunsPolicy());

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        return new RestTemplate(factory);
    }

    // ── QR ──────────────────────────────────────────────────────────────────────
    @Override
    public String getQrCode() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    nodeBaseUrl + "/internal/qr", Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("qr");
            }
        } catch (Exception e) {
            log.error("[WhatGPR] Erreur récupération QR code", e);
        }
        return null;
    }

    // ── Statut ───────────────────────────────────────────────────────────────────
    @Override
    public String getStatus() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    nodeBaseUrl + "/internal/status", Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("status");
            }
        } catch (Exception e) {
            log.warn("[WhatGPR] Microservice Node.js inaccessible: {}", e.getMessage());
        }
        return "disconnected";
    }

    // ── Déconnexion ──────────────────────────────────────────────────────────────
    @Override
    public void disconnect() {
        try {
            restTemplate.postForEntity(nodeBaseUrl + "/internal/disconnect", null, Map.class);
        } catch (Exception e) {
            log.error("[WhatGPR] Erreur déconnexion WhatsApp", e);
        }
    }

    // ── Envoi message ────────────────────────────────────────────────────────────
    @Override
    public void sendMessage(String to, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = Map.of("to", to, "message", message);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForEntity(nodeBaseUrl + "/internal/send", entity, Map.class);
        } catch (Exception e) {
            log.error("[WhatGPR] Erreur envoi message vers {}", to, e);
            throw new RuntimeException("Impossible d'envoyer le message WhatsApp : " + e.getMessage());
        }
    }

    // ── Envoi note vocale simple (pas de sondage) ────────────────────────────────
    @Override
    public void sendAudio(String to, String audioBase64, String mimeType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = Map.of("to", to, "audioBase64", audioBase64,
                "mimeType", mimeType != null ? mimeType : "audio/ogg; codecs=opus");
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForEntity(nodeBaseUrl + "/internal/send-audio", entity, Map.class);
        } catch (Exception e) {
            log.error("[WhatGPR] Erreur envoi audio vers {}", to, e);
            throw new RuntimeException("Impossible d'envoyer l'audio WhatsApp : " + e.getMessage());
        }
    }

    // ── Envoi solution + poll de satisfaction ────────────────────────────────────
    @Override
    public void sendSolutionWithSurvey(String to, String message, Long claimId, Long solutionId, Long measurerId,
                                        String surveyAudioBase64, String surveyAudioMimeType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("to", to);
        body.put("message", message);
        body.put("claimId", claimId);
        body.put("solutionId", solutionId);
        body.put("measurerId", measurerId);
        if (surveyAudioBase64 != null) body.put("surveyAudioBase64", surveyAudioBase64);
        if (surveyAudioMimeType != null) body.put("surveyAudioMimeType", surveyAudioMimeType);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForEntity(nodeBaseUrl + "/internal/send-with-survey", entity, Map.class);
        } catch (Exception e) {
            log.error("[WhatGPR] Erreur envoi solution+survey vers {}", to, e);
            throw new RuntimeException("Impossible d'envoyer la solution WhatsApp : " + e.getMessage());
        }
    }

    // ── Envoi poll ───────────────────────────────────────────────────────────────
    @Override
    public String sendPoll(String to, String question, List<String> options) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("to", to, "question", question, "options", options);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    nodeBaseUrl + "/internal/sendPoll", entity, Map.class);
            if (response.getBody() != null) {
                return (String) response.getBody().get("messageId");
            }
        } catch (Exception e) {
            log.error("[WhatGPR] Erreur envoi poll vers {}", to, e);
            throw new RuntimeException("Impossible d'envoyer le sondage WhatsApp : " + e.getMessage());
        }
        return null;
    }

    // ── Envoi note vocale + poll ─────────────────────────────────────────────────
    @Override
    public void sendSolutionAudioWithSurvey(String to, String audioBase64, String mimeType,
                                            String audioBase642, String audioBase643, String audioBase644,
                                            Long claimId, Long solutionId, Long measurerId,
                                            String surveyAudioBase64) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("to", to);
        body.put("audioBase64", audioBase64);
        body.put("mimeType", mimeType != null ? mimeType : "audio/ogg; codecs=opus");
        body.put("claimId", claimId);
        body.put("solutionId", solutionId);
        body.put("measurerId", measurerId);
        if (audioBase642 != null) body.put("audioBase642", audioBase642);
        if (audioBase643 != null) body.put("audioBase643", audioBase643);
        if (audioBase644 != null) body.put("audioBase644", audioBase644);
        if (surveyAudioBase64 != null) body.put("surveyAudioBase64", surveyAudioBase64);
        HttpEntity<java.util.Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForEntity(nodeBaseUrl + "/internal/send-with-survey-audio", entity, Map.class);
            log.info("[WhatGPR] Note vocale+survey envoyés → claimId={}", claimId);
        } catch (Exception e) {
            log.error("[WhatGPR] Erreur envoi note vocale vers {}", to, e);
            throw new RuntimeException("Impossible d'envoyer la note vocale WhatsApp : " + e.getMessage());
        }
    }

    // ── Accusé de réception ──────────────────────────────────────────────────────
    @Override
    public void sendAcknowledgment(String phone, String codeClient, String clientName, String type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("phone",      phone);
        body.put("codeClient", codeClient != null ? codeClient : "");
        body.put("clientName", clientName != null ? clientName : "");
        body.put("type",       type       != null ? type       : "reclamation");
        HttpEntity<java.util.Map<String, String>> entity = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForEntity(nodeBaseUrl + "/internal/send-acknowledgment", entity, Map.class);
            log.info("[WhatGPR] Accusé de réception ({}) envoyé → {}", type, phone);
        } catch (Exception e) {
            log.warn("[WhatGPR] Accusé de réception non envoyé → {} : {}", phone, e.getMessage());
        }
    }

    // ── Numéro connecté ──────────────────────────────────────────────────────────
    @Override
    public String getConnectedNumber() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    nodeBaseUrl + "/internal/connected-number", Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("connectedNumber");
            }
        } catch (Exception e) {
            log.warn("[WhatGPR] Impossible de récupérer le numéro connecté : {}", e.getMessage());
        }
        return null;
    }

    // ── Force restart ────────────────────────────────────────────────────────────
    @Override
    public void forceRestart() {
        try {
            restTemplate.postForEntity(nodeBaseUrl + "/internal/force-restart", null, Map.class);
        } catch (Exception e) {
            log.warn("[WhatGPR] Force restart : {}", e.getMessage());
        }
    }

    // ── SSE retransmission ───────────────────────────────────────────────────────
    @Override
    public SseEmitter streamEvents() {
        SseEmitter emitter = new SseEmitter(0L); // pas de timeout

        sseExecutor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(nodeBaseUrl + "/internal/events");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Accept",        "text/event-stream");
                conn.setRequestProperty("Cache-Control", "no-cache");
                conn.connect();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    String currentEvent = null;
                    StringBuilder currentData = new StringBuilder();

                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("event: ")) {
                            currentEvent = line.substring(7).trim();
                        } else if (line.startsWith("data: ")) {
                            currentData.append(line.substring(6).trim());
                        } else if (line.isEmpty() && currentData.length() > 0) {
                            // Fin d'un événement SSE → retransmettre avec le bon nom
                            SseEmitter.SseEventBuilder evt = SseEmitter.event()
                                    .data(currentData.toString());
                            if (currentEvent != null) evt.name(currentEvent);
                            emitter.send(evt);
                            currentEvent = null;
                            currentData.setLength(0);
                        } else if (line.startsWith(":")) {
                            // Ping de maintien de connexion envoyé par Node (toutes les 25s) —
                            // retransmis tel quel : c'est le seul signal qui permet au frontend
                            // de détecter une connexion silencieusement morte (pas de FIN/RST).
                            emitter.send(SseEmitter.event().comment(line.substring(1).trim()));
                        }
                    }
                }
                emitter.complete();
            } catch (Exception e) {
                log.warn("[WhatGPR] Flux SSE Node.js terminé : {}", e.getMessage());
                emitter.completeWithError(e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        });

        return emitter;
    }
}