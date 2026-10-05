package com.sicmagroup.gpr.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

/** Vérifie que l'application refuse de démarrer sans clés valides. */
class EncryptionConfigTest {

    private static String randomKey(int bytes) {
        byte[] key = new byte[bytes];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    @Test
    void demarreAvecDeuxClesValides() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("gpr.crypto.key", randomKey(32))
                .withProperty("gpr.crypto.hmac-key", randomKey(32));
        assertThat(EncryptionConfig.create(env)).isNotNull();
    }

    @Test
    void refuseSiVariableDEnvironnementAbsente() {
        // Comme en production : la propriété pointe vers une variable d'environnement non définie
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "gpr.crypto.key", "${GPR_TEST_VARIABLE_INEXISTANTE_1}",
                "gpr.crypto.hmac-key", "${GPR_TEST_VARIABLE_INEXISTANTE_2}")));
        assertThatThrownBy(() -> EncryptionConfig.create(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Démarrage impossible")
                .hasMessageContaining("GPR_ENCRYPTION_KEY");
    }

    @Test
    void refuseSiCleTropCourte() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("gpr.crypto.key", randomKey(16))
                .withProperty("gpr.crypto.hmac-key", randomKey(32));
        assertThatThrownBy(() -> EncryptionConfig.create(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GPR_ENCRYPTION_KEY");
    }

    @Test
    void refuseSiCleHmacAbsente() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("gpr.crypto.key", randomKey(32));
        assertThatThrownBy(() -> EncryptionConfig.create(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GPR_HMAC_KEY");
    }

    @Test
    void refuseSiLesDeuxClesSontIdentiques() {
        String cle = randomKey(32);
        MockEnvironment env = new MockEnvironment()
                .withProperty("gpr.crypto.key", cle)
                .withProperty("gpr.crypto.hmac-key", cle);
        assertThatThrownBy(() -> EncryptionConfig.create(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("différentes");
    }

    @Test
    void leMessageNAfficheJamaisLaCle() {
        String cle = randomKey(16);
        MockEnvironment env = new MockEnvironment()
                .withProperty("gpr.crypto.key", cle)
                .withProperty("gpr.crypto.hmac-key", randomKey(32));
        assertThatThrownBy(() -> EncryptionConfig.create(env))
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(cle));
    }
}
