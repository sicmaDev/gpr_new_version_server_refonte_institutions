package com.sicmagroup.gpr.sla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.service.servicePoint.ServicePointServiceImpl;
import com.sicmagroup.gpr.sla.service.SlaPerimeter;
import com.sicmagroup.gpr.sla.service.SlaPerimeter.Scope;

/**
 * Le SLA ne crée aucune règle de visibilité : il reprend celle des listes de plaintes. Test unitaire.
 */
class SlaPerimeterTest {

    private final ServicePointServiceImpl spService = mock(ServicePointServiceImpl.class);
    private SlaPerimeter perimeter;

    private final ServicePoint direction = ServicePoint.builder().id(20L).build();
    private final ServicePoint agencyA = ServicePoint.builder().id(10L).direction_id(20L).build();
    private final ServicePoint agencyB = ServicePoint.builder().id(11L).direction_id(20L).build();
    private final ServicePoint other = ServicePoint.builder().id(99L).build();

    private final User agentA = User.builder().id(1L).servicePoint(agencyA).build();
    private final User agentB = User.builder().id(2L).servicePoint(agencyB).build();
    private final User raA = User.builder().id(3L).isRa(true).servicePoint(agencyA).build();
    private final User raDirection = User.builder().id(4L).isRa(true).servicePoint(direction).build();
    private final User pilote = User.builder().id(5L).additionalrole(Role.PILOTE).build();
    private final User de = User.builder().id(6L).additionalrole(Role.DE).build();

    @BeforeEach
    void setUp() {
        perimeter = new SlaPerimeter(mock(UserRepository.class), spService);
        when(spService.getByDirectionId(20L)).thenReturn(new ArrayList<>(List.of(agencyA, agencyB)));
        when(spService.getByDirectionId(10L)).thenReturn(new ArrayList<>());
    }

    private static Claim claim(ServicePoint sp, User collector, User affectedTo, User transmittedTo) {
        Claim c = new Claim();
        c.setServicePoint(sp);
        c.setCollector(collector);
        c.setTreatmentAffectedTo(affectedTo);
        c.setTransmittedTo(transmittedTo);
        return c;
    }

    @Test
    void piloteEtDeVoientToutesLesPlaintes() {
        assertThat(perimeter.scopeOf(pilote).all()).isTrue();
        assertThat(perimeter.scopeOf(de).all()).isTrue();
        assertThat(perimeter.canSee(perimeter.scopeOf(pilote), claim(other, null, null, null))).isTrue();
    }

    @Test
    void unRaVoitSonAgenceEtCellesQuiLuiSontRattachees() {
        Scope scope = perimeter.scopeOf(raDirection);

        assertThat(scope.all()).isFalse();
        assertThat(scope.spIds()).containsExactlyInAnyOrder(20L, 10L, 11L);
        assertThat(perimeter.canSee(scope, claim(agencyA, null, null, null))).isTrue();
        assertThat(perimeter.canSee(scope, claim(agencyB, null, null, null))).isTrue();
        assertThat(perimeter.canSee(scope, claim(other, null, null, null))).isFalse();
    }

    @Test
    void unRaSansAgenceRattacheeVoitSeulementLaSienne() {
        Scope scope = perimeter.scopeOf(raA);

        assertThat(scope.spIds()).containsExactly(10L);
        assertThat(perimeter.canSee(scope, claim(agencyB, null, null, null))).isFalse();
    }

    @Test
    void unRaVoitCeQuiLuiEstAffecteOuTransmisMemeHorsDeSonPerimetre() {
        Scope scope = perimeter.scopeOf(raA);

        assertThat(perimeter.canSee(scope, claim(other, null, raA, null))).isTrue();
        assertThat(perimeter.canSee(scope, claim(other, null, null, raA))).isTrue();
    }

    @Test
    void unRaNeVoitPasCeQuIlASaisiHorsDeSonPerimetre() {
        // même règle que les listes de plaintes : l'exclusion de « sa propre plainte » hors de son agence
        Scope scope = perimeter.scopeOf(raA);

        assertThat(scope.collectorUid()).isEqualTo(-1L);
        assertThat(perimeter.canSee(scope, claim(other, raA, null, null))).isFalse();
    }

    @Test
    void unAgentVoitSesDossiersSaisisAffectesOuTransmis() {
        Scope scope = perimeter.scopeOf(agentA);

        assertThat(scope.all()).isFalse();
        assertThat(perimeter.canSee(scope, claim(agencyA, agentA, null, null))).isTrue();
        assertThat(perimeter.canSee(scope, claim(agencyA, null, agentA, null))).isTrue();
        assertThat(perimeter.canSee(scope, claim(agencyA, null, null, agentA))).isTrue();
    }

    @Test
    void unAgentNeVoitPasLesPlaintesDesAutres() {
        Scope scope = perimeter.scopeOf(agentA);

        assertThat(perimeter.canSee(scope, claim(agencyA, agentB, agentB, null))).isFalse();
        assertThat(perimeter.canSee(scope, claim(agencyB, null, null, null))).isFalse();
    }

    @Test
    void sansUtilisateurRienNEstVisible() {
        Scope scope = perimeter.scopeOf(null);

        assertThat(scope.all()).isFalse();
        assertThat(perimeter.canSee(scope, claim(agencyA, agentA, agentA, agentA))).isFalse();
    }
}
