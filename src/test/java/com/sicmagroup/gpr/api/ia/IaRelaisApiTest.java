package com.sicmagroup.gpr.api.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.sicmagroup.gpr.service.ia.AnalyseIa;
import com.sicmagroup.gpr.service.ia.AnalysesIaRecentes;
import com.sicmagroup.gpr.service.jwt.JwtServiceImpl;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Relais IA : l'écran appelle /api/v1/ia/**, le serveur appelle le Core IA avec la clé.
 * Un faux Core IA tourne sur le PC pendant le test : aucun appel au vrai serveur, aucune vraie clé.
 */
@SpringBootTest
@AutoConfigureMockMvc
class IaRelaisApiTest {

    private static final String CLE_DE_TEST = "cle-factice-pour-les-tests";
    private static final String AGENT = "agent.ia@gpr.local";

    /** Ce que le faux Core a reçu, par adresse appelée. */
    private static final Map<String, String> RECU_CORPS = new ConcurrentHashMap<>();
    private static final Map<String, String> RECU_CLE = new ConcurrentHashMap<>();
    private static final Map<String, String> RECU_TYPE = new ConcurrentHashMap<>();

    private static final String FLUX_ANALYSE = """
            data: {"type":"init_base","sentiment":"negatif","mots_cles_detectes":["frais","grille"],"resume":"Conteste des frais."}

            data: {"type":"reasoning","delta":"Le client conteste "}

            data: {"type":"reasoning","delta":"des frais."}

            data: {"type":"init_urgence","urgence":"FAIBLE"}

            data: {"type":"final","categorie_suggeree":"Compte","motif_suggere":"Frais excessifs","urgence":"FAIBLE"}

            """;

    private static final HttpServer FAUX_CORE = demarrerFauxCore();

    private static HttpServer demarrerFauxCore() {
        try {
            HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            s.createContext("/api/v1/analyze/stream", ex -> repondre(ex, 200, "text/event-stream", FLUX_ANALYSE));
            s.createContext("/api/v1/analyze", ex -> repondre(ex, 200, "application/json",
                    "{\"categorie\":\"Compte\",\"motif\":\"Frais excessifs\",\"urgence\":\"MOYEN\",\"resume\":\"r\",\"correlation_id\":\"cai-core-1\"}"));
            s.createContext("/api/v1/copilot/stream", ex -> repondre(ex, 200, "text/event-stream",
                    "data: {\"type\":\"final\",\"options\":[]}\n\n"));
            s.createContext("/api/v1/copilot", ex -> repondre(ex, 200, "application/json", "{\"options\":[]}"));
            s.createContext("/api/v1/transcribe", ex -> repondre(ex, 200, "application/json",
                    "{\"transcription\":\"bonjour\"}"));
            s.start();
            return s;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void repondre(HttpExchange ex, int statut, String type, String corps) throws IOException {
        String chemin = ex.getRequestURI().getPath();
        RECU_CORPS.put(chemin, new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        RECU_CLE.put(chemin, String.valueOf(ex.getRequestHeaders().getFirst("X-API-Key")));
        RECU_TYPE.put(chemin, String.valueOf(ex.getRequestHeaders().getFirst("Content-Type")));
        byte[] b = corps.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", type);
        ex.sendResponseHeaders(statut, b.length);
        try (OutputStream o = ex.getResponseBody()) {
            o.write(b);
        }
    }

    @DynamicPropertySource
    static void adresseDuFauxCore(DynamicPropertyRegistry r) {
        r.add("gpr.ia.url", () -> "http://127.0.0.1:" + FAUX_CORE.getAddress().getPort());
        r.add("gpr.ia.cle", () -> CLE_DE_TEST);
    }

    @AfterAll
    static void arreterFauxCore() {
        FAUX_CORE.stop(0);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtServiceImpl jwtService;

    @Autowired
    private AnalysesIaRecentes analyses;

    @MockBean
    private UserDetailsService userDetailsService;

    private final UserDetails agent = User.withUsername(AGENT).password("x").authorities("H1").build();

    @BeforeEach
    void setUp() {
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(agent);
        RECU_CORPS.clear();
        RECU_CLE.clear();
        RECU_TYPE.clear();
    }

    private String bearer() {
        return "Bearer " + jwtService.generateToken(agent);
    }

    @Test
    void sansConnexionLIaEstRefusee() throws Exception {
        mockMvc.perform(post("/api/v1/ia/analyse/flux").contentType(MediaType.APPLICATION_JSON).content("{\"texte\":\"x\"}"))
                .andExpect(status().is4xxClientError());
        assertThat(RECU_CORPS).isEmpty();
    }

    @Test
    void etatIndiqueQueLIaEstConfigureeSansDonnerLaCle() throws Exception {
        mockMvc.perform(get("/api/v1/ia/etat").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.configuree").value(true))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(CLE_DE_TEST))));
    }

    @Test
    void analyseAuFilDeLEauEstRelayeeAvecLaCleEtGardeeParLeServeur() throws Exception {
        String texte = "{\"texte\":\"Le client conteste des frais de 4 500 FCFA.\",\"nature_dossier\":\"RECLAMATION\"}";
        mockMvc.perform(post("/api/v1/ia/analyse/flux").header("Authorization", bearer())
                .header("X-Correlation-ID", "cai-test-flux-1")
                .contentType(MediaType.APPLICATION_JSON).content(texte))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", "cai-test-flux-1"))
                .andExpect(content().string(FLUX_ANALYSE));

        assertThat(RECU_CLE.get("/api/v1/analyze/stream")).isEqualTo(CLE_DE_TEST);
        assertThat(RECU_CORPS.get("/api/v1/analyze/stream")).contains("4 500 FCFA");

        AnalyseIa a = analyses.recuperer("cai-test-flux-1", AGENT).orElseThrow();
        assertThat(a.getCategorie()).isEqualTo("Compte");
        assertThat(a.getMotif()).isEqualTo("Frais excessifs");
        assertThat(a.getUrgence()).isEqualTo("FAIBLE");
        assertThat(a.getSentiment()).isEqualTo("negatif");
        assertThat(a.getResume()).isEqualTo("Conteste des frais.");
        assertThat(a.getMotsCles()).containsExactly("frais", "grille");
        assertThat(a.getRaisonnementClassification()).isEqualTo("Le client conteste des frais.");
        // Une autre personne ne peut pas reprendre cette analyse.
        assertThat(analyses.recuperer("cai-test-flux-1", "autre@gpr.local")).isEmpty();
    }

    @Test
    void analyseEnUnBlocGardeAussiLeNumeroDonneParLeCore() throws Exception {
        mockMvc.perform(post("/api/v1/ia/analyse").header("Authorization", bearer())
                .header("X-Correlation-ID", "cai-test-bloc-1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"texte\":\"Frais de tenue de compte.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motif").value("Frais excessifs"));
        assertThat(analyses.recuperer("cai-test-bloc-1", AGENT)).isPresent();
        assertThat(analyses.recuperer("cai-core-1", AGENT)).isPresent();
    }

    @Test
    void solutionsLAgentConnecteEstRenseigneParLeServeur() throws Exception {
        mockMvc.perform(post("/api/v1/ia/solutions/flux").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texte\":\"Retrait non servi\",\"agent_connecte\":{\"identifiant\":\"PIRATE\",\"role\":\"DIRECTEUR\"}}"))
                .andExpect(status().isOk());
        String recu = RECU_CORPS.get("/api/v1/copilot/stream");
        assertThat(recu).contains("Retrait non servi").contains("correlation_id").doesNotContain("PIRATE")
                .doesNotContain("DIRECTEUR");
        assertThat(RECU_CLE.get("/api/v1/copilot/stream")).isEqualTo(CLE_DE_TEST);
    }

    @Test
    void transcriptionLeFichierEstTransmisAuCoreSousLeNomFile() throws Exception {
        MockMultipartFile audio = new MockMultipartFile("file", "message.ogg", "audio/ogg", "son-factice".getBytes());
        mockMvc.perform(multipart("/api/v1/ia/transcription").file(audio).param("mode_repli", "AUTO")
                .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcription").value("bonjour"));
        String recu = RECU_CORPS.get("/api/v1/transcribe");
        assertThat(recu).contains("name=\"file\"; filename=\"message.ogg\"").contains("son-factice")
                .contains("name=\"mode_repli\"");
        assertThat(RECU_TYPE.get("/api/v1/transcribe")).startsWith("multipart/form-data; boundary=");
        assertThat(RECU_CLE.get("/api/v1/transcribe")).isEqualTo(CLE_DE_TEST);
    }

    @Test
    void unFichierQuiNEstPasUnAudioEstRefuse() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("file", "doc.pdf", "application/pdf", "x".getBytes());
        mockMvc.perform(multipart("/api/v1/ia/transcription").file(pdf).header("Authorization", bearer()))
                .andExpect(status().isBadRequest());
        assertThat(RECU_CORPS).isEmpty();
    }

    @Test
    void uneDemandeQuiNEstPasDuJsonEstRefuseeSansAppelerLeCore() throws Exception {
        mockMvc.perform(post("/api/v1/ia/analyse").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content("[1,2]"))
                .andExpect(status().isBadRequest());
        assertThat(RECU_CORPS).isEmpty();
    }
}
