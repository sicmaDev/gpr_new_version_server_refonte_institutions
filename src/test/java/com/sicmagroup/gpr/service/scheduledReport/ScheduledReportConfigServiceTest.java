package com.sicmagroup.gpr.service.scheduledReport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.sicmagroup.gpr.domain.enumeration.ReportFrequency;
import com.sicmagroup.gpr.domain.enumeration.ReportRecipientType;
import com.sicmagroup.gpr.domain.model.ScheduledReportConfig;
import com.sicmagroup.gpr.repository.ScheduledReportConfigRepository;

/** Planification des relances : quand une configuration est « due », et validation de ses réglages. */
class ScheduledReportConfigServiceTest {

    private final ScheduledReportConfigRepository repo = mock(ScheduledReportConfigRepository.class);
    private final ScheduledReportConfigService service = new ScheduledReportConfigService(repo);

    // 2026-10-19 est un lundi
    private static final LocalDateTime LUNDI_9H = LocalDateTime.of(2026, 10, 19, 9, 0);

    private ScheduledReportConfig config(ReportRecipientType type, ReportFrequency freq) {
        return ScheduledReportConfig.builder().id(1L).recipientType(type).enabled(true).frequency(freq)
                .sendTime(LocalTime.of(8, 0))
                .timezone(ZoneId.systemDefault().getId()) // même fuseau que la JVM de test : pas de décalage
                .createdAt(LUNDI_9H.minusDays(30)).updatedAt(LUNDI_9H.minusDays(30)).build();
    }

    private void given(ScheduledReportConfig... configs) {
        when(repo.findAll()).thenReturn(List.of(configs));
        for (ReportRecipientType t : ReportRecipientType.values()) {
            when(repo.findByRecipientType(t)).thenReturn(Optional.of(config(t, ReportFrequency.DAILY)));
        }
    }

    @Test
    void uneConfigurationQuotidienneEstDueApresSonHeureEtUneSeuleFoisParJour() {
        ScheduledReportConfig c = config(ReportRecipientType.AGENTS, ReportFrequency.DAILY);
        given(c);

        assertThat(service.getDueConfigurations(LUNDI_9H)).containsExactly(c);
        assertThat(service.getDueConfigurations(LUNDI_9H.withHour(7))).isEmpty(); // avant l'heure d'envoi

        c.setLastExecutionAt(LUNDI_9H.withHour(8).withMinute(1)); // déjà passée ce matin
        assertThat(service.getDueConfigurations(LUNDI_9H)).isEmpty();
        assertThat(service.getDueConfigurations(LUNDI_9H.plusDays(1))).containsExactly(c);
    }

    @Test
    void uneConfigurationDesactiveeN_EstJamaisDue() {
        ScheduledReportConfig c = config(ReportRecipientType.PILOTE, ReportFrequency.DAILY);
        c.setEnabled(false);
        given(c);

        assertThat(service.getDueConfigurations(LUNDI_9H)).isEmpty();
    }

    @Test
    void uneConfigurationHebdomadaireNEstDueQueLeBonJour() {
        ScheduledReportConfig c = config(ReportRecipientType.AGENTS, ReportFrequency.WEEKLY);
        c.setDayOfWeek(1); // lundi
        given(c);

        assertThat(service.getDueConfigurations(LUNDI_9H)).containsExactly(c);
        assertThat(service.getDueConfigurations(LUNDI_9H.plusDays(1))).isEmpty();
    }

    @Test
    void plusieursJoursParSemaine() {
        ScheduledReportConfig c = config(ReportRecipientType.AGENTS, ReportFrequency.MULTIPLE_WEEKLY);
        c.setDaysOfWeek("1, 3");
        given(c);

        assertThat(service.getDueConfigurations(LUNDI_9H)).containsExactly(c);
        assertThat(service.getDueConfigurations(LUNDI_9H.plusDays(1))).isEmpty();
        assertThat(service.getDueConfigurations(LUNDI_9H.plusDays(2))).containsExactly(c);
    }

    @Test
    void jourDuMoisEtTrimestre() {
        ScheduledReportConfig mensuelle = config(ReportRecipientType.AGENTS, ReportFrequency.MONTHLY_DATE);
        mensuelle.setDayOfMonth(19);
        ScheduledReportConfig trimestrielle = config(ReportRecipientType.PILOTE, ReportFrequency.QUARTERLY);
        trimestrielle.setMonthOfQuarter(1); // octobre est le 1er mois du 4e trimestre
        trimestrielle.setDayOfMonth(19);
        given(mensuelle, trimestrielle);

        assertThat(service.getDueConfigurations(LUNDI_9H)).containsExactlyInAnyOrder(mensuelle, trimestrielle);
        assertThat(service.getDueConfigurations(LUNDI_9H.plusMonths(1))).containsExactly(mensuelle);
    }

    @Test
    void modifierLaPlanificationRemetLeDernierPassageAZero() {
        ScheduledReportConfig c = config(ReportRecipientType.AGENTS, ReportFrequency.DAILY);
        c.setLastExecutionAt(LUNDI_9H);
        when(repo.findById(1L)).thenReturn(Optional.of(c));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScheduledReportConfig request = config(ReportRecipientType.AGENTS, ReportFrequency.DAILY);
        request.setSendTime(LocalTime.of(17, 0));
        ScheduledReportConfig saved = service.update(1L, request);

        assertThat(saved.getSendTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(saved.getLastExecutionAt()).isNull();
    }

    @Test
    void uneConfigurationIncompleteEstRefusee() {
        ScheduledReportConfig c = config(ReportRecipientType.AGENTS, ReportFrequency.DAILY);
        when(repo.findById(1L)).thenReturn(Optional.of(c));

        ScheduledReportConfig hebdoSansJour = config(ReportRecipientType.AGENTS, ReportFrequency.WEEKLY);
        assertThatThrownBy(() -> service.update(1L, hebdoSansJour)).isInstanceOf(IllegalArgumentException.class);

        ScheduledReportConfig multipleSansJour = config(ReportRecipientType.AGENTS, ReportFrequency.MULTIPLE_WEEKLY);
        assertThatThrownBy(() -> service.update(1L, multipleSansJour)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unFuseauHoraireInvalideEstRefuse() {
        assertThatThrownBy(() -> service.updateTimezone("Mars/Olympus")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.updateTimezone(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void seulLEnvoiReussiMetAJourLaDerniereRelance() {
        ScheduledReportConfig c = config(ReportRecipientType.AGENTS, ReportFrequency.DAILY);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markExecuted(c, LUNDI_9H, false);
        assertThat(c.getLastExecutionAt()).isEqualTo(LUNDI_9H);
        assertThat(c.getLastSentAt()).isNull();

        service.markExecuted(c, LUNDI_9H.plusDays(1), true);
        assertThat(c.getLastSentAt()).isEqualTo(LUNDI_9H.plusDays(1));
    }
}
