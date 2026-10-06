package com.sicmagroup.gpr.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.api.config.setting.AddSettingRequest;
import com.sicmagroup.gpr.api.config.setting.UpdateSettingRequest;
import com.sicmagroup.gpr.domain.enumeration.ConfigExportEnum;
import com.sicmagroup.gpr.repository.SettingRepository;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
import com.sicmagroup.gpr.utils.Constante;
import com.sicmagroup.gpr.utils.crypto.FieldEncryptor;

import jakarta.persistence.EntityManager;

/**
 * Étape 7 : les mots de passe SMTP et SMS ne sont jamais renvoyés au navigateur (ni au site web via
 * l'export de configuration), et un mot de passe vide à l'enregistrement conserve l'ancien.
 * Transaction annulée à la fin : les paramètres réels ne sont pas modifiés.
 */
@SpringBootTest
@Transactional
class SettingPasswordsTest {

    @Autowired private SettingServiceImpl settingService;
    @Autowired private SettingRepository settingRepository;
    @Autowired private AuthenticationServiceImpl authService;
    @Autowired private FieldEncryptor fieldEncryptor;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EntityManager em;

    private final String mdpMail = "mdp-smtp-" + UUID.randomUUID();
    private final String mdpSms = "mdp-sms-" + UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        FieldEncryptor.install(fieldEncryptor);
        // Les paramètres réels de la base locale peuvent être chiffrés avec la clé de développement,
        // illisible avec la clé de test : on les écarte, uniquement dans cette transaction (annulée à la fin).
        em.createNativeQuery("DELETE FROM gps_setting").executeUpdate();
        em.clear();
        enregistrer(Constante.MAIL_SLUG, "{\"host\":\"smtp.example.com\",\"port\":\"587\",\"user\":\"gpr\",\"pwd\":\"" + mdpMail + "\"}");
        enregistrer(Constante.SMS_SLUG, "{\"url\":\"https://sms.example.com\",\"libMdp\":\"password\",\"valMdp\":\"" + mdpSms + "\"}");
    }

    private void enregistrer(String slug, String json) throws Exception {
        if (settingRepository.findByLibelle(slug).isPresent()) {
            settingService.update(UpdateSettingRequest.builder().libelle(slug).value(json).build());
        } else {
            settingService.save(AddSettingRequest.builder().libelle(slug).value(json).build());
        }
        settingRepository.flush();
    }

    @Test
    void exportDeConfigurationSansMotsDePasse() throws Exception {
        // Même contenu que la réponse de /api/v1/apikey/setting (site web) et /api/v1/config/setting/export
        String json = objectMapper.writeValueAsString(authService.exportConfig(ConfigExportEnum.configs));
        assertThat(json).doesNotContain(mdpMail).doesNotContain(mdpSms);
        assertThat(json).contains("smtp.example.com");
    }

    @Test
    void motDePasseVideALEnregistrementConserveLAncien() throws Exception {
        settingService.update(UpdateSettingRequest.builder().libelle(Constante.MAIL_SLUG)
                .value("{\"host\":\"smtp2.example.com\",\"port\":\"465\",\"user\":\"gpr\",\"pwd\":\"\"}").build());
        settingRepository.flush();
        String stocke = settingService.getbySlug(Constante.MAIL_SLUG).getValue();
        assertThat(stocke).contains("smtp2.example.com").contains(mdpMail);
    }

    @Test
    void lesParametresEnBaseGardentLeMotDePasse() throws Exception {
        // L'envoi d'e-mails / SMS côté serveur a toujours besoin du vrai mot de passe
        authService.exportConfig(ConfigExportEnum.configs);
        assertThat(settingService.getbySlug(Constante.MAIL_SLUG).getValue()).contains(mdpMail);
        assertThat(settingService.getbySlug(Constante.SMS_SLUG).getValue()).contains(mdpSms);
    }
}
