package com.sicmagroup.gpr.service;

import java.util.List;
import java.util.Objects;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.api.config.setting.MailRequest;
import com.sicmagroup.gpr.domain.enumeration.MessageChannel;
import com.sicmagroup.gpr.domain.enumeration.SmsType;
import com.sicmagroup.gpr.domain.model.Setting;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
import com.sicmagroup.gpr.service.smsLog.OutgoingMessageLogger;
import com.sicmagroup.gpr.utils.Constante;
import com.sicmagroup.gpr.utils.MailTransport;

import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final SettingServiceImpl settingServiceImpl;
    private final ObjectMapper objectMapper;

    // Mode test : liste d'adresses séparées par des virgules ; vide en production (aucun filtre).
    @org.springframework.beans.factory.annotation.Value("${gpr.mail.destinataires-test:}")
    private String destinatairesTest;

    public MailService(JavaMailSender mailSender, SettingServiceImpl settingServiceImpl) {
        this.mailSender = mailSender;
        this.settingServiceImpl = settingServiceImpl;
        this.objectMapper = new ObjectMapper();
    }

    @Async("taskExecutor") // Utilise le ThreadPool Spring
    public CompletableFuture<Void> sendMail(String to, String subject, String body, String cc) {
        deliver(to, subject, body, cc, SmsType.INTERNE, null, null);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Mail adressé à un client (solution d'une réclamation, accusé de réception...). Seul le message
     * est envoyé : le commentaire interne du traitant n'est jamais transmis au client. L'envoi est
     * tracé dans le journal des envois comme un message externe, rattaché à la réclamation.
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> sendClientMail(String to, String subject, String message, Long claimId,
            Long suggestId) {
        deliver(to, subject, toHtml(message), null, SmsType.EXTERNE, claimId, suggestId);
        return CompletableFuture.completedFuture(null);
    }

    private void deliver(String to, String subject, String body, String cc, SmsType type, Long claimId,
            Long suggestId) {
        try {
            Setting mailSetting = settingServiceImpl.getbySlug(Constante.MAIL_SLUG);
            if (mailSetting != null) {
                MailRequest mailRequest = objectMapper.readValue(mailSetting.getValue(), MailRequest.class);

                // Configure dynamiquement JavaMailSender si nécessaire
                if (mailSender instanceof JavaMailSenderImpl) {
                    MailTransport.apply((JavaMailSenderImpl) mailSender, mailRequest);
                }

                // Création du mail HTML
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(body, true);
                helper.setFrom(mailRequest.getUser());

                if (cc != null && !cc.isEmpty()) {
                    helper.setCc(cc.split(","));
                }

                verifierDestinataires(mimeMessage);
                mailSender.send(mimeMessage);
                System.out.println("✅ Mail HTML envoyé à " + to);
                logMail(to, subject, body, true, type, claimId, suggestId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Erreur d’envoi de mail à " + to + " : " + e.getMessage());
            logMail(to, subject, body, false, type, claimId, suggestId);
        }
    }

    /**
     * Envoi synchrone, sans passer par le pool async : utilisé pour le "tester l'envoi"
     * manuel des relances planifiées, où l'on veut un succès/échec immédiat et explicite
     * plutôt qu'un envoi silencieux en arrière-plan.
     */
    public void sendMailSync(String to, String subject, String body, String cc) throws Exception {
        try {
            sendMailSyncUnlogged(to, subject, body, cc);
            logMail(to, subject, body, true, SmsType.INTERNE, null, null);
        } catch (Exception e) {
            logMail(to, subject, body, false, SmsType.INTERNE, null, null);
            throw e;
        }
    }

    private void sendMailSyncUnlogged(String to, String subject, String body, String cc) throws Exception {
        // Ici getbySlug lève une exception quand le paramètre n'existe pas (au lieu de renvoyer null) :
        // on la convertit en message clair pour l'utilisateur.
        Setting mailSetting;
        try {
            mailSetting = settingServiceImpl.getbySlug(Constante.MAIL_SLUG);
        } catch (Exception e) {
            throw new IllegalStateException("Aucune configuration mail n'est enregistrée (Configurations > Email).", e);
        }
        if (mailSetting == null) {
            throw new IllegalStateException("Aucune configuration mail n'est enregistrée (Configurations > Email).");
        }

        MailRequest mailRequest = objectMapper.readValue(mailSetting.getValue(), MailRequest.class);

        if (mailSender instanceof JavaMailSenderImpl) {
            MailTransport.apply((JavaMailSenderImpl) mailSender, mailRequest);
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);
        helper.setFrom(mailRequest.getUser());

        if (cc != null && !cc.isEmpty()) {
            helper.setCc(cc.split(","));
        }

        verifierDestinataires(mimeMessage);
        mailSender.send(mimeMessage);
    }

     @Async("taskExecutor")
    public CompletableFuture<Void> sendMail(List<User> usersTo, String subject, String body, String cc) {
        List<String> recipients = usersTo == null ? List.of() : usersTo.stream().map(User::getEmail).filter(Objects::nonNull).collect(Collectors.toList());
        try {
            Setting mailSetting = settingServiceImpl.getbySlug(Constante.MAIL_SLUG);
            if (mailSetting != null) {
                MailRequest mailRequest = objectMapper.readValue(mailSetting.getValue(), MailRequest.class);

                // Configure dynamiquement JavaMailSender si nécessaire
                if (mailSender instanceof JavaMailSenderImpl) {
                    MailTransport.apply((JavaMailSenderImpl) mailSender, mailRequest);
                }

                // Récupérer les emails des utilisateurs
                List<String> emails = usersTo.stream()
                                             .map(User::getEmail)
                                             .collect(Collectors.toList());

                // Création du MimeMessage HTML
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(emails.toArray(new String[0]));
                helper.setSubject(subject);
                helper.setText(body, true); // HTML
                helper.setFrom(mailRequest.getUser());

                if (cc != null && !cc.isEmpty()) {
                    helper.setCc(cc.split(","));
                }

                verifierDestinataires(message);
                mailSender.send(message);
                System.out.println(" Mail HTML envoyé à " + emails);
                recipients.forEach(r -> logMail(r, subject, body, true, SmsType.INTERNE, null, null));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Erreur d’envoi de mail : " + e.getMessage());
            recipients.forEach(r -> logMail(r, subject, body, false, SmsType.INTERNE, null, null));
        }

        return CompletableFuture.completedFuture(null);
    }

    private void logMail(String to, String subject, String body, boolean success, SmsType type, Long claimId,
            Long suggestId) {
        String content = "Objet : " + (subject == null ? "" : subject.trim()) + "\n\n"
                + masquerMotDePasse(OutgoingMessageLogger.htmlToText(body));
        OutgoingMessageLogger.record(MessageChannel.MAIL, type, to, content, success, claimId, suggestId);
    }

    private void verifierDestinataires(MimeMessage message) throws jakarta.mail.MessagingException {
        if (destinatairesTest == null || destinatairesTest.isBlank()) {
            return;
        }
        List<String> autorises = List.of(destinatairesTest.toLowerCase().split(","));
        for (jakarta.mail.Address adresse : message.getAllRecipients()) {
            if (!autorises.contains(adresse.toString().toLowerCase())) {
                throw new jakarta.mail.MessagingException("Envoi bloqué par le mode test");
            }
        }
    }

    // L'archive des envois est lisible à l'écran et exportable : un mot de passe n'y est jamais conservé.
    private static final java.util.regex.Pattern MOT_DE_PASSE_AFFICHE = java.util.regex.Pattern
            .compile("(mot de passe[^:\\n]{0,30}:\\s*)\\S+", java.util.regex.Pattern.CASE_INSENSITIVE);

    static String masquerMotDePasse(String texte) {
        return texte == null ? null : MOT_DE_PASSE_AFFICHE.matcher(texte).replaceAll("$1********");
    }

    // Le mail part en HTML : un texte brut est échappé et garde ses retours à la ligne ; un message qui
    // contient déjà des balises HTML est conservé tel quel.
    private static String toHtml(String text) {
        if (text == null) {
            return "";
        }
        if (text.matches("(?s).*<[a-zA-Z/][^>]*>.*")) {
            return text;
        }
        return HtmlUtils.htmlEscape(text).replace("\r\n", "\n").replace("\n", "<br>");
    }
}
