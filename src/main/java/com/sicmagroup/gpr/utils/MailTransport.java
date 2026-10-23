package com.sicmagroup.gpr.utils;

import java.util.Properties;

import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.sicmagroup.gpr.api.config.setting.MailRequest;

/**
 * Applique une configuration mail (Configurations > Email) à un JavaMailSenderImpl.
 * Point unique pour tous les envois et pour le test d'envoi : le chiffrement choisi par l'institution
 * est donc appliqué partout de la même façon.
 */
public final class MailTransport {

    public static final String SECURITY_SSL = "SSL";
    public static final String SECURITY_STARTTLS = "STARTTLS";
    public static final String SECURITY_NONE = "NONE";

    private static final int DEFAULT_PORT = 587;

    private MailTransport() {
    }

    /** Chiffrement à utiliser : le choix enregistré, sinon, pour une ancienne configuration, déduit du port. */
    public static String resolveSecurity(MailRequest request) {
        String chosen = request.getSecurity() == null ? "" : request.getSecurity().trim().toUpperCase();
        if (SECURITY_SSL.equals(chosen) || SECURITY_STARTTLS.equals(chosen) || SECURITY_NONE.equals(chosen)) {
            return chosen;
        }
        return port(request) == 465 ? SECURITY_SSL : SECURITY_STARTTLS;
    }

    public static int port(MailRequest request) {
        String port = request.getPort();
        return (port == null || port.isBlank()) ? DEFAULT_PORT : Integer.parseInt(port.trim());
    }

    public static void apply(JavaMailSenderImpl sender, MailRequest request) {
        // Configurations anciennes (champ absent) : le serveur exige une authentification, comme avant.
        boolean authRequired = request.getAuthRequired() == null || request.getAuthRequired();

        sender.setHost(request.getHost());
        sender.setPort(port(request));
        // Sans authentification, aucun identifiant n'est transmis : "user" ne sert que d'adresse d'expédition.
        sender.setUsername(authRequired ? request.getUser() : null);
        sender.setPassword(authRequired ? request.getPwd() : null);

        String security = resolveSecurity(request);
        boolean ssl = SECURITY_SSL.equals(security);
        boolean starttls = SECURITY_STARTTLS.equals(security);
        // Configurations anciennes (champ absent) : le certificat du serveur reste accepté comme avant.
        boolean trustCertificate = request.getTrustCertificate() == null || request.getTrustCertificate();

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(authRequired));
        props.put("mail.smtp.ssl.enable", String.valueOf(ssl));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        props.put("mail.smtp.starttls.required", String.valueOf(starttls));
        // L'instance JavaMailSender est partagée : on retire toujours les valeurs d'une configuration précédente.
        if (trustCertificate) {
            props.put("mail.smtp.ssl.trust", request.getHost());
            props.remove("mail.smtp.ssl.checkserveridentity");
        } else {
            props.remove("mail.smtp.ssl.trust");
            props.put("mail.smtp.ssl.checkserveridentity", "true");
        }
        // Timeouts pour éviter les connexions mortes après longue inactivité
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        // Désactiver le cache SSL pour éviter les sessions expirées
        props.put("mail.smtp.ssl.sessioncachetime", "0");
        props.put("mail.debug", "false");
        sender.setJavaMailProperties(props);
    }
}
