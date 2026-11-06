package com.sicmagroup.gpr.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sicmagroup.gpr.utils.Constante;
import com.sicmagroup.gpr.utils.crypto.FieldEncryptor;

/** Seuls les réglages mail et SMS sont chiffrés en base ; les autres restent en clair. */
class SettingChiffrementTest {

    @BeforeAll
    static void installerCle() {
        byte[] cle = new byte[FieldEncryptor.KEY_LENGTH];
        new SecureRandom().nextBytes(cle);
        FieldEncryptor.install(new FieldEncryptor(cle));
    }

    private static String enBase(Setting setting) throws Exception {
        Field champ = Setting.class.getDeclaredField("value");
        champ.setAccessible(true);
        return (String) champ.get(setting);
    }

    private static Setting enregistre(String libelle, String valeur) {
        Setting setting = Setting.builder().libelle(libelle).value(valeur).build();
        setting.chiffrerSiMailOuSms();
        return setting;
    }

    @Test
    void reglageMailChiffreEnBaseEtLisibleParLApplication() throws Exception {
        Setting setting = enregistre(Constante.MAIL_SLUG, "{\"pwd\":\"secret-mail\"}");
        assertThat(enBase(setting)).startsWith(FieldEncryptor.PREFIX).doesNotContain("secret-mail");
        assertThat(setting.getValue()).contains("secret-mail");
    }

    @Test
    void reglageSmsChiffreEnBaseEtLisibleParLApplication() throws Exception {
        Setting setting = enregistre(Constante.SMS_SLUG, "{\"valMdp\":\"secret-sms\"}");
        assertThat(enBase(setting)).startsWith(FieldEncryptor.PREFIX).doesNotContain("secret-sms");
        assertThat(setting.getValue()).contains("secret-sms");
    }

    @Test
    void autreReglageResteEnClair() throws Exception {
        Setting setting = enregistre("app-institution", "{\"nom\":\"Institution\"}");
        assertThat(enBase(setting)).isEqualTo("{\"nom\":\"Institution\"}");
        assertThat(setting.getValue()).isEqualTo("{\"nom\":\"Institution\"}");
    }

    @Test
    void valeurDejaChiffreeNestPasRechiffree() throws Exception {
        Setting setting = enregistre(Constante.MAIL_SLUG, "{\"pwd\":\"secret-mail\"}");
        String premier = enBase(setting);
        setting.chiffrerSiMailOuSms();
        assertThat(enBase(setting)).isEqualTo(premier);
    }

    @Test
    void leTexteDuReglageNeContientPasLeMotDePasse() {
        Setting setting = enregistre(Constante.MAIL_SLUG, "{\"pwd\":\"secret-mail\"}");
        assertThat(setting.toString()).doesNotContain("secret-mail");
    }
}
