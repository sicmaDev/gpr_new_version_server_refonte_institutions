package com.sicmagroup.gpr.service.smsLog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sicmagroup.gpr.domain.enumeration.MessageChannel;
import com.sicmagroup.gpr.domain.enumeration.SmsStatus;
import com.sicmagroup.gpr.domain.enumeration.SmsType;
import com.sicmagroup.gpr.domain.model.Log;
import com.sicmagroup.gpr.domain.model.SmsLog;
import com.sicmagroup.gpr.repository.LogRepository;
import com.sicmagroup.gpr.repository.SmsLogRepository;

/** Journal des envois : un seul point d'enregistrement pour les SMS, les mails et les messages WhatsApp. */
class OutgoingMessageLoggerTest {

    private final SmsLogRepository smsRepo = mock(SmsLogRepository.class);
    private final LogRepository logRepo = mock(LogRepository.class);

    private OutgoingMessageLogger logger() {
        return new OutgoingMessageLogger(smsRepo, logRepo);
    }

    @Test
    void unEnvoiReussiEstEnregistreAvecSonCanalSonTypeEtSaPlainte() {
        logger();

        OutgoingMessageLogger.record(MessageChannel.WHATSAPP, SmsType.EXTERNE, "22952030745", "Bonjour", true, 12L, null);

        ArgumentCaptor<SmsLog> saved = ArgumentCaptor.forClass(SmsLog.class);
        verify(smsRepo).save(saved.capture());
        assertThat(saved.getValue().getChannel()).isEqualTo(MessageChannel.WHATSAPP);
        assertThat(saved.getValue().getType()).isEqualTo(SmsType.EXTERNE);
        assertThat(saved.getValue().getStatus()).isEqualTo(SmsStatus.ENVOYE);
        assertThat(saved.getValue().getClaimId()).isEqualTo(12L);
        assertThat(saved.getValue().getSendAt()).isNotNull();
    }

    @Test
    void unEchecEstEnregistreSansDateDEnvoiEtTraceDansLeLogSysteme() {
        logger();

        OutgoingMessageLogger.record(MessageChannel.SMS, SmsType.INTERNE, "0152030745", "Alerte", false, null, null);

        ArgumentCaptor<SmsLog> saved = ArgumentCaptor.forClass(SmsLog.class);
        verify(smsRepo).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(SmsStatus.ECHEC);
        assertThat(saved.getValue().getSendAt()).isNull();
        verify(logRepo).save(any(Log.class));
    }

    @Test
    void lEchecDeLaJournalisationNeFaitJamaisEchouerUnEnvoi() {
        when(smsRepo.save(any())).thenThrow(new IllegalStateException("base indisponible"));
        logger();

        OutgoingMessageLogger.record(MessageChannel.MAIL, SmsType.INTERNE, "agent@exemple.test", "Objet : test", true, null, null);

        // aucune exception : l'envoi réel n'est pas affecté
    }

    @Test
    void unDestinataireVideEstRemplaceEtUnTexteVideAussi() {
        logger();

        OutgoingMessageLogger.record(MessageChannel.SMS, SmsType.EXTERNE, " ", "", true, null, null);

        ArgumentCaptor<SmsLog> saved = ArgumentCaptor.forClass(SmsLog.class);
        verify(smsRepo).save(saved.capture());
        assertThat(saved.getValue().getNumeroDestinataire()).isEqualTo("(inconnu)");
        assertThat(saved.getValue().getContent()).isEqualTo("(vide)");
    }

    @Test
    void unMailHtmlEstArchiveEnTexteLisible() {
        assertThat(OutgoingMessageLogger.htmlToText("<p>Bonjour&nbsp;Awa</p><br/><b>Merci</b> &amp; à bientôt"))
                .isEqualTo("Bonjour Awa\nMerci & à bientôt");
    }

    @Test
    void leNumeroWhatsappEstLisible() {
        assertThat(OutgoingMessageLogger.phoneFromJid("22952030745@c.us")).isEqualTo("22952030745");
        assertThat(OutgoingMessageLogger.phoneFromJid("0152030745")).isEqualTo("0152030745");
        assertThat(OutgoingMessageLogger.phoneFromJid(null)).isNull();
    }
}
