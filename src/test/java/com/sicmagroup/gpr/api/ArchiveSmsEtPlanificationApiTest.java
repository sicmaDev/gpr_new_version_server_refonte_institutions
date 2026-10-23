package com.sicmagroup.gpr.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.sicmagroup.gpr.domain.enumeration.SmsStatus;
import com.sicmagroup.gpr.domain.enumeration.SmsType;
import com.sicmagroup.gpr.domain.model.SmsLog;
import com.sicmagroup.gpr.repository.SmsLogRepository;
import com.sicmagroup.gpr.service.jwt.JwtServiceImpl;

/**
 * Archive des SMS et planification des relances, de bout en bout (sécurité, JSON, base réelle de test).
 * Transaction annulée à la fin : rien n'est conservé. Les utilisateurs sont simulés.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArchiveSmsEtPlanificationApiTest {

    private static final String ADMIN = "admin.test@gpr.local";
    private static final String AGENT = "agent.test@gpr.local";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtServiceImpl jwtService;
    @Autowired private SmsLogRepository smsLogRepository;
    @Autowired private JdbcTemplate jdbc;
    @MockBean private UserDetailsService userDetailsService;

    private final UserDetails admin = User.withUsername(ADMIN).password("x").authorities("H12").build();
    private final UserDetails agent = User.withUsername(AGENT).password("x").authorities("H1", "MOLDUE").build();

    @BeforeEach
    void setUp() {
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenAnswer(inv -> ADMIN.equals(inv.getArgument(0)) ? admin : agent);
    }

    private String bearer(UserDetails user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private SmsLog sms(SmsStatus status) {
        return smsLogRepository.saveAndFlush(SmsLog.builder().type(SmsType.EXTERNE).numeroDestinataire("002290152030745")
                .content("Votre réclamation a bien été enregistrée").status(status).build());
    }

    // --- Archive des SMS ---------------------------------------------------------------------------------------

    @Test
    void lArchiveRelitLeNumeroEtLeTexteEtLesGardeChiffresEnBase() throws Exception {
        SmsLog saved = sms(SmsStatus.ENVOYE);

        mockMvc.perform(get("/api/v1/sms-log").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.content[0].numeroDestinataire").value("002290152030745"))
                .andExpect(jsonPath("$.content[0].content").value("Votre réclamation a bien été enregistrée"))
                .andExpect(jsonPath("$.content[0].status").value("ENVOYE"));

        String enBase = jdbc.queryForObject("select numero_destinataire from gps_sms_log where id = ?", String.class, saved.getId());
        assertThat(enBase).startsWith("ENC:v1:").doesNotContain("0152030745");
        String texteEnBase = jdbc.queryForObject("select content from gps_sms_log where id = ?", String.class, saved.getId());
        assertThat(texteEnBase).startsWith("ENC:v1:").doesNotContain("réclamation");
    }

    @Test
    void lArchiveSeFiltreParStatut() throws Exception {
        sms(SmsStatus.ECHEC);

        mockMvc.perform(get("/api/v1/sms-log").param("status", "ECHEC").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ECHEC"));
        mockMvc.perform(get("/api/v1/sms-log").param("numeroDestinataire", "0152030745").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].numeroDestinataire").value("002290152030745"));
        mockMvc.perform(get("/api/v1/sms-log").param("numeroDestinataire", "99999999").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void lArchiveEstReserveeAuxAdministrateursEtEnLectureSeule() throws Exception {
        SmsLog saved = sms(SmsStatus.ENVOYE);

        mockMvc.perform(get("/api/v1/sms-log")).andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
        mockMvc.perform(get("/api/v1/sms-log").header("Authorization", bearer(agent))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/sms-log/" + saved.getId()).header("Authorization", bearer(admin))).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/sms-log/987654321").header("Authorization", bearer(admin))).andExpect(status().isNotFound());
        // aucun appel ne permet de créer, modifier ou supprimer une ligne de l'archive
        mockMvc.perform(post("/api/v1/sms-log").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));
    }

    // --- Planification des relances ----------------------------------------------------------------------------

    @Test
    void lesDeuxPlanificationsParDefautSontCreeesAvecLaProchaineRelance() throws Exception {
        mockMvc.perform(get("/api/v1/config/scheduled-reports").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].frequency").value("DAILY"))
                .andExpect(jsonPath("$[0].timezone").value("Africa/Porto-Novo"))
                .andExpect(jsonPath("$[0].nextExecutionAt").isNotEmpty());
    }

    @Test
    void laPlanificationEstReserveeAuxAdministrateurs() throws Exception {
        mockMvc.perform(get("/api/v1/config/scheduled-reports").header("Authorization", bearer(agent))).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/config/scheduled-reports/1").header("Authorization", bearer(agent))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/config/scheduled-reports/timezone").header("Authorization", bearer(agent))
                .contentType(MediaType.APPLICATION_JSON).content("{\"timezone\":\"UTC\"}")).andExpect(status().isForbidden());
    }

    @Test
    void onModifieLaFrequenceEtLeServeurRefuseLesReglagesIncomplets() throws Exception {
        String liste = mockMvc.perform(get("/api/v1/config/scheduled-reports").header("Authorization", bearer(admin)))
                .andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(liste.replaceAll(".*?\"id\":(\\d+).*", "$1"));

        mockMvc.perform(put("/api/v1/config/scheduled-reports/" + id).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"frequency\":\"WEEKLY\",\"dayOfWeek\":2,\"sendTime\":\"09:30\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frequency").value("WEEKLY"))
                .andExpect(jsonPath("$.dayOfWeek").value(2))
                .andExpect(jsonPath("$.nextExecutionAt").isNotEmpty());

        mockMvc.perform(put("/api/v1/config/scheduled-reports/" + id).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"frequency\":\"WEEKLY\",\"sendTime\":\"09:30\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void leFuseauHoraireEstValide() throws Exception {
        mockMvc.perform(put("/api/v1/config/scheduled-reports/timezone").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"timezone\":\"Europe/Paris\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].timezone").value("Europe/Paris"));
        mockMvc.perform(put("/api/v1/config/scheduled-reports/timezone").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"timezone\":\"Mars/Olympus\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void l_envoiDeTestSansAdresseEstRefuseSansRienEnvoyer() throws Exception {
        String liste = mockMvc.perform(get("/api/v1/config/scheduled-reports").header("Authorization", bearer(admin)))
                .andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(liste.replaceAll(".*?\"id\":(\\d+).*", "$1"));

        mockMvc.perform(post("/api/v1/config/scheduled-reports/" + id + "/test").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"to\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // --- Inscription publique : listes de postes et de points de service -----------------------------------------

    @Test
    void lesListesDInscriptionPubliqueSontAccessiblesSansConnexionEtSansHabilitations() throws Exception {
        String postes = mockMvc.perform(get("/api/v1/public/poste/list"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String points = mockMvc.perform(get("/api/v1/public/service_point/list"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // jamais d'habilitations dans une réponse publique
        assertThat(postes).doesNotContain("habilitations");
        assertThat(points).doesNotContain("habilitations");
    }

    @Test
    void lesListesDeConfigurationRestentReserveesAuxAdministrateurs() throws Exception {
        mockMvc.perform(get("/api/v1/config/poste/list"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }
}
