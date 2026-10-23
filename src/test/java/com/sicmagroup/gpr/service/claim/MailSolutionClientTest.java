package com.sicmagroup.gpr.service.claim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.sicmagroup.gpr.domain.enumeration.ClaimEventType;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.MailService;
import com.sicmagroup.gpr.service.claimEvent.ClaimEventServiceImpl;

/** Solution envoyée automatiquement au client par mail : seulement avec une adresse, jamais le commentaire interne. */
class MailSolutionClientTest {

    private final MailService mailService = mock(MailService.class);
    private final ClaimEventServiceImpl events = mock(ClaimEventServiceImpl.class);
    private final ClaimServiceImpl service = mock(ClaimServiceImpl.class, CALLS_REAL_METHODS);

    MailSolutionClientTest() {
        ReflectionTestUtils.setField(service, "mailService", mailService);
        ReflectionTestUtils.setField(service, "claimEventServiceImpl", events);
    }

    private Claim claim(String email, ClaimType type) {
        Claim c = new Claim();
        c.setId(42L);
        c.setCodeClient("REC-ab12");
        c.setType(type);
        c.setEmail(email);
        c.setClientFirstAndLastName("Awa Test");
        return c;
    }

    private Solution solution() {
        Solution s = new Solution();
        s.setContent("Votre compte a été rétabli.");
        s.setCommentaire("NOTE INTERNE : client difficile");
        return s;
    }

    private User treator() {
        User u = new User();
        u.setFirstandlastname("Agent Test");
        u.setEmail("agent@exemple.test");
        return u;
    }

    @Test
    void sansAdresseAucunMailNePart() {
        ReflectionTestUtils.invokeMethod(service, "sendSolutionMailToClient", claim(" ", ClaimType.CLAIM), solution(), treator());
        ReflectionTestUtils.invokeMethod(service, "sendSolutionMailToClient", claim(null, ClaimType.CLAIM), solution(), treator());

        verify(mailService, never()).sendClientMail(anyString(), anyString(), anyString(), anyLong(), any());
    }

    @Test
    void laSolutionPartSeuleSansLeCommentaireInterneEtLEnvoiEstTraceDansLHistorique() {
        ReflectionTestUtils.invokeMethod(service, "sendSolutionMailToClient", claim("awa@exemple.test", ClaimType.CLAIM), solution(), treator());

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendClientMail(eq("awa@exemple.test"), eq("Solution à votre réclamation"), message.capture(), eq(42L), isNull());
        assertThat(message.getValue()).contains("Votre compte a été rétabli.").doesNotContain("NOTE INTERNE");
        verify(events).log(eq(42L), eq("REC-ab12"), eq(ClaimType.CLAIM), eq(ClaimEventType.MAIL_SENT_CLIENT),
                eq("Agent Test"), eq("agent@exemple.test"), eq("Awa Test"));
    }

    @Test
    void unePlainteDeTypeDenonciationParleDeDenonciation() {
        ReflectionTestUtils.invokeMethod(service, "sendSolutionMailToClient", claim("awa@exemple.test", ClaimType.DENUNCIACION), solution(), treator());

        verify(mailService).sendClientMail(eq("awa@exemple.test"), eq("Solution à votre dénonciation"), anyString(), eq(42L), isNull());
    }

    @Test
    void uneErreurDEnvoiNInterrompJamaisLeTraitement() {
        doThrow(new IllegalStateException("smtp indisponible")).when(mailService).sendClientMail(anyString(), anyString(), anyString(), anyLong(), any());

        ReflectionTestUtils.invokeMethod(service, "sendSolutionMailToClient", claim("awa@exemple.test", ClaimType.CLAIM), solution(), treator());

        verify(events, never()).log(anyLong(), anyString(), any(), any(), anyString(), anyString(), anyString());
    }
}
