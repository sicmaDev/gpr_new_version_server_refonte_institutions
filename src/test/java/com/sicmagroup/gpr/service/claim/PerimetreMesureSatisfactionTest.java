package com.sicmagroup.gpr.service.claim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Poste;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.servicePoint.ServicePointServiceImpl;

/**
 * Écran « Mesure de satisfaction » : qui voit quelles plaintes traitées.
 * H5, Pilote et DE voient tout ; un RA voit les plaintes enregistrées dans son agence et dans les agences qui lui sont rattachées ; un agent seulement les siennes.
 */
class PerimetreMesureSatisfactionTest {

    private final ServicePoint direction = ServicePoint.builder().id(100L).build();
    private final ServicePoint agenceA = ServicePoint.builder().id(1L).direction_id(100L).build();
    private final ServicePoint agenceB = ServicePoint.builder().id(2L).build();

    private User user(long id, String habilitations, Role role, boolean ra, ServicePoint agence) {
        Poste poste = new Poste();
        poste.setHabilitations(habilitations);
        User u = new User();
        u.setId(id);
        u.setPoste(poste);
        u.setAdditionalrole(role);
        u.setRa(ra);
        u.setServicePoint(agence);
        return u;
    }

    private Claim claim(long id, ServicePoint agence) {
        Claim c = new Claim();
        c.setId(id);
        c.setServicePoint(agence);
        return c;
    }

    private ClaimServiceImpl service() {
        ClaimServiceImpl service = mock(ClaimServiceImpl.class, CALLS_REAL_METHODS);
        ServicePointServiceImpl points = mock(ServicePointServiceImpl.class);
        when(points.getAllDescendants(100L)).thenReturn(new ArrayList<>(List.of(agenceA)));
        when(points.getAllDescendants(1L)).thenReturn(new ArrayList<>());
        when(points.getAllDescendants(2L)).thenReturn(new ArrayList<>());
        ReflectionTestUtils.setField(service, "servicePointServiceImpl", points);
        return service;
    }

    private final Claim deDirection = claim(5, direction);
    private final Claim deA = claim(10, agenceA);
    private final Claim deB = claim(20, agenceB);

    @Test
    void unUtilisateurH5VoitToutMemeSiEstRa() {
        User h5 = user(1, "H5", Role.MOLDUE, true, agenceA);
        assertThat(service().restrictClaimsToMeasureScope(List.of(deA, deB), h5)).containsExactly(deA, deB);
    }

    @Test
    void lePiloteEtLeDeVoientTout() {
        User pilote = user(2, "H1", Role.PILOTE, false, agenceA);
        User de = user(3, "H1", Role.DE, false, agenceA);
        assertThat(service().restrictClaimsToMeasureScope(List.of(deA, deB), pilote)).containsExactly(deA, deB);
        assertThat(service().restrictClaimsToMeasureScope(List.of(deA, deB), de)).containsExactly(deA, deB);
    }

    @Test
    void unRaSansAgenceRattacheeVoitSeulementSonAgence() {
        User ra = user(4, "H1", Role.MOLDUE, true, agenceA);
        assertThat(service().restrictClaimsToMeasureScope(List.of(deA, deB), ra)).containsExactly(deA);
    }

    @Test
    void unRaAvecAgencesRattacheesVoitSonAgenceEtCellesQuiLuiSontRattachees() {
        User raDirection = user(8, "H1", Role.MOLDUE, true, direction);
        assertThat(service().restrictClaimsToMeasureScope(List.of(deDirection, deA, deB), raDirection))
                .containsExactly(deDirection, deA);
    }

    @Test
    void unRaNeVoitPasUnDossierDUneAutreAgenceNonRattacheeTransmisOuAffecteASonAgence() {
        User ra = user(4, "H1", Role.MOLDUE, true, agenceA);
        User collegue = user(6, "H1", Role.MOLDUE, false, agenceA);
        Claim transmis = claim(40, agenceB);
        transmis.setTransmitted(true);
        transmis.setTreatBy(collegue);
        Claim affecte = claim(50, agenceB);
        affecte.setTreatmentAffectedTo(ra);
        assertThat(service().restrictClaimsToMeasureScope(List.of(deA, transmis, affecte), ra)).containsExactly(deA);
    }

    @Test
    void unCompteH6SansH5NiRoleNeVoitPasTout() {
        User h6 = user(7, "H6", Role.MOLDUE, false, agenceA);
        assertThat(service().restrictClaimsToMeasureScope(List.of(deA, deB), h6)).isEmpty();
    }

    @Test
    void unAgentOrdinaireNeVoitQueSesPlaintes() {
        User agent = user(5, "H1", Role.MOLDUE, false, agenceA);
        Claim sienne = claim(30, agenceA);
        sienne.setCollector(agent);
        assertThat(service().restrictClaimsToMeasureScope(List.of(deA, sienne), agent)).containsExactly(sienne);
    }
}
