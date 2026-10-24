package com.sicmagroup.gpr.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Mot de passe temporaire : longueur, composition, caractères ambigus exclus, valeurs différentes. */
class PasswordGeneratorTest {

    @Test
    void leMotDePasseRespecteLaRegleDeLApplication() {
        for (int i = 0; i < 500; i++) {
            String p = PasswordGenerator.generate();
            assertThat(p).hasSize(PasswordGenerator.DEFAULT_LENGTH);
            assertThat(p).matches(".*[a-z].*").matches(".*[A-Z].*").matches(".*[0-9].*").matches(".*[!@#$%*_-].*");
            assertThat(p).doesNotContain("O", "0", "l", "1", "I");
        }
    }

    @Test
    void lesMotsDePasseGeneresSontDifferents() {
        Set<String> vus = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            vus.add(PasswordGenerator.generate());
        }
        assertThat(vus).hasSize(200);
    }

    @Test
    void uneLongueurTropCourteEstRefusee() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> PasswordGenerator.generate(3));
    }
}
