package com.sicmagroup.gpr.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sicmagroup.gpr.domain.model.Setting;

class SettingSecretsTest {

    private static final String MAIL = "{\"host\":\"smtp.example.com\",\"port\":\"587\",\"user\":\"gpr\",\"pwd\":\"motdepasse\"}";
    private static final String SMS = "{\"url\":\"https://sms.example.com\",\"libMdp\":\"password\",\"valMdp\":\"secret-sms\"}";

    @Test
    void lesMotsDePasseSontRetires() {
        assertThat(SettingSecrets.withoutSecrets(MAIL)).doesNotContain("motdepasse").contains("\"pwd\":null")
                .contains("smtp.example.com");
        assertThat(SettingSecrets.withoutSecrets(SMS)).doesNotContain("secret-sms").contains("\"valMdp\":null")
                .contains("\"libMdp\":\"password\"");
    }

    @Test
    void valeurNonJsonOuSansSecretInchangee() {
        assertThat(SettingSecrets.withoutSecrets("texte simple")).isEqualTo("texte simple");
        assertThat(SettingSecrets.withoutSecrets("{\"theme\":\"bleu\"}")).isEqualTo("{\"theme\":\"bleu\"}");
        assertThat(SettingSecrets.withoutSecrets((String) null)).isNull();
    }

    @Test
    void laCopieNeModifiePasLeParametreOriginal() {
        Setting original = Setting.builder().id(1L).libelle("app-mail").value(MAIL).build();
        List<Setting> copies = SettingSecrets.withoutSecrets(List.of(original));
        assertThat(copies.get(0).getValue()).doesNotContain("motdepasse");
        assertThat(copies.get(0).getLibelle()).isEqualTo("app-mail");
        assertThat(original.getValue()).isEqualTo(MAIL);
        assertThat(copies.get(0)).isNotSameAs(original);
    }

    @Test
    void motDePasseVideConserveLAncien() {
        String nouveau = "{\"host\":\"smtp2.example.com\",\"port\":\"465\",\"user\":\"gpr\",\"pwd\":\"\"}";
        String fusion = SettingSecrets.keepOldSecrets(nouveau, MAIL);
        assertThat(fusion).contains("smtp2.example.com").contains("\"pwd\":\"motdepasse\"");

        String sansChamp = "{\"url\":\"https://sms2.example.com\"}";
        assertThat(SettingSecrets.keepOldSecrets(sansChamp, SMS)).contains("sms2.example.com").contains("secret-sms");

        String nul = "{\"url\":\"https://sms.example.com\",\"valMdp\":null}";
        assertThat(SettingSecrets.keepOldSecrets(nul, SMS)).contains("secret-sms");
    }

    @Test
    void nouveauMotDePasseRemplaceLAncien() {
        String nouveau = "{\"host\":\"smtp.example.com\",\"pwd\":\"nouveau-mdp\"}";
        assertThat(SettingSecrets.keepOldSecrets(nouveau, MAIL)).isEqualTo(nouveau);
    }

    @Test
    void premierEnregistrementSansAncien() {
        assertThat(SettingSecrets.keepOldSecrets(MAIL, null)).isEqualTo(MAIL);
    }
}
