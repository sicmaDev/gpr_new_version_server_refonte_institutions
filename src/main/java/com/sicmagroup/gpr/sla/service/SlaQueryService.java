package com.sicmagroup.gpr.sla.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.enumeration.ClaimEventType;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ClaimEvent;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ClaimEventRepository;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.sla.domain.ClaimSla;
import com.sicmagroup.gpr.sla.dto.SlaInfo;
import com.sicmagroup.gpr.sla.dto.SlaItem;
import com.sicmagroup.gpr.sla.dto.SlaRows.ItemRow;
import com.sicmagroup.gpr.sla.repository.ClaimSlaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Lecture du SLA pour les écrans : liste paginée, indicateurs, compteur du menu, détail d'une plainte.
 * Le résumé est lu en UNE requête puis gardé en mémoire ({@link SlaSummaryCache}) ; il sert aussi de total aux
 * listes, qui ne font donc plus de second comptage.
 */
@Service
@RequiredArgsConstructor
public class SlaQueryService {

    private static final int SEARCH_MAX_IDS = 500;

    private final ClaimSlaRepository repository;
    private final ClaimRepository claimRepository;
    private final SlaPerimeter perimeter;
    private final SlaInfoService infoService;
    private final SlaConfig config;
    private final ClaimEventRepository claimEventRepository;

    private static String pattern(String q) {
        return "%" + q.trim().toLowerCase() + "%";
    }

    private static String normalizeState(String state) {
        return state == null || state.isBlank() ? "ALL" : state.trim().toUpperCase();
    }

    private static int clampSize(int size) {
        return size <= 0 ? 20 : Math.min(size, 200);
    }

    private static long n(Object o) {
        return o == null ? 0 : ((Number) o).longValue();
    }

    /** Oublie les résumés en mémoire (appelé à chaque changement d'une plainte, et par les tests). */
    public void clearCache() {
        SlaSummaryCache.clear();
    }

    // ---------------------------------------------------------------------------------------------
    // Liste paginée
    // ---------------------------------------------------------------------------------------------

    /** Liste paginée selon le périmètre de l'utilisateur. */
    public Map<String, Object> items(User user, String state, ClaimType type, String q, int page, int size) {
        SlaPerimeter.Scope scope = perimeter.scopeOf(user);
        String st = normalizeState(state);
        LocalDateTime now = LocalDateTime.now();
        int pageSize = clampSize(size);
        Pageable pageable = PageRequest.of(Math.max(0, page), pageSize);

        List<ClaimSla> content;
        long total;
        if (q == null || q.isBlank()) {
            // pas de comptage séparé : le total vient du résumé (même définition des états)
            content = repository.findInScope(st, type, now, scope.all(), scope.spIds(), scope.uid(),
                    scope.collectorUid(), pageable).getContent();
            total = totalFor(summary(user, type), st);
        } else {
            // recherche par code : on trouve d'abord les plaintes concernées, puis on applique périmètre et état
            List<Long> ids = repository.claimIdsMatching(pattern(q), PageRequest.of(0, SEARCH_MAX_IDS));
            if (ids.isEmpty()) {
                content = List.of();
                total = 0;
            } else {
                var result = repository.findInScopeByIds(st, type, ids, now, scope.all(), scope.spIds(), scope.uid(),
                        scope.collectorUid(), pageable);
                content = result.getContent();
                total = result.getTotalElements();
            }
        }

        Map<Long, ItemRow> rows = new HashMap<>();
        if (!content.isEmpty()) {
            for (ItemRow r : repository.itemRows(content.stream().map(ClaimSla::getClaimId).collect(Collectors.toSet()))) {
                rows.put(r.claimId(), r);
            }
        }
        Map<Long, String> names = infoService.ownerNames(content);
        List<SlaItem> items = new ArrayList<>();
        for (ClaimSla s : content) {
            ItemRow r = rows.get(s.getClaimId());
            if (r == null || r.type() != s.getTargetType()) {
                continue; // suggestion ou plainte introuvable
            }
            items.add(SlaItem.builder()
                    .claimId(r.claimId())
                    .type(r.type())
                    .code(r.code())
                    .codeClient(r.codeClient())
                    .collectorId(s.getCollectorId())
                    .status(r.status())
                    .objetLibelle(r.objet())
                    .gravity(r.risk())
                    .servicePointLibelle(r.agence())
                    .receiptDateTime(r.receiptDateTime())
                    .sla(infoService.toInfo(s, names))
                    .build());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("page", Math.max(0, page));
        out.put("size", pageSize);
        out.put("total", total);
        out.put("totalPages", (int) Math.ceil(total / (double) pageSize));
        return out;
    }

    private static long totalFor(Map<String, Object> summary, String state) {
        String key = switch (state) {
            case "ALL" -> "TOTAL";
            case "OPEN" -> "OUVERTES";
            default -> state;
        };
        return n(summary.get(key));
    }

    // ---------------------------------------------------------------------------------------------
    // Résumé (une seule lecture, gardée en mémoire)
    // ---------------------------------------------------------------------------------------------

    /** Indicateurs du périmètre : nombre de plaintes par état et taux de respect. */
    public Map<String, Object> summary(User user, ClaimType type) {
        SlaPerimeter.Scope scope = perimeter.scopeOf(user);
        String key = scope.all() + "|" + scope.spIds() + "|" + scope.uid() + "|" + scope.collectorUid() + "|" + type;
        Map<String, Object> cached = SlaSummaryCache.get(key);
        if (cached != null) {
            return cached;
        }
        Map<String, Object> fresh = computeSummary(scope, type);
        SlaSummaryCache.put(key, fresh);
        return fresh;
    }

    private Map<String, Object> computeSummary(SlaPerimeter.Scope scope, ClaimType type) {
        LocalDateTime now = LocalDateTime.now();
        List<Object[]> result = repository.summaryCounts(type, now, now.plusDays(config.regulatoryWarningDays()),
                scope.all(), scope.spIds(), scope.uid(), scope.collectorUid());
        Object[] c = result.isEmpty() ? new Object[10] : result.get(0);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("EN_COURS", n(c[2]));
        out.put("A_RISQUE", n(c[3]));
        out.put("DEPASSE", n(c[4]));
        out.put("SUSPENDU", n(c[5]));
        out.put("RESPECTE", n(c[6]));
        out.put("HORS_DELAI", n(c[7]));
        out.put("OUVERTES", n(c[1]));
        out.put("TOTAL", n(c[0]));
        long respected = n(c[6]);
        long denominator = respected + n(c[4]) + n(c[7]);
        out.put("tauxRespect", denominator == 0 ? null : Math.round(respected * 1000.0 / denominator) / 10.0);
        out.put("PROCHES_REGLEMENTAIRE", n(c[9]));
        out.put("TRAITEES_NON_MESUREES", type == ClaimType.DENUNCIACION ? 0L : n(c[8]));
        return out;
    }

    /** Nombre de plaintes dans un état (badge du menu) : lu dans le résumé en mémoire. */
    public long count(User user, String state, ClaimType type) {
        return totalFor(summary(user, type), normalizeState(state));
    }

    /** Compteur SLA d'une plainte, si l'utilisateur la voit. Renvoie null sinon. */
    public SlaInfo forClaim(User user, ClaimType type, Long claimId, SlaEngine engine) {
        Claim claim = claimRepository.findById(claimId).orElse(null);
        if (claim == null || claim.getType() != type) {
            return null;
        }
        if (!perimeter.canSee(perimeter.scopeOf(user), claim)) {
            return null;
        }
        SlaInfo info = engine.current(type, claimId).map(s -> infoService.toInfo(s, infoService.ownerNames(List.of(s))))
                .orElse(null);
        if (info != null && info.isAutoEscalated()) {
            // « Remonté automatiquement depuis X » : lu dans la dernière remontée de l'historique de la plainte
            List<ClaimEvent> events = claimEventRepository.findByClaimIdOrderByCreatedAtAsc(claimId);
            for (int i = events.size() - 1; i >= 0; i--) {
                ClaimEvent e = events.get(i);
                if (e.getEventType() == ClaimEventType.AUTO_TRANSMITTED && e.getMetadata() != null) {
                    info.setEscalatedFrom(e.getMetadata().split("\\|", -1)[0]);
                    info.setEscalatedAt(e.getCreatedAt());
                    break;
                }
            }
        }
        return info;
    }
}
