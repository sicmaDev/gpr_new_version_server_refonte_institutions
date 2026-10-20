package com.sicmagroup.gpr.cron;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sicmagroup.gpr.domain.enumeration.ReportRecipientType;
import com.sicmagroup.gpr.domain.model.ScheduledReportConfig;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.service.MailService;
import com.sicmagroup.gpr.service.scheduledReport.ScheduledReportConfigService;
import com.sicmagroup.gpr.sla.service.SlaConfig;

/** Relance de retard planifiée : elle laisse la place au suivi des délais quand celui-ci est actif. */
class AlertRetardCronTest {

    private final MailService mail = mock(MailService.class);
    private final ScheduledReportConfigService schedule = mock(ScheduledReportConfigService.class);
    private final SlaConfig slaConfig = mock(SlaConfig.class);
    private final ClaimRepository claims = mock(ClaimRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AlertRetardCron cron = new AlertRetardCron(mail, schedule);

    AlertRetardCronTest() {
        ReflectionTestUtils.setField(cron, "slaConfig", slaConfig);
        ReflectionTestUtils.setField(cron, "claimRepository", claims);
        ReflectionTestUtils.setField(cron, "userRepository", users);
    }

    @Test
    void quandLeSuiviDesDelaisEstActifLaRelanceNeFaitRien() {
        when(slaConfig.enabled()).thenReturn(true);

        cron.sendRelanceMail();

        verifyNoInteractions(schedule);
        verifyNoInteractions(mail);
    }

    @Test
    void sansConfigurationDueAucunMailNePart() {
        when(slaConfig.enabled()).thenReturn(false);
        when(schedule.getDueConfigurations(any(LocalDateTime.class))).thenReturn(List.of());

        cron.sendRelanceMail();

        verifyNoInteractions(mail);
    }

    @Test
    void uneConfigurationDueSansDossierEnRetardEstMarqueeSansEnvoi() {
        when(slaConfig.enabled()).thenReturn(false);
        ScheduledReportConfig due = ScheduledReportConfig.builder().recipientType(ReportRecipientType.AGENTS).build();
        when(schedule.getDueConfigurations(any(LocalDateTime.class))).thenReturn(List.of(due));
        when(claims.findByTypeAndIsDeletedFalseAndStatusIn(any(), any())).thenReturn(List.of());

        cron.sendRelanceMail();

        verify(schedule).markExecuted(any(ScheduledReportConfig.class), any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq(false));
        verify(schedule, never()).markExecuted(any(), any(), org.mockito.ArgumentMatchers.eq(true));
    }
}
