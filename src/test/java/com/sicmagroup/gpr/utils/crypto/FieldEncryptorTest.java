package com.sicmagroup.gpr.utils.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.sicmagroup.gpr.domain.converter.EncryptedStringConverter;

class FieldEncryptorTest {

    private static byte[] randomKey() {
        byte[] key = new byte[FieldEncryptor.KEY_LENGTH];
        new SecureRandom().nextBytes(key);
        return key;
    }

    private final FieldEncryptor encryptor = new FieldEncryptor(randomKey(), randomKey());

    @Test
    void allerRetourAvecAccentsEtEmoji() {
        String texte = "Koffi Adjovi — réclamation à Cotonou, très mécontent 😡🇧🇯";
        String chiffre = encryptor.encrypt(texte);
        assertThat(chiffre).startsWith(FieldEncryptor.PREFIX).doesNotContain("Koffi");
        assertThat(encryptor.decrypt(chiffre)).isEqualTo(texte);
    }

    @Test
    void chaineVideAllerRetour() {
        assertThat(encryptor.decrypt(encryptor.encrypt(""))).isEmpty();
    }

    @Test
    void deuxChiffrementsDuMemeTexteSontDifferents() {
        String a = encryptor.encrypt("97000000");
        String b = encryptor.encrypt("97000000");
        assertThat(a).isNotEqualTo(b);
        assertThat(encryptor.decrypt(a)).isEqualTo(encryptor.decrypt(b));
    }

    @Test
    void valeurEnClairRelueTelleQuelle() {
        assertThat(encryptor.decrypt("ancienne donnée en clair")).isEqualTo("ancienne donnée en clair");
    }

    @Test
    void nullResteNull() {
        assertThat(encryptor.encrypt(null)).isNull();
        assertThat(encryptor.decrypt(null)).isNull();
    }

    @Test
    void donneeModifieeEstDetectee() {
        String chiffre = encryptor.encrypt("contenu de la plainte");
        byte[] data = Base64.getDecoder().decode(chiffre.substring(FieldEncryptor.PREFIX.length()));
        data[data.length / 2] ^= 0x01;
        String altere = FieldEncryptor.PREFIX + Base64.getEncoder().encodeToString(data);
        assertThatThrownBy(() -> encryptor.decrypt(altere)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void donneeTronqueeOuBase64InvalideEstRefusee() {
        assertThatThrownBy(() -> encryptor.decrypt(FieldEncryptor.PREFIX + "AAAA"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> encryptor.decrypt(FieldEncryptor.PREFIX + "pas du base64 !"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mauvaiseCleProvoqueUneErreur() {
        String chiffre = encryptor.encrypt("secret");
        FieldEncryptor autre = new FieldEncryptor(randomKey(), randomKey());
        assertThatThrownBy(() -> autre.decrypt(chiffre)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cleAbsenteOuTropCourteEstRefusee() {
        String bonne = Base64.getEncoder().encodeToString(randomKey());
        String courte = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> FieldEncryptor.fromBase64(null, bonne))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("GPR_ENCRYPTION_KEY");
        assertThatThrownBy(() -> FieldEncryptor.fromBase64(bonne, ""))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("GPR_HMAC_KEY");
        assertThatThrownBy(() -> FieldEncryptor.fromBase64(courte, bonne))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("32");
        assertThatThrownBy(() -> FieldEncryptor.fromBase64("pas-du-base64!!", bonne))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Base64");
    }

    @Test
    void deuxClesIdentiquesSontRefusees() {
        byte[] cle = randomKey();
        assertThatThrownBy(() -> new FieldEncryptor(cle, cle.clone()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("différentes");
    }

    @Test
    void empreinteStableEtSansEspaces() {
        String h = encryptor.blindIndex("97000000");
        assertThat(h).hasSize(64).matches("[0-9a-f]+");
        assertThat(encryptor.blindIndex("97000000")).isEqualTo(h);
        assertThat(encryptor.blindIndex(" 97 00 00 00 ")).isEqualTo(h);
        assertThat(encryptor.blindIndex("97000001")).isNotEqualTo(h);
    }

    @Test
    void empreinteNullSiVide() {
        assertThat(encryptor.blindIndex(null)).isNull();
        assertThat(encryptor.blindIndex("")).isNull();
        assertThat(encryptor.blindIndex("   ")).isNull();
    }

    @Test
    void empreinteDependDeLaCleHmac() {
        FieldEncryptor autre = new FieldEncryptor(randomKey(), randomKey());
        assertThat(autre.blindIndex("97000000")).isNotEqualTo(encryptor.blindIndex("97000000"));
    }

    @Test
    void convertisseurJpaChiffreEtDechiffre() {
        FieldEncryptor.install(encryptor);
        EncryptedStringConverter converter = new EncryptedStringConverter();
        String enBase = converter.convertToDatabaseColumn("Adresse : Akpakpa");
        assertThat(enBase).startsWith(FieldEncryptor.PREFIX);
        assertThat(converter.convertToEntityAttribute(enBase)).isEqualTo("Adresse : Akpakpa");
        assertThat(converter.convertToEntityAttribute("ancienne valeur")).isEqualTo("ancienne valeur");
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }
}
