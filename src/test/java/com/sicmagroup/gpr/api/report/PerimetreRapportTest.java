package com.sicmagroup.gpr.api.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Poste;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.servicePoint.ServicePointServiceImpl;

/** Rapport global : qui voit tout (H6, Pilote, DE) et quel périmètre pour les autres (agence, plus ses rattachées pour un RA). */
class PerimetreRapportTest {

    private final ReportController controller = mock(ReportController.class, CALLS_REAL_METHODS);

    private User user(String habilitations, Role role, boolean ra, ServicePoint sp) {
        Poste poste = new Poste();
        poste.setHabilitations(habilitations);
        User u = new User();
        u.setPoste(poste);
        u.setAdditionalrole(role);
        u.setRa(ra);
        u.setServicePoint(sp);
        return u;
    }

    private ServicePoint sp(long id) {
        ServicePoint s = new ServicePoint();
        s.setId(id);
        return s;
    }

    private boolean elevated(User u) {
        return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(controller, "isElevatedForReport", u));
    }

    @Test
    void h6PiloteEtDeVoientToutLesAutresNon() {
        assertThat(elevated(user("H11,H6", Role.MOLDUE, false, sp(1)))).isTrue();
        assertThat(elevated(user("H11", Role.PILOTE, false, sp(1)))).isTrue();
        assertThat(elevated(user("H11", Role.DE, false, sp(1)))).isTrue();
        assertThat(elevated(user("H11", Role.MOLDUE, false, sp(1)))).isFalse();
        assertThat(elevated(user("H11", Role.MOLDUE, true, sp(1)))).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void unRaVoitSonAgenceEtSesRattacheesUnSimpleAgentSeulementLaSienne() {
        ServicePointServiceImpl spService = mock(ServicePointServiceImpl.class);
        ReflectionTestUtils.setField(controller, "spServiceImpl", spService);
        when(spService.getAllDescendants(10L)).thenReturn(List.of(sp(11), sp(12)));

        List<Long> ra = (List<Long>) ReflectionTestUtils.invokeMethod(controller, "computeScopedServicePointIds",
                user("H11", Role.MOLDUE, true, sp(10)));
        List<Long> agent = (List<Long>) ReflectionTestUtils.invokeMethod(controller, "computeScopedServicePointIds",
                user("H11", Role.MOLDUE, false, sp(10)));

        assertThat(ra).containsExactly(10L, 11L, 12L);
        assertThat(agent).containsExactly(10L);
    }
}
