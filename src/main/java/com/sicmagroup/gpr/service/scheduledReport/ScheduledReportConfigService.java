package com.sicmagroup.gpr.service.scheduledReport;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.enumeration.ReportFrequency;
import com.sicmagroup.gpr.domain.enumeration.ReportRecipientType;
import com.sicmagroup.gpr.domain.model.ScheduledReportConfig;
import com.sicmagroup.gpr.repository.ScheduledReportConfigRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduledReportConfigService {

    // Fuseau horaire par défaut appliqué tant que l'admin n'a rien choisi dans l'écran de
    // planification : celui de l'institution (GPR_TIMEZONE, Porto-Novo par défaut, voir GprApplication).
    private static final String DEFAULT_TIMEZONE = com.sicmagroup.gpr.GprApplication.resolveTimezone();

    private final ScheduledReportConfigRepository repository;

    public List<ScheduledReportConfig> getAll() {
        ensureDefaults();
        LocalDateTime now = LocalDateTime.now();
        return repository.findAll().stream()
                .peek(config -> config.setNextExecutionAt(nextExecutionAt(config, now)))
                .toList();
    }

    /**
     * Fuseau horaire unique choisi par l'admin pour toute la planification (page "Fréquences
     * alertes"). Applique la même valeur aux deux configurations (AGENTS, PILOTE) pour qu'elles
     * restent toujours synchronisées, puis recalcule les prochaines exécutions avec ce nouveau
     * fuseau.
     */
    public List<ScheduledReportConfig> updateTimezone(String timezone) {
        validateTimezone(timezone);
        ensureDefaults();
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledReportConfig> all = repository.findAll();
        all.forEach(config -> {
            config.setTimezone(timezone);
            config.setUpdatedAt(now);
        });
        repository.saveAll(all);
        return getAll();
    }

    public ScheduledReportConfig update(Long id, ScheduledReportConfig request) {
        ScheduledReportConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuration introuvable"));
        validate(request);
        config.setEnabled(request.isEnabled());
        config.setFrequency(request.getFrequency());
        config.setDayOfWeek(request.getDayOfWeek());
        config.setDaysOfWeek(request.getDaysOfWeek());
        config.setDayOfMonth(request.getDayOfMonth());
        config.setWeekOfMonth(request.getWeekOfMonth());
        config.setMonthOfQuarter(request.getMonthOfQuarter());
        config.setSendTime(request.getSendTime());
        config.setUpdatedAt(LocalDateTime.now());
        // isDue() se base sur "lastExecutionAt a-t-il déjà la date d'aujourd'hui" pour ne
        // déclencher qu'une fois par jour. Sans cette remise à zéro, changer la planification
        // (ex: passer l'heure d'envoi à plus tard aujourd'hui) reste bloqué jusqu'au lendemain
        // par un lastExecutionAt datant d'avant la modification, sous l'ancienne config.
        config.setLastExecutionAt(null);
        ScheduledReportConfig saved = repository.save(config);
        saved.setNextExecutionAt(nextExecutionAt(saved, LocalDateTime.now()));
        return saved;
    }

    public List<ScheduledReportConfig> getDueConfigurations(LocalDateTime now) {
        ensureDefaults();
        return repository.findAll().stream()
                .filter(ScheduledReportConfig::isEnabled)
                .filter(config -> isDue(config, now))
                .toList();
    }

    public ReportRecipientType getRecipientType(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuration introuvable"))
                .getRecipientType();
    }

    /**
     * @param sent true si un mail a réellement été envoyé avec succès lors de ce passage du
     *             cron (met alors aussi à jour lastSentAt, affiché comme "Dernière relance").
     *             false si le cron s'est simplement exécuté sans rien avoir à envoyer, ou si
     *             l'envoi a échoué : lastExecutionAt avance quand même (le créneau du jour est
     *             considéré traité, pour ne pas retenter en boucle), mais lastSentAt - et donc
     *             ce qui est affiché à l'utilisateur - reste inchangé.
     */
    public void markExecuted(ScheduledReportConfig config, LocalDateTime executedAt, boolean sent) {
        config.setLastExecutionAt(executedAt);
        config.setUpdatedAt(executedAt);
        if (sent) {
            config.setLastSentAt(executedAt);
        }
        repository.save(config);
    }

    private boolean isDue(ScheduledReportConfig config, LocalDateTime now) {
        LocalDateTime nowInZone = toConfigZone(now, config);
        LocalTime currentTime = nowInZone.toLocalTime();
        if (currentTime.isBefore(config.getSendTime())) {
            return false;
        }
        if (!matchesCalendar(config, nowInZone)) {
            return false;
        }
        if (config.getLastExecutionAt() == null) {
            return true;
        }
        LocalDateTime lastExecutionInZone = toConfigZone(config.getLastExecutionAt(), config);
        return !lastExecutionInZone.toLocalDate().isEqual(nowInZone.toLocalDate());
    }

    private void ensureDefaults() {
        for (ReportRecipientType type : ReportRecipientType.values()) {
            if (repository.findByRecipientType(type).isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                repository.save(ScheduledReportConfig.builder()
                        .recipientType(type)
                        .enabled(true)
                        .frequency(ReportFrequency.DAILY)
                        .dayOfWeek(DayOfWeek.MONDAY.getValue())
                        .sendTime(LocalTime.of(8, 0))
                        .timezone(DEFAULT_TIMEZONE)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
            }
        }
    }

    private void validateTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new IllegalArgumentException("Le fuseau horaire est obligatoire");
        }
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Fuseau horaire invalide : " + timezone);
        }
    }

    /**
     * Résout le fuseau d'une configuration, en retombant sur le fuseau par défaut si aucun
     * n'a encore été choisi (configs créées avant l'ajout de ce champ) ou si la valeur stockée
     * n'est plus un identifiant IANA valide.
     */
    private ZoneId resolveZone(ScheduledReportConfig config) {
        if (config.getTimezone() != null && !config.getTimezone().isBlank()) {
            try {
                return ZoneId.of(config.getTimezone());
            } catch (DateTimeException e) {
                // valeur corrompue : on retombe sur le défaut plutôt que de faire planter le cron
            }
        }
        return ZoneId.of(DEFAULT_TIMEZONE);
    }

    /**
     * Convertit un instant stocké dans le fuseau par défaut de la JVM (voir
     * GprApplication) vers l'heure locale équivalente dans le fuseau choisi pour
     * cette configuration, pour que sendTime/dayOfWeek/etc. soient comparés dans le bon fuseau.
     */
    private LocalDateTime toConfigZone(LocalDateTime value, ScheduledReportConfig config) {
        return value.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(resolveZone(config))
                .toLocalDateTime();
    }

    private void validate(ScheduledReportConfig request) {
        if (request.getFrequency() == null || request.getSendTime() == null) {
            throw new IllegalArgumentException("La fréquence et l'heure sont obligatoires");
        }
        if (request.getFrequency() == ReportFrequency.WEEKLY
                && !validDay(request.getDayOfWeek())) {
            throw new IllegalArgumentException("Le jour de la semaine est obligatoire");
        }
        if (request.getFrequency() == ReportFrequency.MULTIPLE_WEEKLY
                && (request.getDaysOfWeek() == null || request.getDaysOfWeek().isBlank())) {
            throw new IllegalArgumentException("Sélectionnez au moins un jour de la semaine");
        }
        if (request.getFrequency() == ReportFrequency.MONTHLY_DATE
                && !validRange(request.getDayOfMonth(), 1, 31)) {
            throw new IllegalArgumentException("Le jour du mois doit être compris entre 1 et 31");
        }
        if (request.getFrequency() == ReportFrequency.MONTHLY_WEEK
                && !validRange(request.getWeekOfMonth(), 1, 5)
                && request.getWeekOfMonth() != -1) {
            throw new IllegalArgumentException("La semaine du mois est invalide");
        }
        if (request.getFrequency() == ReportFrequency.MONTHLY_WEEK
                && !validDay(request.getDayOfWeek())) {
            throw new IllegalArgumentException("Le jour de la semaine est obligatoire");
        }
        if (request.getFrequency() == ReportFrequency.QUARTERLY
                && !validRange(request.getMonthOfQuarter(), 1, 3)) {
            throw new IllegalArgumentException("Le mois du trimestre doit être compris entre 1 et 3");
        }
        if (request.getFrequency() == ReportFrequency.QUARTERLY
                && !validRange(request.getDayOfMonth(), 1, 31)) {
            throw new IllegalArgumentException("Le jour du mois doit être compris entre 1 et 31");
        }
    }

    private boolean matchesCalendar(ScheduledReportConfig config, LocalDateTime now) {
        int day = now.getDayOfWeek().getValue();
        return switch (config.getFrequency()) {
            case DAILY -> true;
            case WEEKLY -> validDay(config.getDayOfWeek()) && config.getDayOfWeek() == day;
            case MULTIPLE_WEEKLY -> Arrays.stream(config.getDaysOfWeek().split(","))
                    .map(String::trim).anyMatch(value -> value.equals(String.valueOf(day)));
            case MONTHLY_DATE -> config.getDayOfMonth() != null
                    && config.getDayOfMonth() == now.getDayOfMonth();
            case MONTHLY_WEEK -> isWeekOfMonth(now, config.getWeekOfMonth())
                    && validDay(config.getDayOfWeek()) && config.getDayOfWeek() == day;
            case QUARTERLY -> config.getMonthOfQuarter() != null
                    && ((now.getMonthValue() - 1) % 3) + 1 == config.getMonthOfQuarter()
                    && config.getDayOfMonth() != null && config.getDayOfMonth() == now.getDayOfMonth();
        };
    }

    private LocalDateTime nextExecutionAt(ScheduledReportConfig config, LocalDateTime now) {
        if (!config.isEnabled() || config.getSendTime() == null || config.getFrequency() == null) {
            return null;
        }
        ZoneId zone = resolveZone(config);
        LocalDateTime nowInZone = toConfigZone(now, config);
        LocalDate candidateDate = nowInZone.toLocalDate();
        for (int offset = 0; offset <= 370; offset++) {
            LocalDateTime candidateInZone = candidateDate.plusDays(offset).atTime(config.getSendTime());
            if (candidateInZone.isAfter(nowInZone) && matchesCalendar(config, candidateInZone)) {
                // reconverti dans le fuseau par défaut de la JVM pour rester cohérent avec
                // createdAt/updatedAt/lastExecutionAt, tous stockés dans ce même référentiel.
                return candidateInZone.atZone(zone)
                        .withZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDateTime();
            }
        }
        return null;
    }

    private boolean isWeekOfMonth(LocalDateTime now, Integer weekOfMonth) {
        if (weekOfMonth == null)
            return false;
        if (weekOfMonth == -1) {
            return now.toLocalDate().plusWeeks(1).getMonthValue() != now.getMonthValue();
        }
        return (now.getDayOfMonth() - 1) / 7 + 1 == weekOfMonth;
    }

    private boolean validDay(Integer value) {
        return validRange(value, 1, 7);
    }

    private boolean validRange(Integer value, int min, int max) {
        return value != null && value >= min && value <= max;
    }
}
