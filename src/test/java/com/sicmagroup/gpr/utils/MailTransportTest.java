package com.sicmagroup.gpr.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.sicmagroup.gpr.api.config.setting.MailRequest;

/** Réglages du serveur mail : chiffrement choisi, certificat, authentification, et comportement des anciennes configurations. */
class MailTransportTest {

    private MailRequest request(String port, String security, Boolean trust, Boolean auth) {
        MailRequest r = new MailRequest();
        r.setHost("smtp.exemple.test");
        r.setPort(port);
        r.setUser("expediteur@exemple.test");
        r.setPwd("secret");
        r.setSecurity(security);
        r.setTrustCertificate(trust);
        r.setAuthRequired(auth);
        return r;
    }

    private JavaMailSenderImpl apply(MailRequest r) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        MailTransport.apply(sender, r);
        return sender;
    }

    @Test
    void uneAncienneConfigurationDeduitLeChiffrementDuPort() {
        assertThat(MailTransport.resolveSecurity(request("465", null, null, null))).isEqualTo("SSL");
        assertThat(MailTransport.resolveSecurity(request("587", null, null, null))).isEqualTo("STARTTLS");
        assertThat(MailTransport.resolveSecurity(request("", null, null, null))).isEqualTo("STARTTLS");
        assertThat(MailTransport.port(request("", null, null, null))).isEqualTo(587);
    }

    @Test
    void leChoixEnregistreLEmporteSurLePort() {
        assertThat(MailTransport.resolveSecurity(request("587", "ssl", null, null))).isEqualTo("SSL");
        assertThat(MailTransport.resolveSecurity(request("465", "NONE", null, null))).isEqualTo("NONE");
    }

    @Test
    void sslStarttlsEtAucunChiffrementSontAppliquesALaConnexion() {
        var ssl = apply(request("465", "SSL", null, null)).getJavaMailProperties();
        assertThat(ssl.get("mail.smtp.ssl.enable")).isEqualTo("true");
        assertThat(ssl.get("mail.smtp.starttls.enable")).isEqualTo("false");

        var tls = apply(request("587", "STARTTLS", null, null)).getJavaMailProperties();
        assertThat(tls.get("mail.smtp.ssl.enable")).isEqualTo("false");
        assertThat(tls.get("mail.smtp.starttls.required")).isEqualTo("true");

        var none = apply(request("25", "NONE", null, null)).getJavaMailProperties();
        assertThat(none.get("mail.smtp.ssl.enable")).isEqualTo("false");
        assertThat(none.get("mail.smtp.starttls.enable")).isEqualTo("false");
    }

    @Test
    void sansAuthentificationAucunIdentifiantNEstTransmis() {
        JavaMailSenderImpl sender = apply(request("25", "NONE", null, false));

        assertThat(sender.getUsername()).isNull();
        assertThat(sender.getPassword()).isNull();
        assertThat(sender.getJavaMailProperties().get("mail.smtp.auth")).isEqualTo("false");
    }

    @Test
    void uneAncienneConfigurationGardeLAuthentificationEtAccepteLeCertificat() {
        JavaMailSenderImpl sender = apply(request("587", null, null, null));

        assertThat(sender.getUsername()).isEqualTo("expediteur@exemple.test");
        assertThat(sender.getPassword()).isEqualTo("secret");
        assertThat(sender.getJavaMailProperties().get("mail.smtp.ssl.trust")).isEqualTo("smtp.exemple.test");
    }

    @Test
    void refuserLesCertificatsNonVerifiablesVerifieLIdentiteDuServeur() {
        var props = apply(request("465", "SSL", false, true)).getJavaMailProperties();

        assertThat(props.get("mail.smtp.ssl.trust")).isNull();
        assertThat(props.get("mail.smtp.ssl.checkserveridentity")).isEqualTo("true");
    }
}
