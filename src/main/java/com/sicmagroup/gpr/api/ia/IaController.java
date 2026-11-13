package com.sicmagroup.gpr.api.ia;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.ia.AnalyseIa;
import com.sicmagroup.gpr.service.ia.AnalysesIaRecentes;
import com.sicmagroup.gpr.service.ia.IaRelaisService;
import com.sicmagroup.gpr.service.ia.IaRelaisService.FluxIa;
import com.sicmagroup.gpr.service.ia.IaRelaisService.ReponseIa;
import com.sicmagroup.gpr.service.ia.LectureAnalyseIa;
import com.sicmagroup.gpr.utils.CurrentUserUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Relais vers le Core IA : l'écran appelle ces adresses avec sa connexion GPR habituelle,
 * le serveur contacte le Core IA avec la clé (que l'écran ne connaît pas) et renvoie la réponse
 * telle quelle, en un bloc ou au fil de l'eau.
 * Réservé aux personnes connectées (règle générale de SecurityConfig).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ia")
@RequiredArgsConstructor
public class IaController {

    static final int TAILLE_MAX_JSON = 1024 * 1024;
    static final int TAILLE_MAX_LECTURE_ANALYSE = 2 * 1024 * 1024;

    private static final Duration ANALYSE = Duration.ofSeconds(30);
    private static final Duration ANALYSE_FLUX = Duration.ofSeconds(120);
    private static final Duration SOLUTIONS = Duration.ofSeconds(60);
    private static final Duration SOLUTIONS_FLUX = Duration.ofSeconds(180);
    private static final Duration TRANSCRIPTION = Duration.ofSeconds(120);
    private static final Duration TRANSCRIPTION_FLUX = Duration.ofSeconds(300);

    private final IaRelaisService relais;
    private final AnalysesIaRecentes analyses;
    private final CurrentUserUtils utilisateurCourant;
    private final com.sicmagroup.gpr.service.ia.RetourAgentIa retourAgent;
    private final ObjectMapper json;

    /** Indique à l'écran si l'IA est disponible sur ce serveur (sans rien dire de la clé). */
    @GetMapping("/etat")
    public Map<String, Object> etat() {
        return Map.of("status", true, "content", Map.of("configuree", relais.estConfiguree()));
    }

    // ---------------------------------------------------------------- Analyse (enregistrement)

    @PostMapping(value = "/analyse", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void analyse(@RequestBody byte[] corps, @RequestHeader(value = "X-Correlation-ID", required = false) String id,
            Principal qui, HttpServletResponse res) throws IOException {
        String correlation = IaRelaisService.correlationId(id);
        Optional<byte[]> payload = verifierJson(corps, res, n -> n);
        if (payload.isEmpty()) return;
        relayerBloc("/api/v1/analyze", payload.get(), MediaType.APPLICATION_JSON_VALUE, correlation, ANALYSE, res,
                reponse -> garderAnalyse(LectureAnalyseIa.depuisJson(reponse, json), correlation, qui));
    }

    @PostMapping(value = "/analyse/flux", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void analyseFlux(@RequestBody byte[] corps, @RequestHeader(value = "X-Correlation-ID", required = false) String id,
            Principal qui, HttpServletResponse res) throws IOException {
        String correlation = IaRelaisService.correlationId(id);
        Optional<byte[]> payload = verifierJson(corps, res, n -> n);
        if (payload.isEmpty()) return;
        relayerFlux("/api/v1/analyze/stream", payload.get(), MediaType.APPLICATION_JSON_VALUE, correlation, ANALYSE_FLUX,
                res, texte -> garderAnalyse(LectureAnalyseIa.depuisFlux(texte, json), correlation, qui));
    }

    // ---------------------------------------------------------------- Solutions (traitement)

    @PostMapping(value = "/solutions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void solutions(@RequestBody byte[] corps, @RequestHeader(value = "X-Correlation-ID", required = false) String id,
            HttpServletResponse res) throws IOException {
        String correlation = IaRelaisService.correlationId(id);
        Optional<byte[]> payload = verifierJson(corps, res, n -> completerCopilote(n, correlation));
        if (payload.isEmpty()) return;
        relayerBloc("/api/v1/copilot", payload.get(), MediaType.APPLICATION_JSON_VALUE, correlation, SOLUTIONS, res, null);
    }

    @PostMapping(value = "/solutions/flux", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void solutionsFlux(@RequestBody byte[] corps, @RequestHeader(value = "X-Correlation-ID", required = false) String id,
            HttpServletResponse res) throws IOException {
        String correlation = IaRelaisService.correlationId(id);
        Optional<byte[]> payload = verifierJson(corps, res, n -> completerCopilote(n, correlation));
        if (payload.isEmpty()) return;
        relayerFlux("/api/v1/copilot/stream", payload.get(), MediaType.APPLICATION_JSON_VALUE, correlation, SOLUTIONS_FLUX,
                res, null);
    }

    // ---------------------------------------------------------------- Transcription (voix)

    @PostMapping(value = "/transcription", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void transcription(@RequestParam("file") MultipartFile fichier, HttpServletRequest req,
            @RequestHeader(value = "X-Correlation-ID", required = false) String id, HttpServletResponse res) throws IOException {
        String correlation = IaRelaisService.correlationId(id);
        if (!verifierAudio(fichier, res)) return;
        String frontiere = "----gpr-ia-" + UUID.randomUUID();
        relayerBloc("/api/v1/transcribe", multipart(fichier, req, frontiere), "multipart/form-data; boundary=" + frontiere,
                correlation, TRANSCRIPTION, res, null);
    }

    @PostMapping(value = "/transcription/flux", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void transcriptionFlux(@RequestParam("file") MultipartFile fichier, HttpServletRequest req,
            @RequestHeader(value = "X-Correlation-ID", required = false) String id, HttpServletResponse res) throws IOException {
        String correlation = IaRelaisService.correlationId(id);
        if (!verifierAudio(fichier, res)) return;
        String frontiere = "----gpr-ia-" + UUID.randomUUID();
        relayerFlux("/api/v1/transcribe/stream", multipart(fichier, req, frontiere),
                "multipart/form-data; boundary=" + frontiere, correlation, TRANSCRIPTION_FLUX, res, null);
    }

    // ---------------------------------------------------------------- Retour de l'agent (traitement)

    /** Ce que l'agent a retenu de la proposition de l'IA. L'agent est celui qui est connecté. */
    @PostMapping("/retour/{claimId}")
    public org.springframework.http.ResponseEntity<Map<String, Object>> retour(@org.springframework.web.bind.annotation.PathVariable Long claimId,
            @RequestBody com.sicmagroup.gpr.api.claim.AiFeedbackRequest demande) {
        User agent = null;
        try {
            agent = utilisateurCourant.getUser();
        } catch (Exception ignore) {
            // agent inconnu : le retour est gardé sans auteur
        }
        try {
            com.sicmagroup.gpr.domain.model.Claim c = retourAgent.enregistrer(claimId, demande, agent);
            return org.springframework.http.ResponseEntity.ok(Map.of("status", true,
                    "content", Map.of("id", c.getId(), "aiQualityScore", c.getAiQualityScore())));
        } catch (java.util.NoSuchElementException e) {
            return org.springframework.http.ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", false, "content", Map.of("message", "Dossier introuvable")));
        } catch (IllegalArgumentException e) {
            return org.springframework.http.ResponseEntity.badRequest()
                    .body(Map.of("status", false, "content", Map.of("message", e.getMessage())));
        }
    }

    // ---------------------------------------------------------------- Relais

    private void relayerBloc(String chemin, byte[] corps, String type, String correlation, Duration delai,
            HttpServletResponse res, java.util.function.Consumer<String> apres) throws IOException {
        if (!relais.estConfiguree()) {
            erreur(res, HttpStatus.SERVICE_UNAVAILABLE, "IA NON CONFIGUREE", "L'IA n'est pas configurée sur ce serveur.");
            return;
        }
        long debut = System.currentTimeMillis();
        ReponseIa r;
        try {
            r = relais.envoyer(chemin, corps, type, correlation, delai);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            indisponible(res, chemin, e);
            return;
        } catch (IOException e) {
            indisponible(res, chemin, e);
            return;
        }
        log.info("IA {} -> {} en {} ms ({})", chemin, r.statut(), System.currentTimeMillis() - debut, correlation);
        res.setStatus(r.statut());
        res.setContentType(r.typeContenu());
        res.setHeader("X-Correlation-ID", correlation);
        r.entetes().forEach((k, v) -> {
            if (!"X-Correlation-ID".equals(k)) res.setHeader(k, v);
        });
        res.getOutputStream().write(r.corps());
        if (apres != null && r.statut() >= 200 && r.statut() < 300) {
            apres.accept(new String(r.corps(), StandardCharsets.UTF_8));
        }
    }

    private void relayerFlux(String chemin, byte[] corps, String type, String correlation, Duration dureeMax,
            HttpServletResponse res, java.util.function.Consumer<String> apres) throws IOException {
        if (!relais.estConfiguree()) {
            erreur(res, HttpStatus.SERVICE_UNAVAILABLE, "IA NON CONFIGUREE", "L'IA n'est pas configurée sur ce serveur.");
            return;
        }
        long debut = System.currentTimeMillis();
        FluxIa flux;
        try {
            flux = relais.ouvrirFlux(chemin, corps, type, correlation, Duration.ofSeconds(60), dureeMax);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            indisponible(res, chemin, e);
            return;
        } catch (IOException e) {
            indisponible(res, chemin, e);
            return;
        }
        try (flux) {
            res.setStatus(flux.statut());
            res.setHeader("X-Correlation-ID", correlation);
            flux.entetes().forEach((k, v) -> {
                if (!"X-Correlation-ID".equals(k)) res.setHeader(k, v);
            });
            boolean ok = flux.statut() >= 200 && flux.statut() < 300;
            if (ok) {
                res.setContentType("text/event-stream;charset=UTF-8");
                res.setHeader("Cache-Control", "no-cache");
                res.setHeader("X-Accel-Buffering", "no");
            } else {
                res.setContentType(flux.typeContenu());
            }
            ByteArrayOutputStream copie = (apres != null && ok) || !ok ? new ByteArrayOutputStream() : null;
            boolean coupe = transmettre(flux.corps(), res.getOutputStream(), copie);
            log.info("IA {} (flux) -> {} en {} ms{} ({})", chemin, flux.statut(), System.currentTimeMillis() - debut,
                    coupe ? ", interrompu" : "", correlation);
            if (!ok && copie != null) {
                String raison = copie.toString(StandardCharsets.UTF_8);
                log.warn("IA {} refusé ({}) : {}", chemin, flux.statut(), raison.length() > 1000 ? raison.substring(0, 1000) : raison);
                return;
            }
            if (copie != null && !coupe) apres.accept(copie.toString(StandardCharsets.UTF_8));
        }
    }

    /** Recopie le flux du Core vers l'écran morceau par morceau. Renvoie vrai si l'écran a quitté avant la fin. */
    static boolean transmettre(InputStream in, OutputStream out, ByteArrayOutputStream copie) {
        byte[] tampon = new byte[4096];
        int n;
        try {
            while ((n = in.read(tampon)) != -1) {
                try {
                    out.write(tampon, 0, n);
                    out.flush();
                } catch (IOException ecranParti) {
                    return true;
                }
                if (copie != null && copie.size() + n <= TAILLE_MAX_LECTURE_ANALYSE) copie.write(tampon, 0, n);
            }
        } catch (IOException fluxCoupe) {
            // Core arrêté ou durée maximale atteinte : on termine proprement ce qui a été reçu.
        }
        return false;
    }

    // ---------------------------------------------------------------- Contrôles et compléments

    private Optional<byte[]> verifierJson(byte[] corps, HttpServletResponse res, Function<ObjectNode, ObjectNode> completer)
            throws IOException {
        if (corps == null || corps.length == 0 || corps.length > TAILLE_MAX_JSON) {
            erreur(res, HttpStatus.BAD_REQUEST, "REQUETE IA INVALIDE", "La demande est vide ou trop volumineuse.");
            return Optional.empty();
        }
        JsonNode n;
        try {
            n = json.readTree(corps);
        } catch (IOException e) {
            n = null;
        }
        if (n == null || !n.isObject()) {
            erreur(res, HttpStatus.BAD_REQUEST, "REQUETE IA INVALIDE", "La demande n'est pas un objet JSON.");
            return Optional.empty();
        }
        return Optional.of(json.writeValueAsBytes(completer.apply((ObjectNode) n)));
    }

    /**
     * Le serveur renseigne lui-même l'agent connecté et le numéro d'analyse :
     * l'écran ne peut pas se faire passer pour un autre agent (contrôle d'habilitation du Core).
     */
    private ObjectNode completerCopilote(ObjectNode n, String correlation) {
        n.put("correlation_id", correlation);
        ObjectNode agent = n.putObject("agent_connecte");
        try {
            User u = utilisateurCourant.getUser();
            agent.put("identifiant", u.getCode() != null ? u.getCode() : String.valueOf(u.getId()));
            agent.put("nom", u.getFirstandlastname());
            agent.put("role", u.getPoste() != null && u.getPoste().getLibelle() != null ? u.getPoste().getLibelle() : "AGENT");
        } catch (Exception e) {
            agent.put("identifiant", "INCONNU");
            agent.put("nom", "");
            agent.put("role", "AGENT");
        }
        return n;
    }

    private boolean verifierAudio(MultipartFile fichier, HttpServletResponse res) throws IOException {
        if (fichier == null || fichier.isEmpty()) {
            erreur(res, HttpStatus.BAD_REQUEST, "REQUETE IA INVALIDE", "Aucun fichier audio reçu.");
            return false;
        }
        String type = fichier.getContentType();
        if (type != null && !type.startsWith("audio/") && !type.startsWith("video/")
                && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(type)) {
            erreur(res, HttpStatus.BAD_REQUEST, "REQUETE IA INVALIDE", "Le fichier envoyé n'est pas un audio.");
            return false;
        }
        return true;
    }

    /** Reconstruit le formulaire pour le Core : le fichier (champ « file ») et les autres champs reçus. */
    private static byte[] multipart(MultipartFile fichier, HttpServletRequest req, String frontiere) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        String fin = "\r\n";
        for (Map.Entry<String, String[]> champ : req.getParameterMap().entrySet()) {
            if ("file".equals(champ.getKey())) continue;
            for (String valeur : champ.getValue()) {
                b.write(("--" + frontiere + fin).getBytes(StandardCharsets.UTF_8));
                b.write(("Content-Disposition: form-data; name=\"" + nettoyer(champ.getKey()) + "\"" + fin + fin)
                        .getBytes(StandardCharsets.UTF_8));
                b.write(valeur.getBytes(StandardCharsets.UTF_8));
                b.write(fin.getBytes(StandardCharsets.UTF_8));
            }
        }
        String nom = fichier.getOriginalFilename() != null ? fichier.getOriginalFilename() : "audio";
        String type = fichier.getContentType() != null ? fichier.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        b.write(("--" + frontiere + fin).getBytes(StandardCharsets.UTF_8));
        b.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + nettoyer(nom) + "\"" + fin)
                .getBytes(StandardCharsets.UTF_8));
        b.write(("Content-Type: " + nettoyer(type) + fin + fin).getBytes(StandardCharsets.UTF_8));
        b.write(fichier.getBytes());
        b.write((fin + "--" + frontiere + "--" + fin).getBytes(StandardCharsets.UTF_8));
        return b.toByteArray();
    }

    private static String nettoyer(String s) {
        return s.replaceAll("[\"\\r\\n]", "_");
    }

    private void garderAnalyse(Optional<AnalyseIa> analyse, String correlation, Principal qui) {
        if (qui == null) return;
        analyse.filter(AnalyseIa::estComplete).ifPresent(a -> {
            if (a.getCorrelationId() == null) a.setCorrelationId(correlation);
            analyses.enregistrer(a, qui.getName());
            if (!a.getCorrelationId().equals(correlation)) {
                // Le Core a donné son propre numéro : l'analyse est retrouvable avec les deux.
                AnalyseIa copie = new AnalyseIa();
                org.springframework.beans.BeanUtils.copyProperties(a, copie);
                copie.setCorrelationId(correlation);
                analyses.enregistrer(copie, qui.getName());
            }
        });
    }

    private void indisponible(HttpServletResponse res, String chemin, Exception e) throws IOException {
        String cause = e instanceof HttpTimeoutException ? "délai dépassé"
                : e instanceof ConnectException ? "connexion refusée" : e.getClass().getSimpleName();
        log.warn("IA {} indisponible : {}", chemin, cause);
        erreur(res, HttpStatus.BAD_GATEWAY, "IA INDISPONIBLE", "Le service d'IA ne répond pas. Réessayez dans un instant.");
    }

    private void erreur(HttpServletResponse res, HttpStatus statut, String titre, String message) throws IOException {
        res.setStatus(statut.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(json.writeValueAsString(
                Map.of("status", false, "content", Map.of("title", titre, "message", message))));
    }
}
