package com.sicmagroup.gpr.sla;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sicmagroup.gpr.sla.service.BusinessTime;

/**
 * Calcul en temps ouvré (lundi à vendredi, 8 h - 17 h, jours fériés). Octobre 2026 : le 5 est un lundi.
 * Test unitaire : aucune base de données.
 */
class BusinessTimeTest {

    private final BusinessTime defaults = new BusinessTime();

    private static LocalDateTime t(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute);
    }

    private static BusinessTime withHolidays(LocalDate... holidays) {
        Map<Integer, LocalTime[]> week = new HashMap<>();
        for (int d = 1; d <= 5; d++) {
            week.put(d, new LocalTime[] { LocalTime.of(8, 0), LocalTime.of(17, 0) });
        }
        return new BusinessTime(week, Set.of(holidays));
    }

    @Test
    void troisJoursOuvresDepuisLundi9hDonnentJeudi9h() {
        // 3 jours ouvrés = 3 x 9 h = 1620 minutes
        assertThat(defaults.add(true, t(2026, 10, 5, 9, 0), 1620)).isEqualTo(t(2026, 10, 8, 9, 0));
    }

    @Test
    void vendredi16h30PlusQuatreHeuresDonneLundi11h30() {
        assertThat(defaults.add(true, t(2026, 10, 9, 16, 30), 240)).isEqualTo(t(2026, 10, 12, 11, 30));
    }

    @Test
    void lundiFerieReporteLEcheanceAuMardi() {
        BusinessTime bt = withHolidays(LocalDate.of(2026, 10, 12));
        assertThat(bt.add(true, t(2026, 10, 9, 16, 30), 240)).isEqualTo(t(2026, 10, 13, 11, 30));
    }

    @Test
    void depuisLeSamediLeChronometreDemarreLeLundi() {
        assertThat(defaults.add(true, t(2026, 10, 10, 10, 0), 60)).isEqualTo(t(2026, 10, 12, 9, 0));
    }

    @Test
    void delaiDeQuatreHeuresAChevalSurDeuxJours() {
        // mardi 15 h : 2 h restent, puis 2 h le mercredi matin
        assertThat(defaults.add(true, t(2026, 10, 6, 15, 0), 240)).isEqualTo(t(2026, 10, 7, 10, 0));
    }

    @Test
    void avantLOuvertureLeChronometreDemarreAHuitHeures() {
        assertThat(defaults.add(true, t(2026, 10, 6, 6, 0), 60)).isEqualTo(t(2026, 10, 6, 9, 0));
    }

    @Test
    void changementDAnneeAvecLeJourDeLAnFerie() {
        // jeudi 31/12/2026 16 h + 2 h : 1 h le jeudi, 1er janvier férié, week-end, puis lundi 4 janvier 9 h
        BusinessTime bt = withHolidays(LocalDate.of(2027, 1, 1));
        assertThat(bt.add(true, t(2026, 12, 31, 16, 0), 120)).isEqualTo(t(2027, 1, 4, 9, 0));
    }

    @Test
    void lesMinutesCalendairesIgnorentLeCalendrier() {
        assertThat(defaults.add(false, t(2026, 10, 9, 16, 30), 240)).isEqualTo(t(2026, 10, 9, 20, 30));
    }

    @Test
    void minutesOuvreesEntreDeuxDates() {
        assertThat(defaults.between(true, t(2026, 10, 5, 9, 0), t(2026, 10, 8, 9, 0))).isEqualTo(1620);
        assertThat(defaults.between(true, t(2026, 10, 6, 10, 0), t(2026, 10, 6, 11, 30))).isEqualTo(90);
        // vendredi 16 h -> lundi 9 h : 1 h le vendredi + 1 h le lundi
        assertThat(defaults.between(true, t(2026, 10, 9, 16, 0), t(2026, 10, 12, 9, 0))).isEqualTo(120);
    }

    @Test
    void ecartNegatifQuandLaDateEstAvant() {
        assertThat(defaults.between(true, t(2026, 10, 8, 9, 0), t(2026, 10, 5, 9, 0))).isEqualTo(-1620);
    }

    @Test
    void unJourDeTravailFaitNeufHeures() {
        assertThat(defaults.workdayMinutes()).isEqualTo(540);
    }

    @Test
    void ajouterZeroMinuteNeBougePas() {
        assertThat(defaults.add(true, t(2026, 10, 10, 10, 0), 0)).isEqualTo(t(2026, 10, 10, 10, 0));
    }
}
