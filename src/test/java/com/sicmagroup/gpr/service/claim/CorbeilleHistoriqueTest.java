package com.sicmagroup.gpr.service.claim;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sicmagroup.gpr.domain.enumeration.ClaimEventType;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.service.claimEvent.ClaimEventServiceImpl;
import com.sicmagroup.gpr.service.log.LogServiceImpl;

import jakarta.servlet.http.HttpServletRequest;

/** Mise à la corbeille et restauration : elles sont tracées dans l'historique du dossier. */
class CorbeilleHistoriqueTest {

    private final ClaimEventServiceImpl events = mock(ClaimEventServiceImpl.class);
    private final ClaimRepository repo = mock(ClaimRepository.class);

    private ClaimServiceImpl service() {
        ClaimServiceImpl s = mock(ClaimServiceImpl.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(s, "repository", repo);
        ReflectionTestUtils.setField(s, "claimEventServiceImpl", events);
        ReflectionTestUtils.setField(s, "logServiceImpl", mock(LogServiceImpl.class));
        ReflectionTestUtils.setField(s, "httpServletRequest", mock(HttpServletRequest.class));
        return s;
    }

    private User agent() {
        User u = new User();
        u.setId(3L);
        u.setFirstandlastname("Awa Test");
        u.setEmail("awa@exemple.test");
        return u;
    }

    private Claim claim() {
        Claim c = new Claim();
        c.setId(9L);
        c.setCode("REC-1");
        c.setCodeClient("REC-ab12");
        c.setType(ClaimType.CLAIM);
        return c;
    }

    @Test
    void laSuppressionEstTraceeAvecSonMotif() throws Exception {
        service().deleteById(claim(), "Doublon", agent());

        verify(events).log(eq(9L), eq("REC-ab12"), eq(ClaimType.CLAIM), eq(ClaimEventType.DELETED),
                eq("Awa Test"), eq("awa@exemple.test"), eq("Doublon"));
    }

    @Test
    void laRestaurationEstTracee() throws Exception {
        Claim c = claim();
        c.setDeleted(true);
        when(repo.findById(9L)).thenReturn(Optional.of(c));

        service().restoreById(9L, agent());

        verify(events).log(eq(9L), eq("REC-ab12"), eq(ClaimType.CLAIM), eq(ClaimEventType.RESTORED),
                eq("Awa Test"), eq("awa@exemple.test"), any());
    }
}
