package com.sicmagroup.gpr.service.smsLog;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.sicmagroup.gpr.domain.enumeration.LogTarget;
import com.sicmagroup.gpr.domain.enumeration.LogType;
import com.sicmagroup.gpr.domain.enumeration.MessageChannel;
import com.sicmagroup.gpr.domain.enumeration.SmsStatus;
import com.sicmagroup.gpr.domain.enumeration.SmsType;
import com.sicmagroup.gpr.domain.model.Log;
import com.sicmagroup.gpr.domain.model.SmsLog;
import com.sicmagroup.gpr.repository.LogRepository;
import com.sicmagroup.gpr.repository.SmsLogRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Point unique d'enregistrement de tout ce que l'application envoie (SMS, mail, WhatsApp) dans le
 * journal des envois (table sms_log, écran "Archive des envois").
 *
 * L'accès est statique pour pouvoir être appelé depuis des classes utilitaires (Utils.sendSms) et
 * depuis n'importe quel service sans modifier leurs constructeurs. L'enregistrement ne doit jamais
 * faire échouer un envoi : toute erreur de journalisation est seulement signalée dans les logs.
 */
@Slf4j
@Component
public class OutgoingMessageLogger {

    private static final int MAX_CONTENT_LENGTH = 8000;

    private static volatile OutgoingMessageLogger instance;

    private final SmsLogRepository repository;
    private final LogRepository logRepository;

    public OutgoingMessageLogger(SmsLogRepository repository, LogRepository logRepository) {
        this.repository = repository;
        this.logRepository = logRepository;
        instance = this;
    }

    public static void record(MessageChannel channel, SmsType type, String recipient, String content,
            boolean success, Long claimId, Long suggestId) {
        OutgoingMessageLogger logger = instance;
        if (logger == null) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            logger.repository.save(SmsLog.builder()
                    .channel(channel)
                    .type(type)
                    .claimId(claimId)
                    .suggestId(suggestId)
                    .numeroDestinataire(recipient == null || recipient.isBlank() ? "(inconnu)" : recipient.trim())
                    .content(truncate(content))
                    .status(success ? SmsStatus.ENVOYE : SmsStatus.ECHEC)
                    .createdAt(now)
                    .sendAt(success ? now : null)
                    .build());
            logger.recordSystemLog(channel, recipient, success, claimId, now);
        } catch (Exception e) {
            log.warn("Journalisation de l'envoi ({} vers {}) impossible : {}", channel, recipient, e.getMessage());
        }
    }

    /**
     * Trace dans "Log Système" : tous les échecs, et les succès SMS / WhatsApp (le succès d'un mail y est
     * déjà enregistré par les services appelants).
     */
    private void recordSystemLog(MessageChannel channel, String recipient, boolean success, Long claimId,
            LocalDateTime now) {
        if (success && channel == MessageChannel.MAIL) {
            return;
        }
        String label = channel == MessageChannel.MAIL ? "mail" : channel == MessageChannel.WHATSAPP ? "whatsapp" : "sms";
        logRepository.save(Log.builder()
                .libelle((success ? "Envoi " : "Echec ") + label + " notification")
                .content((success ? "Envoyé à " : "Non envoyé à ") + recipient
                        + (claimId != null ? " (réclamation " + claimId + ")" : "")
                        + (success ? "" : " - voir Archive des envois"))
                .type(success ? LogType.INFO : LogType.ERROR)
                .target(claimId != null ? LogTarget.CLAIM : LogTarget.APP)
                .createdAt(now)
                .build());
    }

    /** Texte lisible d'un mail HTML : balises retirées, retours à la ligne conservés. */
    public static String htmlToText(String html) {
        if (html == null) {
            return "";
        }
        String text = html
                .replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|div|h[1-6]|li|tr)>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        return text.replaceAll("[ \\t]+", " ").replaceAll("\\s*\\n\\s*", "\n").trim();
    }

    /** Numéro WhatsApp lisible : "22952030745@c.us" devient "22952030745". */
    public static String phoneFromJid(String jid) {
        if (jid == null) {
            return null;
        }
        int at = jid.indexOf('@');
        return at > 0 ? jid.substring(0, at) : jid;
    }

    private static String truncate(String content) {
        if (content == null || content.isBlank()) {
            return "(vide)";
        }
        return content.length() > MAX_CONTENT_LENGTH ? content.substring(0, MAX_CONTENT_LENGTH) + "…" : content;
    }
}
