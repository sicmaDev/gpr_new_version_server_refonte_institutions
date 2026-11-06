package com.sicmagroup.gpr.service.claim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Poste;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.User;

/**
 * Écran « Mesure de satisfaction » : qui voit quelles plaintes traitées.
 * H5, Pilote et DE voient tout ; un RA seulement les plaintes enregistrées dans son agence ; un agent seulement les siennes.
 */
class PerimetreMesureSatisfactionTest {

    private final ServicePoint agenceA = ServicePoint.builder().id(1L).build();
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
        return mock(ClaimServiceImpl.class, CALLS_REAL_METHODS);
    }

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
    void unRaSansH5VoitSeulementSonAgence() {
        User ra = user(4, "H1", Role.MOLDUE, true, agenceA);
        assertThat(service().restrictClaimsToMeasureScope(List.of(deA, deB), ra)).containsExactly(deA);
    }

    @Test
    void unRaNeVoitPasUnDossierDUneAutreAgenceTransmisOuAffecteASonAgence() {
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
