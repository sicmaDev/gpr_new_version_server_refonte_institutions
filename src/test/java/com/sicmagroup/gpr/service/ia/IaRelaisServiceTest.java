package com.sicmagroup.gpr.service.ia;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

/** Relais IA et lecture des réponses du Core, sans démarrer l'application. */
class IaRelaisServiceTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void sansCleLIaNEstPasConfiguree() {
        assertThat(new IaRelaisService("http://exemple:8000", "").estConfiguree()).isFalse();
        assertThat(new IaRelaisService("http://exemple:8000", "   ").estConfiguree()).isFalse();
        assertThat(new IaRelaisService("", "cle").estConfiguree()).isFalse();
        assertThat(new IaRelaisService("http://exemple:8000/", "cle").estConfiguree()).isTrue();
    }

    @Test
    void numeroDAnalyseRepris() {
        assertThat(IaRelaisService.correlationId("cai-123-abc")).isEqualTo("cai-123-abc");
        assertThat(IaRelaisService.correlationId("mauvais id\r\nX-Inject: 1")).startsWith("cai-").doesNotContain("\n");
        assertThat(IaRelaisService.correlationId(null)).startsWith("cai-");
    }

    @Test
    void coreArreteDonneUneErreurDeConnexion() {
        IaRelaisService relais = new IaRelaisService("http://127.0.0.1:1", "cle");
        try {
            relais.envoyer("/api/v1/analyze", "{}".getBytes(), "application/json", "cai-1", Duration.ofSeconds(3));
        } catch (Exception e) {
            assertThat(e).isInstanceOf(ConnectException.class);
            return;
        }
        throw new AssertionError("une erreur de connexion était attendue");
    }

    @Test
    void unFluxQuiNeFinitJamaisEstCoupeAuBoutDeLaDureeMax() throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/lent", ex -> {
            ex.sendResponseHeaders(200, 0);
            OutputStream o = ex.getResponseBody();
            o.write("data: {\"type\":\"init_base\"}\n\n".getBytes());
            o.flush();
            try {
                Thread.sleep(6_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            o.close();
        });
        s.start();
        try {
            IaRelaisService relais = new IaRelaisService("http://127.0.0.1:" + s.getAddress().getPort(), "cle");
            long debut = System.currentTimeMillis();
            try (IaRelaisService.FluxIa flux = relais.ouvrirFlux("/lent", "{}".getBytes(), "application/json", "cai-1",
                    Duration.ofSeconds(5), Duration.ofSeconds(1))) {
                try {
                    flux.corps().readAllBytes();
                } catch (java.io.IOException coupe) {
                    // coupure attendue
                }
            }
            // Coupé au bout d'1 s, bien avant les 6 s où le faux Core aurait fini.
            assertThat(System.currentTimeMillis() - debut).isLessThan(4_000);
        } finally {
            s.stop(0);
        }
    }

    @Test
    void lectureDUnFluxDAnalyseFormatAncien() {
        String flux = "data: {\"type\":\"init_base\",\"sentiment\":\"positif\",\"resume\":\"ok\"}\r\n\r\n"
                + "data: {\"type\":\"raisonnement_urgence_chunk\",\"content\":\"Pas urgent.\"}\n\n"
                + "data: {\"type\":\"raisonnement_chunk\",\"content\":\"Frais.\"}\n\n"
                + "data: {\"type\":\"final\",\"result\":{\"category\":\"Compte\",\"motif\":\"Frais\"},\"urgence\":\"GRAVE\"}\n\n";
        AnalyseIa a = LectureAnalyseIa.depuisFlux(flux, json).orElseThrow();
        assertThat(a.getCategorie()).isEqualTo("Compte");
        assertThat(a.getMotif()).isEqualTo("Frais");
        assertThat(a.getUrgence()).isEqualTo("GRAVE");
        assertThat(a.getSentiment()).isEqualTo("positif");
        assertThat(a.getRaisonnementUrgence()).isEqualTo("Pas urgent.");
        assertThat(a.getRaisonnementClassification()).isEqualTo("Frais.");
        assertThat(a.estComplete()).isTrue();
    }

    @Test
    void fluxInterrompuAvantLaFinNEstPasComplet() {
        AnalyseIa a = LectureAnalyseIa.depuisFlux("data: {\"type\":\"init_base\",\"resume\":\"x\"}\n\n", json).orElseThrow();
        assertThat(a.estComplete()).isFalse();
        assertThat(LectureAnalyseIa.depuisFlux("pas du tout un flux", json)).isEmpty();
    }

    @Test
    void laMemoireNeRendLAnalyseQuASonAuteur() {
        AnalysesIaRecentes m = new AnalysesIaRecentes();
        AnalyseIa a = new AnalyseIa();
        a.setCorrelationId("cai-9");
        a.setMotif("Frais");
        m.enregistrer(a, "a@gpr");
        assertThat(m.recuperer("cai-9", "a@gpr")).isPresent();
        assertThat(m.recuperer("cai-9", "b@gpr")).isEmpty();
        assertThat(m.recuperer("cai-inconnu", "a@gpr")).isEmpty();
    }

    @Test
    void laMemoireResteBornee() {
        AnalysesIaRecentes m = new AnalysesIaRecentes();
        for (int i = 0; i < AnalysesIaRecentes.TAILLE_MAX + 50; i++) {
            AnalyseIa a = new AnalyseIa();
            a.setCorrelationId("cai-" + i);
            m.enregistrer(a, "a@gpr");
        }
        assertThat(m.taille()).isLessThanOrEqualTo(AnalysesIaRecentes.TAILLE_MAX);
    }
}
