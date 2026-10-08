package com.sicmagroup.gpr.sla.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.sla.dto.SlaRows.StatsRow;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.sla.domain.ClaimSla;
import com.sicmagroup.gpr.sla.domain.SlaBreachReason;
import com.sicmagroup.gpr.sla.domain.SlaCycleKind;
import com.sicmagroup.gpr.sla.domain.SlaPhase;
import com.sicmagroup.gpr.sla.repository.ClaimSlaRepository;
import com.sicmagroup.gpr.sla.repository.SlaBreachReasonRepository;

import lombok.RequiredArgsConstructor;

/**
 * Indicateurs de respect des délais pour les rapports et le tableau de bord. Chacun voit les indicateurs de son
 * périmètre. Aucune donnée d'identité : uniquement des comptes, des taux, des durées et des libellés
 * d'objets, d'agences et d'agents.
 */
@Service
@RequiredArgsConstructor
public class SlaStatsService {

    private final ClaimSlaRepository repository;
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final SlaBreachReasonRepository reasonRepository;
    private final SlaPerimeter perimeter;
    private final BusinessTime businessTime;
    private final SlaConfig config;

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    /** Un compteur et les attributs de sa plainte, avec les durées déjà calculées. */
    private record Row(ClaimSla sla, StatsRow attrs, Double resolutionHours, Boolean resolutionOnTime) {
    }

    public Map<String, Object> stats(User user, ClaimType type, LocalDate from, LocalDate to) {
        LocalDateTime start = (from == null ? LocalDate.now().withDayOfMonth(1) : from).atStartOfDay();
        LocalDateTime end = (to == null ? LocalDate.now() : to).plusDays(1).atStartOfDay();
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("La date de fin est avant la date de début");
        }
        // un rapport porte sur une période raisonnable (mesuré : l'historique entier d'un gros volume est très lent)
        if (java.time.Duration.between(start, end).toDays() > 400) {
            throw new IllegalArgumentException("La période ne peut pas dépasser 400 jours");
        }
        List<ClaimType> types = type == null ? List.of(ClaimType.CLAIM, ClaimType.DENUNCIACION) : List.of(type);
        SlaPerimeter.Scope scope = perimeter.scopeOf(user);
        List<Row> rows = new ArrayList<>();
        for (StatsRow attrs : repository.statsRows(types, start, end, scope.all(), scope.spIds(), scope.uid(),
                scope.collectorUid())) {
            ClaimSla s = attrs.sla();
            Double hours = null;
            Boolean onTime = null;
            if (s.getResolvedAt() != null && s.getReceivedAt() != null) {
                hours = businessTime.between(true, s.getReceivedAt(), s.getResolvedAt()) / 60.0;
                onTime = s.getResolutionDueAt() != null && !s.getResolvedAt().isAfter(s.getResolutionDueAt());
            }
            rows.add(new Row(s, attrs, hours, onTime));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("periode", Map.of("du", start.toLocalDate().toString(), "au", end.minusDays(1).toLocalDate().toString()));
        out.put("global", global(rows, repository.countReopened(SlaCycleKind.SATISFACTION, start, end, scope.all(),
                scope.spIds(), scope.uid(), scope.collectorUid())));
        out.put("parRisque", group(rows, r -> r.attrs().risk() == null ? "Non défini" : r.attrs().risk().name()));
        out.put("parCategorie", group(rows, r -> nd(r.attrs().category())));
        out.put("parObjet", group(rows, r -> nd(r.attrs().objet())));
        out.put("parAgence", group(rows, r -> nd(r.attrs().agence())));
        out.put("parAgent", group(rows, r -> r.attrs().agent() == null ? "Non affectée" : r.attrs().agent()));
        out.put("parMois", groupSorted(rows, r -> r.sla().getReceivedAt().format(MONTH)));
        out.put("retardsParTranche", overdueBuckets(rows));
        out.put("remonteesParNiveau", escalations(rows));
        out.put("motifsDeRetard", reasons(rows));
        out.put("delaiEnregistrementParCanal", registrationByChannel(rows));
        out.put("cloture", closure(rows));
        return out;
    }

    private static String nd(String v) {
        return v == null ? "Non défini" : v;
    }

    // --- Indicateurs globaux -----------------------------------------------------------------------------------

    private Map<String, Object> global(List<Row> rows, long reopened) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("recues", rows.size());
        long resolved = rows.stream().filter(r -> r.sla().getResolvedAt() != null).count();
        long onTime = rows.stream().filter(r -> Boolean.TRUE.equals(r.resolutionOnTime())).count();
        m.put("resolues", resolved);
        m.put("tauxRespectResolution", rate(onTime, resolved));

        long taken = rows.stream().filter(r -> r.sla().getTakeoverAt() != null && r.sla().getTakeoverDueAt() != null).count();
        long takenOnTime = rows.stream().filter(r -> r.sla().getTakeoverAt() != null && r.sla().getTakeoverDueAt() != null
                && !r.sla().getTakeoverAt().isAfter(r.sla().getTakeoverDueAt())).count();
        m.put("tauxRespectPriseEnCharge", rate(takenOnTime, taken));

        List<Double> hours = rows.stream().map(Row::resolutionHours).filter(h -> h != null).sorted().toList();
        m.put("delaiMoyenHeuresOuvrees", hours.isEmpty() ? null : round(hours.stream().mapToDouble(d -> d).average().orElse(0)));
        m.put("delaiMedianHeuresOuvrees", hours.isEmpty() ? null : round(median(hours)));
        List<Double> days = rows.stream().filter(r -> r.sla().getResolvedAt() != null && r.sla().getReceivedAt() != null)
                .map(r -> java.time.Duration.between(r.sla().getReceivedAt(), r.sla().getResolvedAt()).toMinutes() / 1440.0)
                .sorted().toList();
        m.put("delaiMoyenJoursCalendaires", days.isEmpty() ? null : round(days.stream().mapToDouble(d -> d).average().orElse(0)));
        m.put("delaiMedianJoursCalendaires", days.isEmpty() ? null : round(median(days)));

        long open = rows.stream().filter(r -> r.sla().getPhase() == SlaPhase.OPEN).count();
        m.put("ouvertes", open);
        long reg = rows.stream().filter(r -> r.sla().getResolvedAt() != null && r.sla().getRegulatoryDueAt() != null
                && !r.sla().getResolvedAt().isAfter(r.sla().getRegulatoryDueAt())).count();
        long regBreached = rows.stream().filter(r -> r.sla().isRegulatoryBreached()).count();
        m.put("conformiteReglementaire", rate(reg, resolved + regBreached - rows.stream()
                .filter(r -> r.sla().getResolvedAt() != null && r.sla().isRegulatoryBreached()).count()));
        m.put("horsDelaiReglementaire", regBreached);

        long resolvedClaims = rows.stream().filter(r -> r.sla().getTargetType() == ClaimType.CLAIM
                && r.sla().getResolvedAt() != null).count();
        m.put("tauxReouverture", rate(reopened, resolvedClaims));
        long justified = rows.stream().filter(r -> r.sla().getBreachReasonId() != null).count();
        m.put("retardsJustifies", justified);
        return m;
    }

    // --- Ventilations --------------------------------------------------------------------------------------------

    private List<Map<String, Object>> group(List<Row> rows, Function<Row, String> key) {
        Map<String, List<Row>> groups = rows.stream().collect(Collectors.groupingBy(key));
        return groups.entrySet().stream().map(e -> groupLine(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong((Map<String, Object> g) -> (long) g.get("recues")).reversed()).toList();
    }

    private List<Map<String, Object>> groupSorted(List<Row> rows, Function<Row, String> key) {
        Map<String, List<Row>> groups = rows.stream().collect(Collectors.groupingBy(key, TreeMap::new, Collectors.toList()));
        return groups.entrySet().stream().map(e -> groupLine(e.getKey(), e.getValue())).toList();
    }

    private Map<String, Object> groupLine(String label, List<Row> rows) {
        Map<String, Object> m = new LinkedHashMap<>();
        long resolved = rows.stream().filter(r -> r.sla().getResolvedAt() != null).count();
        long onTime = rows.stream().filter(r -> Boolean.TRUE.equals(r.resolutionOnTime())).count();
        List<Double> hours = rows.stream().map(Row::resolutionHours).filter(h -> h != null).toList();
        m.put("libelle", label);
        m.put("recues", (long) rows.size());
        m.put("resolues", resolved);
        m.put("dansLesDelais", onTime);
        m.put("tauxRespect", rate(onTime, resolved));
        m.put("delaiMoyenHeuresOuvrees", hours.isEmpty() ? null : round(hours.stream().mapToDouble(d -> d).average().orElse(0)));
        m.put("enRetard", rows.stream().filter(r -> r.sla().getPhase() == SlaPhase.OPEN && isOverdue(r.sla())).count());
        return m;
    }

    private static boolean isOverdue(ClaimSla s) {
        LocalDateTime due = SlaEngine.currentDue(s);
        return due != null && LocalDateTime.now().isAfter(due);
    }

    private List<Map<String, Object>> overdueBuckets(List<Row> rows) {
        String[] labels = { "0 à 1 jour", "2 à 3 jours", "4 à 7 jours", "8 à 15 jours", "16 jours et plus" };
        long[] counts = new long[5];
        LocalDateTime now = LocalDateTime.now();
        for (Row r : rows) {
            ClaimSla s = r.sla();
            LocalDateTime due = SlaEngine.currentDue(s);
            if (s.getPhase() != SlaPhase.OPEN || due == null || !now.isAfter(due)) {
                continue;
            }
            long days = java.time.Duration.between(due, now).toDays();
            int i = days <= 1 ? 0 : days <= 3 ? 1 : days <= 7 ? 2 : days <= 15 ? 3 : 4;
            counts[i]++;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            out.add(Map.of("tranche", labels[i], "nombre", counts[i]));
        }
        return out;
    }

    private List<Map<String, Object>> escalations(List<Row> rows) {
        Map<String, Long> byLevel = new TreeMap<>();
        for (Row r : rows) {
            if (r.sla().getEscalationCount() > 0 && r.sla().getOwnerLevel() != null) {
                byLevel.merge(r.sla().getOwnerLevel().name(), 1L, Long::sum);
            }
        }
        return byLevel.entrySet().stream().map(e -> Map.<String, Object>of("niveau", e.getKey(), "nombre", e.getValue())).toList();
    }

    private List<Map<String, Object>> reasons(List<Row> rows) {
        Map<Long, String> labels = new HashMap<>();
        for (SlaBreachReason r : reasonRepository.findAll()) {
            labels.put(r.getId(), r.getLibelle());
        }
        Map<String, Long> counts = new TreeMap<>();
        for (Row r : rows) {
            if (r.sla().getBreachReasonId() != null) {
                counts.merge(labels.getOrDefault(r.sla().getBreachReasonId(), "Autre"), 1L, Long::sum);
            }
        }
        return counts.entrySet().stream().map(e -> Map.<String, Object>of("motif", e.getKey(), "nombre", e.getValue())).toList();
    }

    private List<Map<String, Object>> registrationByChannel(List<Row> rows) {
        Map<String, List<Double>> byChannel = new TreeMap<>();
        for (Row r : rows) {
            ClaimSla s = r.sla();
            if (s.getRegisteredAt() == null || s.getReceivedAt() == null) {
                continue;
            }
            String channel = nd(r.attrs().channel());
            double minutes = Math.max(0, java.time.Duration.between(s.getReceivedAt(), s.getRegisteredAt()).toMinutes());
            byChannel.computeIfAbsent(channel, k -> new ArrayList<>()).add(minutes);
        }
        return byChannel.entrySet().stream().map(e -> Map.<String, Object>of("canal", e.getKey(),
                "delaiMoyenHeures", round(e.getValue().stream().mapToDouble(d -> d).average().orElse(0) / 60.0),
                "nombre", e.getValue().size())).toList();
    }

    /** Taux de clôture des réclamations traitées : mesurées, injoignables, en attente de mesure. */
    private Map<String, Object> closure(List<Row> rows) {
        List<Row> treated = rows.stream().filter(r -> r.sla().getTargetType() == ClaimType.CLAIM
                && r.sla().getResolvedAt() != null).toList();
        long unreachable = treated.stream().filter(r -> r.sla().isClosedUnreachable()).count();
        long measured = treated.stream().filter(r -> r.sla().getPhase() == SlaPhase.DONE && !r.sla().isClosedUnreachable()).count();
        List<Row> waiting = treated.stream().filter(r -> r.sla().getPhase() == SlaPhase.OPEN).toList();
        LocalDateTime now = LocalDateTime.now();
        double avgAge = waiting.isEmpty() ? 0 : waiting.stream()
                .mapToDouble(r -> java.time.Duration.between(r.sla().getResolvedAt(), now).toMinutes() / 1440.0).average().orElse(0);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("traitees", treated.size());
        m.put("mesurees", measured);
        m.put("clientInjoignable", unreachable);
        m.put("enAttenteDeMesure", waiting.size());
        m.put("tauxCloture", rate(measured, treated.size()));
        m.put("ageMoyenEnAttenteJours", waiting.isEmpty() ? null : round(avgAge));
        return m;
    }

    // --- Aides -----------------------------------------------------------------------------------------------------

    private static Double rate(long part, long total) {
        return total <= 0 ? null : Math.round(part * 1000.0 / total) / 10.0;
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double median(List<Double> sorted) {
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }
}
