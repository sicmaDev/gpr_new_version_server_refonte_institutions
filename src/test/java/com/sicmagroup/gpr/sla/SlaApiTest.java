package com.sicmagroup.gpr.sla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.service.jwt.JwtServiceImpl;
import com.sicmagroup.gpr.sla.repository.ClaimSlaRepository;
import com.sicmagroup.gpr.sla.repository.SlaPolicyRepository;
import com.sicmagroup.gpr.sla.service.SlaConfig;

/**
 * API du SLA et requêtes réelles contre la base locale de test. Transaction annulée à la fin : rien n'est
 * modifié. Les utilisateurs sont simulés (aucun compte créé).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SlaApiTest {

    private static final String ADMIN = "admin.test@gpr.local";
    private static final String AGENT = "agent.test@gpr.local";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtServiceImpl jwtService;
    @Autowired private SlaConfig config;
    @Autowired private SlaPolicyRepository policyRepository;
    @Autowired private ClaimSlaRepository claimSlaRepository;
    @MockBean private UserDetailsService userDetailsService;

    private final UserDetails admin = User.withUsername(ADMIN).password("x").authorities("H12", "DE").build();
    private final UserDetails agent = User.withUsername(AGENT).password("x").authorities("H1", "MOLDUE").build();

    @BeforeEach
    void setUp() {
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenAnswer(inv -> ADMIN.equals(inv.getArgument(0)) ? admin : agent);
    }

    @AfterEach
    void forgetCache() {
        // le paramètre « sla.enabled » activé dans un test est annulé avec la transaction ; on oublie le cache
        config.invalidate();
    }

    private String bearer(UserDetails user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    // --- Accès et désactivation par défaut -------------------------------------------------------------------------

    @Test
    void sansConnexionLApiSlaEstRefusee() throws Exception {
        mockMvc.perform(get("/api/v1/sla/summary"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    void parDefautLeSlaEstDesactive() throws Exception {
        mockMvc.perform(get("/api/v1/sla/status").header("Authorization", bearer(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.enabled").value(false));
        mockMvc.perform(get("/api/v1/sla/count").header("Authorization", bearer(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.count").value(0))
                .andExpect(jsonPath("$.content.enabled").value(false));
        mockMvc.perform(get("/api/v1/sla/items").header("Authorization", bearer(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.enabled").value(false));
    }

    // --- Configuration : réservée à l'administrateur --------------------------------------------------------------

    @Test
    void lesPolitiquesParDefautSontCreeesEtLisiblesParLAdministrateur() throws Exception {
        mockMvc.perform(get("/api/v1/sla/policies").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(7)); // 2 types x 3 risques + suggestions
        assertThat(policyRepository.count()).isGreaterThanOrEqualTo(7);
    }

    @Test
    void unAgentNePeutPasLireLaConfigurationDuSla() throws Exception {
        mockMvc.perform(get("/api/v1/sla/policies").header("Authorization", bearer(agent))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/sla/calendar").header("Authorization", bearer(agent))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/sla/settings").header("Authorization", bearer(agent))).andExpect(status().isForbidden());
    }

    @Test
    void unAgentPeutLireLesMotifsPourJustifierUnRetard() throws Exception {
        mockMvc.perform(get("/api/v1/sla/breach-reasons?kind=RETARD").header("Authorization", bearer(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].kind").value("RETARD"));
    }

    @Test
    void unAgentNePeutPasAjouterUnMotif() throws Exception {
        mockMvc.perform(post("/api/v1/sla/breach-reasons").header("Authorization", bearer(agent))
                .contentType(MediaType.APPLICATION_JSON).content("{\"libelle\":\"x\",\"kind\":\"RETARD\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void lesParametresInvalidesSontRefuses() throws Exception {
        mockMvc.perform(put("/api/v1/sla/settings").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"sla.scan_minutes\":\"0\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/sla/settings").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"sla.inconnu\":\"1\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/sla/settings").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"sla.enabled\":\"peut-etre\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lesPolitiquesInvalidesSontRefusees() throws Exception {
        // les rappels doivent être entre 1 et 99 %, le premier avant le second
        String body = "[{\"claimType\":\"CLAIM\",\"riskLevel\":\"GRAVE\",\"takeoverMinutes\":60,\"resolutionMinutes\":600,"
                + "\"closureMinutes\":300,\"reopenMinutes\":300,\"reminder1Pct\":80,\"reminder2Pct\":50,"
                + "\"graceMinutes\":540,\"complianceTarget\":90,\"active\":true}]";
        mockMvc.perform(put("/api/v1/sla/policies").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // --- SLA activé : les requêtes réelles s'exécutent sur la base ------------------------------------------------------

    @Test
    void ActiveLeResumeLaListeEtLeCompteurRepondentSurLaBaseReelle() throws Exception {
        config.set(SlaConfig.ENABLED, "true");

        mockMvc.perform(get("/api/v1/sla/status").header("Authorization", bearer(agent)))
                .andExpect(jsonPath("$.content.enabled").value(true));
        // l'utilisateur simulé n'existe pas en base : son périmètre est vide, les requêtes s'exécutent quand même
        mockMvc.perform(get("/api/v1/sla/summary").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.DEPASSE").value(0))
                .andExpect(jsonPath("$.content.PROCHES_REGLEMENTAIRE").value(0))
                .andExpect(jsonPath("$.content.TRAITEES_NON_MESUREES").value(0));
        mockMvc.perform(get("/api/v1/sla/items?state=DEPASSE&type=CLAIM&q=rec&page=0&size=10")
                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.total").value(0));
        mockMvc.perform(get("/api/v1/sla/count?state=A_RISQUE").header("Authorization", bearer(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.count").value(0));
        mockMvc.perform(get("/api/v1/sla/claim/CLAIM/999999999").header("Authorization", bearer(agent)))
                .andExpect(status().isNotFound());
    }

    @Test
    void activeLesIndicateursEtLesRapportsRepondent() throws Exception {
        config.set(SlaConfig.ENABLED, "true");

        mockMvc.perform(get("/api/v1/sla/stats?from=2026-01-01&to=2026-12-31").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.global.recues").value(0))
                .andExpect(jsonPath("$.content.parRisque").isArray())
                .andExpect(jsonPath("$.content.cloture.traitees").value(0));
    }

    @Test
    void uneperiodeTropLongueEstRefuseePourLesIndicateurs() throws Exception {
        config.set(SlaConfig.ENABLED, "true");

        mockMvc.perform(get("/api/v1/sla/stats?from=2020-01-01&to=2026-12-31").header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/sla/stats?from=2026-12-31&to=2026-01-01").header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lesIndicateursSontReservesAuxRapportsAuPiloteEtAuDe() throws Exception {
        config.set(SlaConfig.ENABLED, "true");

        // un agent (H1) n'a ni H11, ni Pilote, ni DE
        mockMvc.perform(get("/api/v1/sla/stats").header("Authorization", bearer(agent))).andExpect(status().isForbidden());
    }

    @Test
    void lesRequetesDeRattrapageEtDeControleSExecutentSurLaBase() {
        // lecture seule : ces requêtes sont celles de la tâche planifiée
        assertThat(claimSlaRepository.findClaimIdsWithoutSla(List.of(ClaimType.CLAIM, ClaimType.DENUNCIACION),
                PageRequest.of(0, 5))).isNotNull();
        assertThat(claimSlaRepository.findSuggestionIdsWithoutSla(PageRequest.of(0, 5))).isNotNull();
        assertThat(claimSlaRepository.findDue(LocalDateTime.now(), PageRequest.of(0, 5))).isNotNull();
    }

    @Test
    void activerLeSlaViaLesParametresReinitialiseLaReprise() throws Exception {
        mockMvc.perform(put("/api/v1/sla/settings").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"sla.enabled\":\"true\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content['sla.enabled']").value("true"));

        // à l'activation, la reprise des plaintes ouvertes est à refaire (sans alerte rétroactive)
        assertThat(config.getBoolean("sla.backfill_done", true)).isFalse();
    }
}
