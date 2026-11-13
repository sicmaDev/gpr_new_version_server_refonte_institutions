package com.sicmagroup.gpr.api.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterAll;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.sicmagroup.gpr.service.jwt.JwtServiceImpl;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Reporting IA : droits (comme le menu Rapports), règles personnelles ajoutées à la question,
 * règles de toute l'institution réservées à H12, graphiques épinglés propres à chacun,
 * et adresses d'export pour l'IA ouvertes (décision : comme l'ancienne version).
 * Un faux Wren tourne sur le PC : aucun appel au vrai serveur.
 */
@SpringBootTest
@AutoConfigureMockMvc
class IaRapportsApiTest {

    private static final Map<String, String> RECU = new ConcurrentHashMap<>();

    private static final HttpServer FAUX_WREN = demarrer();

    private static HttpServer demarrer() {
        try {
            HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            s.createContext("/api/v1/reporting/stream/ask", ex -> repondre(ex, "text/event-stream",
                    "data: {\"type\":\"content_block_delta\",\"delta\":{\"text\":\"| Agence | Dossiers |\\n|---|---|\\n| A | 3 |\"}}\n\n"
                            + "data: {\"type\":\"message_stop\"}\n\n"));
            s.createContext("/api/graphql", ex -> repondre(ex, "application/json", "{\"data\":{\"instructions\":[]}}"));
            s.createContext("/api/v1/run_sql", ex -> repondre(ex, "application/json", "{\"records\":[]}"));
            s.start();
            return s;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void repondre(HttpExchange ex, String type, String corps) throws IOException {
        RECU.put(ex.getRequestURI().getPath(), new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] b = corps.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", type);
        ex.sendResponseHeaders(200, b.length);
        try (OutputStream o = ex.getResponseBody()) {
            o.write(b);
        }
    }

    @DynamicPropertySource
    static void adresses(DynamicPropertyRegistry r) {
        r.add("gpr.ia.url", () -> "http://127.0.0.1:" + FAUX_WREN.getAddress().getPort());
        r.add("gpr.ia.cle", () -> "cle-factice-pour-les-tests");
    }

    @AfterAll
    static void arreter() {
        FAUX_WREN.stop(0);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtServiceImpl jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private final UserDetails agentRapports = User.withUsername("rapports.ia@gpr.local").password("x").authorities("H11").build();
    private final UserDetails autreAgent = User.withUsername("autre.ia@gpr.local").password("x").authorities("H11").build();
    private final UserDetails guichet = User.withUsername("guichet.ia@gpr.local").password("x").authorities("H1").build();

    @BeforeEach
    void setUp() {
        when(userDetailsService.loadUserByUsername(anyString())).thenAnswer(i -> {
            String n = i.getArgument(0);
            return n.equals(autreAgent.getUsername()) ? autreAgent : n.equals(guichet.getUsername()) ? guichet : agentRapports;
        });
        RECU.clear();
    }

    private String bearer(UserDetails u) {
        return "Bearer " + jwtService.generateToken(u);
    }

    @Test
    void sansLeDroitAuxRapportsLaPageIaEstRefusee() throws Exception {
        mockMvc.perform(post("/api/v1/ia/rapports/question").header("Authorization", bearer(guichet))
                .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"Combien de plaintes ?\"}"))
                .andExpect(status().isForbidden());
        assertThat(RECU).isEmpty();
    }

    @Test
    void laQuestionPartAvecLesReglesPersonnellesDeLAgent() throws Exception {
        mockMvc.perform(post("/api/v1/ia/rapports/regles").header("Authorization", bearer(agentRapports))
                .contentType(MediaType.APPLICATION_JSON).content("{\"instruction\":\"Ne pas compter les plaintes annulées\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/ia/rapports/question").header("Authorization", bearer(agentRapports))
                .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"Combien de plaintes par agence ?\",\"threadId\":\"pas-un-uuid\"}"))
                .andExpect(status().isOk());
        String recu = RECU.get("/api/v1/reporting/stream/ask");
        assertThat(recu).contains("Combien de plaintes par agence ?").contains("Ne pas compter les plaintes annul")
                .doesNotContain("pas-un-uuid");
    }

    @Test
    void lesReglesDeToutLInstitutionNeSeModifientQuAvecH12() throws Exception {
        mockMvc.perform(post("/api/v1/ia/rapports/regles-globales").header("Authorization", bearer(agentRapports))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"mutation CreateInstruction($data: CreateInstructionInput!) { createInstruction(data: $data) { id } }\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/ia/rapports/regles-globales").header("Authorization", bearer(agentRapports))
                .contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"query { instructions { id instruction } }\"}"))
                .andExpect(status().isOk());
        // Autre chose que les règles : refusé (pas d'accès libre à Wren).
        mockMvc.perform(post("/api/v1/ia/rapports/regles-globales").header("Authorization", bearer(agentRapports))
                .contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"query { threads { id } }\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unGraphiqueEpingleNEstVuEtActualiseQueParSonAuteur() throws Exception {
        String reponse = mockMvc.perform(post("/api/v1/ia/rapports/epingles").header("Authorization", bearer(agentRapports))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Plaintes par agence\",\"chartType\":\"bar\",\"sqlQuery\":\"SELECT 1\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id = reponse.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/v1/ia/rapports/epingles/" + id + "/actualiser").header("Authorization", bearer(autreAgent)))
                .andExpect(status().isNotFound());
        assertThat(RECU).doesNotContainKey("/api/v1/run_sql");

        // Réutiliser l'identifiant d'un autre ne remplace pas son graphique.
        mockMvc.perform(post("/api/v1/ia/rapports/epingles").header("Authorization", bearer(autreAgent))
                .contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"" + id + "\",\"title\":\"Pirate\",\"sqlQuery\":\"SELECT 2\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/ia/rapports/epingles").header("Authorization", bearer(agentRapports)))
                .andExpect(jsonPath("$.content[?(@.id=='" + id + "')].title").value("Plaintes par agence"));

        mockMvc.perform(post("/api/v1/ia/rapports/epingles/" + id + "/actualiser").header("Authorization", bearer(agentRapports)))
                .andExpect(status().isOk());
        assertThat(RECU.get("/api/v1/run_sql")).contains("SELECT 1");
    }

    @Test
    void lesAdressesDExportPourLIaSontOuvertesCommeDansLAncienneVersion() throws Exception {
        mockMvc.perform(get("/api/v1/ai/export-referentiels")).andExpect(status().isOk());
    }

    @Test
    void transcriptionDUnAudioInconnu() throws Exception {
        mockMvc.perform(put("/api/v1/claimaudio/999999999/transcription").header("Authorization", bearer(agentRapports))
                .contentType(MediaType.APPLICATION_JSON).content("{\"transcriptionRaw\":\"bonjour\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void retourDeLAgentSurUnDossierInconnu() throws Exception {
        mockMvc.perform(post("/api/v1/ia/retour/999999999").header("Authorization", bearer(agentRapports))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"VALIDATED\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/ia/retour/1").header("Authorization", bearer(agentRapports))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
