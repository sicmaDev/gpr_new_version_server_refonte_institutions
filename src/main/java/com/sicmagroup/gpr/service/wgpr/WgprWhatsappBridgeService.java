package com.sicmagroup.gpr.service.wgpr;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface WgprWhatsappBridgeService {

    /** Retourne le QR code PNG en base64 (null si non disponible). */
    String getQrCode();

    /** Retourne le statut : connected | disconnected | qr_pending | connecting. */
    String getStatus();

    /** Déconnecte la session WhatsApp. */
    void disconnect();

    /** Envoie un message texte vers le JID donné. */
    void sendMessage(String to, String message);

    /** Envoie une note vocale simple (pas de sondage) vers le JID donné. */
    void sendAudio(String to, String audioBase64, String mimeType);

    /**
     * Envoie le message de solution + le poll de satisfaction au client.
     * Stocke le mapping claimId/solutionId/measurerId dans Node.js pour traitement automatique du vote.
     */
    void sendSolutionWithSurvey(String to, String message, Long claimId, Long solutionId, Long measurerId,
                                 String surveyAudioBase64, String surveyAudioMimeType);

    /**
     * Envoie un poll natif vers le JID donné.
     * @return l'identifiant du message WhatsApp (pour relier les votes).
     */
    String sendPoll(String to, String question, List<String> options);

    /**
     * Envoie une note vocale (base64) de la solution + poll de satisfaction au client.
     * @param to           JID WhatsApp du client (ex: "22507XXXXXXX@c.us")
     * @param audioBase64  audio 1 : solution (obligatoire)
     * @param mimeType     type MIME de l'audio (ex: "audio/ogg; codecs=opus")
     * @param audioBase642 audio 2 : demande de commentaire si insatisfait/partiel (optionnel)
     * @param audioBase643 audio 3 : remerciement si satisfait (optionnel)
     * @param audioBase644 audio 4 : réponse après commentaire insatisfait (optionnel)
     */
    void sendSolutionAudioWithSurvey(String to, String audioBase64, String mimeType,
                                     String audioBase642, String audioBase643, String audioBase644,
                                     Long claimId, Long solutionId, Long measurerId,
                                     String surveyAudioBase64);

    /**
     * Envoie un accusé de réception WhatsApp au client après enregistrement.
     * @param phone      numéro de téléphone brut (ex: "22507XXXXXXX")
     * @param codeClient référence (ex: "REC-a1b2")
     * @param clientName prénom et nom du client
     * @param type       "reclamation" | "suggestion" — détermine le texte du message
     */
    void sendAcknowledgment(String phone, String codeClient, String clientName, String type);

    /** Retourne le numéro GPR WhatsApp actuellement connecté (ex: "22952030745@c.us"), null si déconnecté. */
    String getConnectedNumber();

    /** Force la réinitialisation de la connexion WhatsApp (après veille PC). */
    void forceRestart();

    /** Ouvre un flux SSE retransmettant les événements du microservice Node.js. */
    SseEmitter streamEvents();
}