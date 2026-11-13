package com.sicmagroup.gpr.service.ia;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Relais entre GPR et le Core IA. Seul ce service connaît la clé d'accès (variable GPR_AI_API_KEY)
 * et l'ajoute aux appels (en-tête X-API-Key) : l'écran ne la voit jamais.
 * Le contenu des échanges est transmis tel quel, au format de l'ancienne version GPR.
 * Ni la clé ni le texte des plaintes ne sont écrits dans les journaux.
 */
@Service
public class IaRelaisService {

    public static final Duration DELAI_CONNEXION = Duration.ofSeconds(10);
    private static final Pattern CORRELATION_VALIDE = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    private static final ScheduledExecutorService MINUTEUR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ia-relais-minuteur");
        t.setDaemon(true);
        return t;
    });

    private final String url;
    private final String urlWren;
    private final String cle;
    private final HttpClient http;

    public IaRelaisService(@Value("${gpr.ia.url:}") String url, @Value("${gpr.ia.cle:}") String cle) {
        this(url, url, cle);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public IaRelaisService(@Value("${gpr.ia.url:}") String url, @Value("${gpr.ia.wren-url:${gpr.ia.url:}}") String urlWren,
            @Value("${gpr.ia.cle:}") String cle) {
        this.url = url == null ? "" : url.trim().replaceAll("/+$", "");
        this.urlWren = urlWren == null || urlWren.isBlank() ? this.url : urlWren.trim().replaceAll("/+$", "");
        this.cle = cle == null ? "" : cle.trim();
        // HTTP/1.1 imposé : par défaut Java tente HTTP/2 (« Upgrade: h2c »), que le serveur de l'IA (uvicorn)
        // refuse avec « Invalid HTTP request received » (400).
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(DELAI_CONNEXION)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /** Vrai si l'adresse du Core et la clé sont renseignées. */
    public boolean estConfiguree() {
        return !url.isEmpty() && !cle.isEmpty();
    }

    /** Reprend le numéro d'analyse donné par l'écran s'il est valide, sinon en crée un (format de l'ancienne version). */
    public static String correlationId(String propose) {
        if (propose != null && CORRELATION_VALIDE.matcher(propose).matches()) return propose;
        return "cai-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 7);
    }

    /** Réponse complète du Core (statut, corps, type de contenu, en-têtes utiles). */
    public record ReponseIa(int statut, byte[] corps, String typeContenu, Map<String, String> entetes) {
    }

    /** Appel en un bloc : la réponse est lue en entier. */
    public ReponseIa envoyer(String chemin, byte[] corps, String typeContenu, String correlationId, Duration delai)
            throws IOException, InterruptedException {
        return envoyerA(url, chemin, corps, typeContenu, correlationId, delai);
    }

    /** Même chose vers Wren (rapports IA). */
    public ReponseIa envoyerWren(String chemin, byte[] corps, String correlationId, Duration delai)
            throws IOException, InterruptedException {
        return envoyerA(urlWren, chemin, corps, "application/json", correlationId, delai);
    }

    private ReponseIa envoyerA(String base, String chemin, byte[] corps, String typeContenu, String correlationId,
            Duration delai) throws IOException, InterruptedException {
        HttpResponse<byte[]> r = http.send(requete(base, chemin, corps, typeContenu, correlationId, delai, "application/json"),
                HttpResponse.BodyHandlers.ofByteArray());
        return new ReponseIa(r.statusCode(), r.body(),
                r.headers().firstValue("Content-Type").orElse("application/json"), entetesUtiles(r));
    }

    /**
     * Appel au fil de l'eau : renvoie le flux du Core dès réception des en-têtes. Le flux est coupé
     * d'office au bout de {@code dureeMax} pour ne jamais bloquer le serveur si le Core ne termine pas.
     */
    public FluxIa ouvrirFlux(String chemin, byte[] corps, String typeContenu, String correlationId, Duration delaiPremiereReponse,
            Duration dureeMax) throws IOException, InterruptedException {
        return ouvrirFluxA(url, chemin, corps, typeContenu, correlationId, delaiPremiereReponse, dureeMax);
    }

    /** Même chose vers Wren (rapports IA). */
    public FluxIa ouvrirFluxWren(String chemin, byte[] corps, String correlationId, Duration delaiPremiereReponse,
            Duration dureeMax) throws IOException, InterruptedException {
        return ouvrirFluxA(urlWren, chemin, corps, "application/json", correlationId, delaiPremiereReponse, dureeMax);
    }

    private FluxIa ouvrirFluxA(String base, String chemin, byte[] corps, String typeContenu, String correlationId,
            Duration delaiPremiereReponse, Duration dureeMax) throws IOException, InterruptedException {
        HttpResponse<InputStream> r = http.send(
                requete(base, chemin, corps, typeContenu, correlationId, delaiPremiereReponse, "text/event-stream"),
                HttpResponse.BodyHandlers.ofInputStream());
        InputStream in = r.body();
        ScheduledFuture<?> coupure = MINUTEUR.schedule(() -> {
            try {
                in.close();
            } catch (IOException ignore) {
                // déjà fermé
            }
        }, dureeMax.toMillis(), TimeUnit.MILLISECONDS);
        return new FluxIa(r.statusCode(), in, r.headers().firstValue("Content-Type").orElse("text/event-stream"),
                entetesUtiles(r), coupure);
    }

    /** Flux ouvert vers le Core ; à fermer après usage. */
    public record FluxIa(int statut, InputStream corps, String typeContenu, Map<String, String> entetes,
            ScheduledFuture<?> coupure) implements AutoCloseable {
        @Override
        public void close() {
            coupure.cancel(false);
            try {
                corps.close();
            } catch (IOException ignore) {
                // déjà fermé
            }
        }
    }

    private HttpRequest requete(String base, String chemin, byte[] corps, String typeContenu, String correlationId,
            Duration delai, String accept) {
        return HttpRequest.newBuilder(URI.create(base + chemin))
                .timeout(delai)
                .header("Content-Type", typeContenu)
                .header("Accept", accept)
                .header("X-API-Key", cle)
                .header("X-Correlation-ID", correlationId)
                .POST(HttpRequest.BodyPublishers.ofByteArray(corps))
                .build();
    }

    private static Map<String, String> entetesUtiles(HttpResponse<?> r) {
        java.util.LinkedHashMap<String, String> m = new java.util.LinkedHashMap<>();
        for (String nom : new String[] { "X-SLA-Status", "X-Response-Time", "X-Correlation-ID" }) {
            r.headers().firstValue(nom).ifPresent(v -> m.put(nom, v));
        }
        return m;
    }
}
