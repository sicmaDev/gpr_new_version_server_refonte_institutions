package com.sicmagroup.gpr.api.ia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sicmagroup.gpr.domain.dto.ai.AiConversationRequestDto;
import com.sicmagroup.gpr.domain.dto.ai.PinnedWidgetRequestDto;
import com.sicmagroup.gpr.domain.dto.ai.PinnedWidgetResponseDto;
import com.sicmagroup.gpr.domain.dto.ai.UserInstructionRequestDto;
import com.sicmagroup.gpr.domain.dto.ai.UserInstructionResponseDto;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.repository.ServicePointRepository;
import com.sicmagroup.gpr.service.ai.AiConversationService;
import com.sicmagroup.gpr.service.ai.PinnedWidgetService;
import com.sicmagroup.gpr.service.ai.UserInstructionService;
import com.sicmagroup.gpr.service.ia.IaRelaisService;
import com.sicmagroup.gpr.service.ia.IaRelaisService.FluxIa;
import com.sicmagroup.gpr.service.ia.IaRelaisService.ReponseIa;
import com.sicmagroup.gpr.service.report.PerimetreRapport;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Page « Reporting IA » : questions posées à Wren, conversations, graphiques épinglés et règles personnelles.
 * Réservé aux personnes qui ont accès aux rapports (H11, PILOTE, DE : règle de SecurityConfig).
 * Chaque personne ne voit que ses propres conversations, épingles et règles (identifiant pris dans la connexion).
 * Un responsable d'agence ou un agent H11 ne peut interroger que son périmètre d'agences : le serveur l'impose
 * dans la question, comme pour les rapports classiques.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ia/rapports")
@RequiredArgsConstructor
public class IaRapportsController {

    private static final Pattern UUID = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final int QUESTION_MAX = 2000;
    private static final int REGLES_MAX = 10;

    private final IaRelaisService relais;
    private final AiConversationService conversations;
    private final PinnedWidgetService epingles;
    private final UserInstructionService regles;
    private final PerimetreRapport perimetre;
    private final ServicePointRepository pointsDeService;
    private final ObjectMapper json;

    // ---------------------------------------------------------------- Question à Wren (au fil de l'eau)

    @PostMapping(value = "/question", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void question(@RequestBody Map<String, Object> demande, Principal qui, HttpServletResponse res) throws IOException {
        Object q = demande.get("question");
        String question = q == null ? "" : q.toString().trim();
        if (question.isEmpty() || question.length() > QUESTION_MAX) {
            erreur(res, HttpStatus.BAD_REQUEST, "La question est vide ou trop longue.");
            return;
        }
        if (!relais.estConfiguree()) {
            erreur(res, HttpStatus.SERVICE_UNAVAILABLE, "L'IA n'est pas configurée sur ce serveur.");
            return;
        }
        Object fil = demande.get("threadId");
        ObjectNode corps = json.createObjectNode();
        corps.put("question", questionEncadree(question, qui.getName()));
        corps.put("language", "FR");
        if (fil != null && UUID.matcher(fil.toString()).matches()) corps.put("threadId", fil.toString());
        corps.put("sampleSize", 500);

        String correlation = IaRelaisService.correlationId(null);
        long debut = System.currentTimeMillis();
        FluxIa flux;
        try {
            flux = relais.ouvrirFluxWren("/api/v1/reporting/stream/ask", json.writeValueAsBytes(corps), correlation,
                    Duration.ofSeconds(60), Duration.ofMinutes(4));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            indisponible(res, e);
            return;
        } catch (IOException e) {
            indisponible(res, e);
            return;
        }
        try (flux) {
            res.setStatus(flux.statut());
            boolean ok = flux.statut() >= 200 && flux.statut() < 300;
            res.setContentType(ok ? "text/event-stream;charset=UTF-8" : flux.typeContenu());
            if (ok) {
                res.setHeader("Cache-Control", "no-cache");
                res.setHeader("X-Accel-Buffering", "no");
            }
            boolean coupe = IaController.transmettre(flux.corps(), res.getOutputStream(), null);
            log.info("IA Wren question -> {} en {} ms{} ({})", flux.statut(), System.currentTimeMillis() - debut,
                    coupe ? ", interrompu" : "", correlation);
        }
    }

    /**
     * Ajoute à la question les consignes imposées par le serveur : périmètre d'agences de la personne
     * (si elle n'a pas l'accès complet) et ses règles personnelles actives (10 au plus).
     */
    String questionEncadree(String question, String utilisateur) {
        StringBuilder q = new StringBuilder();
        List<UserInstructionResponseDto> actives;
        try {
            actives = regles.getUserInstructions(utilisateur).stream().filter(UserInstructionResponseDto::isActive)
                    .limit(REGLES_MAX).toList();
        } catch (Exception e) {
            actives = List.of();
        }
        if (!actives.isEmpty()) {
            q.append("[Consignes spécifiques collaborateur :\n");
            actives.forEach(r -> q.append("- ").append(r.getInstruction()).append('\n'));
            q.append("]\n");
        }
        List<Long> agences = perimetre.agencesImposeesOuNull();
        if (agences != null) {
            String noms = agences.isEmpty() ? "aucune agence"
                    : pointsDeService.findAllById(agences).stream().map(ServicePoint::getLibelle)
                            .map(n -> "'" + n.replace("'", " ") + "'").collect(Collectors.joining(", "));
            q.append(agences.size() > 1 ? "Pour les agences " : "Pour l'agence ").append(noms)
                    .append(" uniquement (ne jamais donner de chiffres d'autres agences) : ");
        }
        return q.append(question).toString();
    }

    // ---------------------------------------------------------------- Actualiser un graphique épinglé

    /** Relance, sur Wren, la requête SQL d'un graphique épinglé par la personne (et seulement celle-là). */
    @PostMapping("/epingles/{id}/actualiser")
    public void actualiser(@PathVariable String id, Principal qui, HttpServletResponse res) throws IOException {
        String sql = epingles.getWidgets(qui.getName()).stream().filter(w -> id.equals(w.getId()))
                .map(PinnedWidgetResponseDto::getSqlQuery).findFirst().orElse(null);
        if (sql == null || sql.isBlank()) {
            erreur(res, HttpStatus.NOT_FOUND, "Graphique introuvable ou sans requête à relancer.");
            return;
        }
        if (!sql.trim().toLowerCase().matches("(?s)^(select|with)\\b.*")) {
            erreur(res, HttpStatus.BAD_REQUEST, "Seule une requête de lecture peut être relancée.");
            return;
        }
        if (!relais.estConfiguree()) {
            erreur(res, HttpStatus.SERVICE_UNAVAILABLE, "L'IA n'est pas configurée sur ce serveur.");
            return;
        }
        try {
            ReponseIa r = relais.envoyerWren("/api/v1/run_sql", json.writeValueAsBytes(Map.of("sql", sql.trim())),
                    IaRelaisService.correlationId(null), Duration.ofSeconds(60));
            res.setStatus(r.statut());
            res.setContentType(r.typeContenu());
            res.getOutputStream().write(r.corps());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            indisponible(res, e);
        } catch (IOException e) {
            indisponible(res, e);
        }
    }

    // ---------------------------------------------------------------- Règles valables pour toute la banque (Wren)

    /** Liste, création, modification ou suppression des règles globales de Wren (GraphQL). Modifier : H12 seulement. */
    @PostMapping(value = "/regles-globales", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void reglesGlobales(@RequestBody byte[] corps, Authentication auth, HttpServletResponse res) throws IOException {
        JsonNode n;
        try {
            n = json.readTree(corps);
        } catch (IOException e) {
            n = null;
        }
        if (n == null || !n.isObject() || !n.path("query").isTextual()) {
            erreur(res, HttpStatus.BAD_REQUEST, "Demande invalide.");
            return;
        }
        String requete = n.get("query").asText();
        String r = requete.replaceAll("\\s+", " ").toLowerCase();
        boolean modification = r.contains("mutation");
        boolean surLesRegles = r.contains("instruction");
        if (!surLesRegles) {
            erreur(res, HttpStatus.FORBIDDEN, "Seules les règles métier sont accessibles ici.");
            return;
        }
        if (modification && auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).noneMatch("H12"::equals)) {
            erreur(res, HttpStatus.FORBIDDEN, "Seul un administrateur (H12) peut modifier les règles de toute la banque.");
            return;
        }
        if (!relais.estConfiguree()) {
            erreur(res, HttpStatus.SERVICE_UNAVAILABLE, "L'IA n'est pas configurée sur ce serveur.");
            return;
        }
        try {
            ReponseIa rep = relais.envoyerWren("/api/graphql", corps, IaRelaisService.correlationId(null), Duration.ofSeconds(30));
            res.setStatus(rep.statut());
            res.setContentType(rep.typeContenu());
            res.getOutputStream().write(rep.corps());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            indisponible(res, e);
        } catch (IOException e) {
            indisponible(res, e);
        }
    }

    // ---------------------------------------------------------------- Conversations

    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> listeConversations(Principal qui) {
        return ok(conversations.getConversations(qui.getName()));
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<Map<String, Object>> conversation(@PathVariable String id, Principal qui) {
        return conversations.getConversation(qui.getName(), id).map(c -> ok((Object) c))
                .orElseGet(() -> nonTrouve("Conversation introuvable."));
    }

    @PostMapping("/conversations")
    public ResponseEntity<Map<String, Object>> enregistrerConversation(@RequestBody AiConversationRequestDto dto, Principal qui) {
        try {
            return ok(conversations.saveConversation(qui.getName(), dto));
        } catch (IllegalArgumentException e) {
            return invalide(e.getMessage());
        }
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Map<String, Object>> supprimerConversation(@PathVariable String id, Principal qui) {
        return ok(conversations.deleteConversation(qui.getName(), id));
    }

    @DeleteMapping("/conversations")
    public ResponseEntity<Map<String, Object>> viderConversations(Principal qui) {
        return ok(conversations.clearConversations(qui.getName()));
    }

    // ---------------------------------------------------------------- Graphiques épinglés

    @GetMapping("/epingles")
    public ResponseEntity<Map<String, Object>> listeEpingles(Principal qui) {
        return ok(epingles.getWidgets(qui.getName()));
    }

    @PostMapping("/epingles")
    public ResponseEntity<Map<String, Object>> epingler(@RequestBody PinnedWidgetRequestDto dto, Principal qui) {
        try {
            return ok(epingles.saveWidget(qui.getName(), dto));
        } catch (IllegalArgumentException e) {
            return invalide(e.getMessage());
        }
    }

    @DeleteMapping("/epingles/{id}")
    public ResponseEntity<Map<String, Object>> desepingler(@PathVariable String id, Principal qui) {
        return ok(epingles.deleteWidget(qui.getName(), id));
    }

    @PutMapping("/epingles/ordre")
    public ResponseEntity<Map<String, Object>> ordre(@RequestBody List<String> ids, Principal qui) {
        return ok(epingles.reorderWidgets(qui.getName(), ids));
    }

    // ---------------------------------------------------------------- Règles personnelles

    @GetMapping("/regles")
    public ResponseEntity<Map<String, Object>> listeRegles(Principal qui) {
        return ok(regles.getUserInstructions(qui.getName()));
    }

    @PostMapping("/regles")
    public ResponseEntity<Map<String, Object>> enregistrerRegle(@RequestBody UserInstructionRequestDto dto, Principal qui) {
        try {
            return ok(regles.saveUserInstruction(qui.getName(), dto));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return invalide(e.getMessage());
        }
    }

    @DeleteMapping("/regles/{id}")
    public ResponseEntity<Map<String, Object>> supprimerRegle(@PathVariable String id, Principal qui) {
        return ok(regles.deleteUserInstruction(qui.getName(), id));
    }

    // ---------------------------------------------------------------- Réponses

    private static ResponseEntity<Map<String, Object>> ok(Object contenu) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", true);
        m.put("content", contenu);
        return ResponseEntity.ok(m);
    }

    private static ResponseEntity<Map<String, Object>> invalide(String message) {
        return ResponseEntity.badRequest().body(Map.of("status", false, "content", Map.of("message", message)));
    }

    private static ResponseEntity<Map<String, Object>> nonTrouve(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", false, "content", Map.of("message", message)));
    }

    private void indisponible(HttpServletResponse res, Exception e) throws IOException {
        log.warn("IA Wren indisponible : {}", e.getClass().getSimpleName());
        erreur(res, HttpStatus.BAD_GATEWAY, "Le service de rapports IA ne répond pas. Réessayez dans un instant.");
    }

    private void erreur(HttpServletResponse res, HttpStatus statut, String message) throws IOException {
        res.setStatus(statut.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getOutputStream().write(json.writeValueAsString(Map.of("status", false, "content", Map.of("message", message)))
                .getBytes(StandardCharsets.UTF_8));
    }
}
