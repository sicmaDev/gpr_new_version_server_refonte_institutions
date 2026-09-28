package com.sicmagroup.gpr.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import com.sicmagroup.gpr.service.jwt.JwtServiceImpl;

/**
 * Vérifie que l'API n'est plus accessible sans connexion (étape 1).
 * Les utilisateurs sont simulés (UserDetailsService mocké) : aucun compte n'est créé en base.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    private static final String ADMIN = "admin.test@gpr.local";
    private static final String AGENT = "agent.test@gpr.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtServiceImpl jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private final UserDetails admin = User.withUsername(ADMIN).password("x").authorities("H12", "DE").build();
    private final UserDetails agent = User.withUsername(AGENT).password("x").authorities("H1", "MOLDUE").build();

    @BeforeEach
    void setUp() {
        when(userDetailsService.loadUserByUsername(anyString())).thenAnswer(inv -> {
            String email = inv.getArgument(0);
            return ADMIN.equals(email) ? admin : agent;
        });
    }

    private String bearer(UserDetails user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    // --- Sans token : les routes protégées sont refusées ---

    @Test
    void routeProtegeeSansTokenEstRefusee() throws Exception {
        mockMvc.perform(get("/api/v1/demo"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    void configurationSansTokenEstRefusee() throws Exception {
        mockMvc.perform(get("/api/v1/config/objet/list/false"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    void synchroSansTokenEstRefusee() throws Exception {
        mockMvc.perform(post("/api/v1/sync/claim").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    // --- Avec un token valide : la route répond ---

    @Test
    void routeProtegeeAvecTokenRepond() throws Exception {
        mockMvc.perform(get("/api/v1/demo").header("Authorization", bearer(agent)))
                .andExpect(status().isOk());
    }

    // --- @RolesAllowed("H12") est appliqué, sans préfixe ROLE_ ---

    @Test
    void administrateurAccedeALaConfiguration() throws Exception {
        mockMvc.perform(get("/api/v1/config/objet/list/false").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void agentNAccedePasALaConfiguration() throws Exception {
        mockMvc.perform(get("/api/v1/config/objet/list/false").header("Authorization", bearer(agent)))
                .andExpect(status().isForbidden());
    }

    // --- Routes publiques : la sécurité laisse passer (le contrôleur répond lui-même) ---
    // Corps vide => 400 renvoyé par Spring MVC avant tout traitement : rien n'est créé ni envoyé.

    @Test
    void connexionEstPublique() throws Exception {
        mockMvc.perform(post("/api/v1/auth/authenticate").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void motDePasseOublieEstPublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forget/password").contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void inscriptionPubliqueResteOuverte() throws Exception {
        mockMvc.perform(post("/api/v1/config/user/publicRegister").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void routeBotSansCleEstGereeParLeControleur() throws Exception {
        // Pas de 401/403 de Spring Security : c'est le contrôleur qui vérifie la clé API (il renvoie 404)
        mockMvc.perform(get("/api/v1/apikey/setting"))
                .andExpect(status().isNotFound());
    }
}
