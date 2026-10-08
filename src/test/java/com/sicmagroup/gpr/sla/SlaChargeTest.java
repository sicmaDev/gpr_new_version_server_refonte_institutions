package com.sicmagroup.gpr.sla;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.sla.domain.ClaimSla;
import com.sicmagroup.gpr.sla.repository.ClaimSlaRepository;
import com.sicmagroup.gpr.sla.service.SlaAlertService;
import com.sicmagroup.gpr.sla.service.SlaQueryService;
import com.sicmagroup.gpr.sla.service.SlaScanner;
import com.sicmagroup.gpr.sla.service.SlaStatsService;

/**
 * Test de charge du SLA : mesure le temps des vraies requêtes sur une base de test de 50 000 plaintes
 * (gpr_sicma_charge, données fictives). Ce test n'est PAS lancé avec les autres : il faut le demander avec
 * -Dsla.charge=true. Il ne touche jamais à la base de développement ni à une base de production.
 *
 * Préparation : voir CHIFFREMENT.md n'est pas concerné ; la base est créée et remplie à part (script de charge).
 */
@EnabledIfSystemProperty(named = "sla.charge", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/gpr_sicma_charge?useSSL=false&serverTimezone=Africa/Porto-Novo&useUnicode=true&characterEncoding=UTF-8",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
class SlaChargeTest {

    @Autowired private SlaQueryService queryService;
    @Autowired private SlaStatsService statsService;
    @Autowired private SlaAlertService alertService;
    @Autowired private SlaScanner scanner;
    @Autowired private ClaimSlaRepository repository;
    @Autowired private UserRepository userRepository;

    /** Mesure : un passage de chauffe puis la médiane de 3 exécutions, en millisecondes. */
    private long time(String label, Callable<Object> action) throws Exception {
        // chaque mesure part de la mémoire vidée : on mesure le coût réel, pas la lecture en mémoire
        queryService.clearCache();
        action.call(); // chauffe
        long[] runs = new long[3];
        for (int i = 0; i < 3; i++) {
            queryService.clearCache();
            long t0 = System.nanoTime();
            action.call();
            runs[i] = (System.nanoTime() - t0) / 1_000_000;
        }
        java.util.Arrays.sort(runs);
        System.out.printf("CHARGE | %-62s | %6d ms%n", label, runs[1]);
        return runs[1];
    }

    @Test
    void mesureDesRequetesSurCinquanteMillePlaintes() throws Exception {
        User pilote = userRepository.findById(1L).orElseThrow();
        User ra = userRepository.findById(301L).orElseThrow();          // RA d'une agence
        User raDirection = userRepository.findById(401L).orElseThrow(); // RA d'une direction (agences rattachées)
        User agent = userRepository.findById(5L).orElseThrow();

        System.out.println("CHARGE | plaintes avec compteur : " + repository.count());
        long worst = 0;

        // --- Écrans : indicateurs, liste paginée, badge ---
        worst = Math.max(worst, time("résumé (Pilote : voit tout)", () -> queryService.summary(pilote, null)));
        worst = Math.max(worst, time("résumé (RA d'une direction)", () -> queryService.summary(raDirection, null)));
        worst = Math.max(worst, time("résumé (RA d'une agence)", () -> queryService.summary(ra, null)));
        worst = Math.max(worst, time("résumé (agent)", () -> queryService.summary(agent, null)));
        worst = Math.max(worst, time("badge du menu : en retard (Pilote)", () -> queryService.count(pilote, "DEPASSE", null)));
        worst = Math.max(worst, time("badge du menu : en retard (agent)", () -> queryService.count(agent, "DEPASSE", null)));
        worst = Math.max(worst, time("liste page 1 : en retard (Pilote, 20 lignes)",
                () -> queryService.items(pilote, "DEPASSE", null, null, 0, 20)));
        worst = Math.max(worst, time("liste page 100 : en retard (Pilote, 20 lignes)",
                () -> queryService.items(pilote, "DEPASSE", null, null, 100, 20)));
        worst = Math.max(worst, time("liste : tout (RA d'une direction, 20 lignes)",
                () -> queryService.items(raDirection, "ALL", null, null, 0, 20)));
        worst = Math.max(worst, time("liste : recherche par code (Pilote)",
                () -> queryService.items(pilote, "ALL", null, "rec-1234", 0, 20)));
        worst = Math.max(worst, time("liste : dénonciations à risque (Pilote)",
                () -> queryService.items(pilote, "A_RISQUE", ClaimType.DENUNCIACION, null, 0, 20)));

        // --- Alertes et indicateurs ---
        worst = Math.max(worst, time("page Alertes : toutes les réclamations en retard (Pilote)",
                () -> alertService.overdue(ClaimType.CLAIM)));
        worst = Math.max(worst, time("indicateurs du dernier mois (Pilote)",
                () -> statsService.stats(pilote, null, LocalDate.now().minusDays(30), LocalDate.now())));
        worst = Math.max(worst, time("indicateurs des 12 derniers mois (Pilote)",
                () -> statsService.stats(pilote, null, LocalDate.now().minusDays(365), LocalDate.now())));

        // --- Tâche planifiée ---
        worst = Math.max(worst, time("contrôle : compteurs à regarder (lot de 100)",
                () -> repository.findDue(LocalDateTime.now(), PageRequest.of(0, 100))));
        worst = Math.max(worst, time("rattrapage : plaintes sans compteur",
                () -> repository.findClaimIdsWithoutSla(List.of(ClaimType.CLAIM, ClaimType.DENUNCIACION), PageRequest.of(0, 200))));

        List<ClaimSla> due = repository.findDue(LocalDateTime.now(), PageRequest.of(0, 100));
        long t0 = System.nanoTime();
        int done = 0;
        for (ClaimSla s : due) {
            try {
                scanner.processOne(s.getId());
                done++;
            } catch (Exception e) {
                // un compteur en erreur ne compte pas dans la mesure
            }
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("CHARGE | contrôle : traitement de %d compteurs (rappels/alertes/remontées) | %6d ms (%d ms par compteur)%n",
                done, ms, done == 0 ? 0 : ms / done);

        Map<String, Object> summary = queryService.summary(pilote, null);
        System.out.println("CHARGE | résumé du Pilote : " + summary);
        assertThat(repository.count()).isGreaterThanOrEqualTo(50_000);
        assertThat(worst).as("la requête la plus lente (ms)").isLessThan(120_000);
    }
}
