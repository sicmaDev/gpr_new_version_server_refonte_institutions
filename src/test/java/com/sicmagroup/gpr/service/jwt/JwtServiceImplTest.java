package com.sicmagroup.gpr.service.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/** Étape 5 : la clé de signature des jetons vient de GPR_JWT_SECRET et n'est plus dans le code. */
class JwtServiceImplTest {

    private final UserDetails agent = User.withUsername("agent@gpr.local").password("x").authorities("H1").build();

    private static String randomKey(int bytes) {
        byte[] k = new byte[bytes];
        new SecureRandom().nextBytes(k);
        return Base64.getEncoder().encodeToString(k);
    }

    private static JwtServiceImpl service(String secret) {
        return new JwtServiceImpl(new MockEnvironment().withProperty("gpr.jwt.secret", secret));
    }

    @Test
    void jetonValideAvecLaBonneCle() {
        JwtServiceImpl jwt = service(randomKey(64));
        String token = jwt.generateToken(agent);
        assertThat(jwt.extracUserName(token)).isEqualTo("agent@gpr.local");
        assertThat(jwt.isTokenValid(token, agent)).isTrue();
    }

    @Test
    void jetonSigneAvecUneAutreCleEstRefuse() {
        String fauxJeton = service(randomKey(64)).generateToken(agent);
        JwtServiceImpl serveur = service(randomKey(64));
        assertThatThrownBy(() -> serveur.extracUserName(fauxJeton))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }

    @Test
    void refuseDeDemarrerSansCle() {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("test",
                Map.of("gpr.jwt.secret", "${GPR_TEST_JWT_INEXISTANTE}")));
        assertThatThrownBy(() -> new JwtServiceImpl(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GPR_JWT_SECRET est absente");
        assertThatThrownBy(() -> new JwtServiceImpl(new MockEnvironment()))
                .hasMessageContaining("GPR_JWT_SECRET est absente");
    }

    @Test
    void refuseUneCleTropCourteOuInvalide() {
        assertThatThrownBy(() -> service(randomKey(16)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("au moins 32 octets");
        assertThatThrownBy(() -> service("pas du base64 !!"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Base64");
    }

    @Test
    void leMessageNAfficheJamaisLaCle() {
        String cle = randomKey(16);
        assertThatThrownBy(() -> service(cle)).satisfies(e -> assertThat(e.getMessage()).doesNotContain(cle));
    }
}
