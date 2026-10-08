package com.sicmagroup.gpr.sla.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.sla.domain.BusinessDay;
import com.sicmagroup.gpr.sla.domain.Holiday;
import com.sicmagroup.gpr.sla.repository.BusinessDayRepository;
import com.sicmagroup.gpr.sla.repository.HolidayRepository;

/**
 * Calcul en temps ouvré : jours travaillés, horaires et jours fériés (calendrier configurable).
 * Par défaut : lundi à vendredi, 8 h à 17 h, aucun férié. Les dates sont des dates locales du serveur.
 */
@Service
public class BusinessTime {

    private static final long CACHE_MS = 5 * 60 * 1000;
    private static final int GUARD_DAYS = 4000;

    /** Plage de travail d'un jour. */
    private record Window(LocalTime start, LocalTime end) {
    }

    private final BusinessDayRepository dayRepository;
    private final HolidayRepository holidayRepository;

    private volatile Map<Integer, Window> windows = defaultWindows();
    private volatile Set<LocalDate> fixedHolidays = Set.of();
    private volatile Set<MonthDay> recurringHolidays = Set.of();
    private volatile long loadedAt = 0;

    @Autowired
    public BusinessTime(BusinessDayRepository dayRepository, HolidayRepository holidayRepository) {
        this.dayRepository = dayRepository;
        this.holidayRepository = holidayRepository;
    }

    /** Constructeur pour les tests : calendrier par défaut, sans base de données. */
    public BusinessTime() {
        this.dayRepository = null;
        this.holidayRepository = null;
        this.loadedAt = Long.MAX_VALUE;
    }

    /** Constructeur pour les tests : calendrier et fériés fournis. */
    public BusinessTime(Map<Integer, LocalTime[]> weekWindows, Set<LocalDate> holidays) {
        this();
        Map<Integer, Window> w = new HashMap<>();
        weekWindows.forEach((day, hours) -> w.put(day, new Window(hours[0], hours[1])));
        this.windows = w;
        this.fixedHolidays = holidays;
    }

    private static Map<Integer, Window> defaultWindows() {
        Map<Integer, Window> w = new HashMap<>();
        for (int d = 1; d <= 5; d++) {
            w.put(d, new Window(LocalTime.of(8, 0), LocalTime.of(17, 0)));
        }
        return w;
    }

    private void reloadIfNeeded() {
        if (dayRepository == null || System.currentTimeMillis() - loadedAt < CACHE_MS) {
            return;
        }
        Map<Integer, Window> w = new HashMap<>();
        var days = dayRepository.findAll();
        if (days.isEmpty()) {
            w = defaultWindows();
        } else {
            for (BusinessDay d : days) {
                if (d.isWorking() && d.getStartTime() != null && d.getEndTime() != null
                        && d.getEndTime().isAfter(d.getStartTime())) {
                    w.put(d.getDayOfWeek(), new Window(d.getStartTime(), d.getEndTime()));
                }
            }
        }
        Set<LocalDate> fixed = new HashSet<>();
        Set<MonthDay> recurring = new HashSet<>();
        for (Holiday h : holidayRepository.findAll()) {
            if (h.getDate() == null) {
                continue;
            }
            if (h.isRecurring()) {
                recurring.add(MonthDay.from(h.getDate()));
            } else {
                fixed.add(h.getDate());
            }
        }
        windows = w;
        fixedHolidays = fixed;
        recurringHolidays = recurring;
        loadedAt = System.currentTimeMillis();
    }

    /** Force la relecture du calendrier (après une modification dans l'écran de configuration). */
    public void invalidate() {
        loadedAt = 0;
    }

    private boolean isHoliday(LocalDate d) {
        return fixedHolidays.contains(d) || recurringHolidays.contains(MonthDay.from(d));
    }

    private Window windowFor(LocalDate d) {
        if (isHoliday(d)) {
            return null;
        }
        return windows.get(d.getDayOfWeek().getValue());
    }

    /** Durée d'un jour de travail en minutes (le plus long des jours ouvrés du calendrier). */
    public long workdayMinutes() {
        reloadIfNeeded();
        long max = 0;
        for (Window w : windows.values()) {
            max = Math.max(max, Duration.between(w.start, w.end).toMinutes());
        }
        return max > 0 ? max : 540;
    }

    public boolean isWorkingDay(LocalDate d) {
        reloadIfNeeded();
        return windowFor(d) != null;
    }

    /** Ajoute des minutes à une date : en temps ouvré si business, sinon en minutes calendaires. */
    public LocalDateTime add(boolean business, LocalDateTime from, long minutes) {
        if (from == null) {
            return null;
        }
        return business ? addBusiness(from, minutes) : from.plusMinutes(minutes);
    }

    /** Minutes entre deux dates (positif si to > from, négatif sinon). */
    public long between(boolean business, LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return 0;
        }
        if (to.isBefore(from)) {
            return -between(business, to, from);
        }
        return business ? businessBetween(from, to) : Duration.between(from, to).toMinutes();
    }

    public LocalDateTime addBusiness(LocalDateTime start, long minutes) {
        reloadIfNeeded();
        if (minutes <= 0) {
            return start;
        }
        LocalDateTime cur = start;
        long remaining = minutes;
        for (int guard = 0; guard < GUARD_DAYS; guard++) {
            Window w = windowFor(cur.toLocalDate());
            if (w != null) {
                LocalDateTime ws = cur.toLocalDate().atTime(w.start);
                LocalDateTime we = cur.toLocalDate().atTime(w.end);
                if (cur.isBefore(ws)) {
                    cur = ws;
                }
                if (cur.isBefore(we)) {
                    long available = Duration.between(cur, we).toMinutes();
                    if (remaining <= available) {
                        return cur.plusMinutes(remaining);
                    }
                    remaining -= available;
                }
            }
            cur = cur.toLocalDate().plusDays(1).atStartOfDay();
        }
        return cur;
    }

    private long businessBetween(LocalDateTime from, LocalDateTime to) {
        reloadIfNeeded();
        long seconds = 0;
        LocalDate day = from.toLocalDate();
        LocalDate last = to.toLocalDate();
        for (int guard = 0; !day.isAfter(last) && guard < GUARD_DAYS; guard++, day = day.plusDays(1)) {
            Window w = windowFor(day);
            if (w == null) {
                continue;
            }
            LocalDateTime start = day.atTime(w.start);
            LocalDateTime end = day.atTime(w.end);
            LocalDateTime a = from.isAfter(start) ? from : start;
            LocalDateTime b = to.isBefore(end) ? to : end;
            if (b.isAfter(a)) {
                seconds += Duration.between(a, b).getSeconds();
            }
        }
        return seconds / 60;
    }

    /** Jour de la semaine au format du calendrier (1 = lundi). */
    public static int dayNumber(DayOfWeek d) {
        return d.getValue();
    }
}
