package com.sicmagroup.gpr.api.config.setting;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data 
@AllArgsConstructor 
@NoArgsConstructor
@Builder
public class MailRequest {
    private String host;
    private String port;
    private String user;
    private String pwd;

    /**
     * Chiffrement de la connexion SMTP : "SSL" (SSL/TLS dès la connexion, port 465 en général), "STARTTLS"
     * (chiffrement négocié, obligatoire, port 587 en général) ou "NONE" (aucun, déconseillé). Absent dans les
     * configurations enregistrées avant l'ajout de ce choix : voir MailTransport.
     */
    private String security;

    /**
     * true : accepte le certificat du serveur même s'il n'est pas vérifiable (auto-signé, expiré...).
     * null (configurations anciennes) est traité comme true pour ne rien casser à la mise à jour.
     */
    private Boolean trustCertificate;

    /**
     * false : le serveur n'exige aucune authentification (relais interne) ; l'adresse "user" ne sert alors que
     * d'expéditeur et aucun mot de passe n'est envoyé. null (configurations anciennes) vaut true.
     */
    private Boolean authRequired;
}
