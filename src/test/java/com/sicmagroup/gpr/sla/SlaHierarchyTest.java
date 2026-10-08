package com.sicmagroup.gpr.sla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ServicePointRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.sla.domain.SlaOwnerLevel;
import com.sicmagroup.gpr.sla.service.SlaHierarchy;
import com.sicmagroup.gpr.sla.service.SlaHierarchy.Target;

/**
 * Hiérarchie de remontée : agent -> RA de l'agence -> direction -> Pilote -> DE, avec les niveaux absents
 * sautés. Test unitaire : aucune base de données.
 */
class SlaHierarchyTest {

    private final UserRepository userRepo = mock(UserRepository.class);
    private final ServicePointRepository spRepo = mock(ServicePointRepository.class);
    private SlaHierarchy hierarchy;

    private final ServicePoint direction = ServicePoint.builder().id(20L).libelle("Direction").build();
    private final ServicePoint agency = ServicePoint.builder().id(10L).libelle("Agence").direction_id(20L).build();
    private final User ra = User.builder().id(7L).firstandlastname("RA Agence").isRa(true).servicePoint(agency).build();
    private final User directionRa = User.builder().id(8L).firstandlastname("RA Direction").isRa(true)
            .servicePoint(direction).build();
    private final User pilote = User.builder().id(9L).firstandlastname("Pilote").additionalrole(Role.PILOTE).build();
    private final User de = User.builder().id(11L).firstandlastname("DE").additionalrole(Role.DE).build();
    private Claim claim;

    @BeforeEach
    void setUp() {
        hierarchy = new SlaHierarchy(userRepo, spRepo);
        claim = new Claim();
        claim.setServicePoint(agency);
        when(spRepo.findById(20L)).thenReturn(Optional.of(direction));
        when(userRepo.findRaByServicePointId(10L)).thenReturn(Optional.of(ra));
        when(userRepo.findRaByServicePointId(20L)).thenReturn(Optional.of(directionRa));
        when(userRepo.findByAdditionalroleInAndIsDeleted(eq(List.of(Role.PILOTE)), eq(false))).thenReturn(List.of(pilote));
        when(userRepo.findByAdditionalroleInAndIsDeleted(eq(List.of(Role.DE)), eq(false))).thenReturn(List.of(de));
    }

    @Test
    void lEchelleCompleteVaDeLAgentAuDe() {
        Target t1 = hierarchy.nextTarget(claim, SlaOwnerLevel.AGENT, 5L);
        assertThat(t1.user()).isSameAs(ra);
        assertThat(t1.level()).isEqualTo(SlaOwnerLevel.RA);
        assertThat(t1.copy()).containsExactly(directionRa); // le niveau d'après est en copie

        Target t2 = hierarchy.nextTarget(claim, SlaOwnerLevel.RA, 7L);
        assertThat(t2.user()).isSameAs(directionRa);
        assertThat(t2.level()).isEqualTo(SlaOwnerLevel.DIRECTION);

        Target t3 = hierarchy.nextTarget(claim, SlaOwnerLevel.DIRECTION, 8L);
        assertThat(t3.user()).isSameAs(pilote);
        assertThat(t3.level()).isEqualTo(SlaOwnerLevel.PILOTE);

        Target t4 = hierarchy.nextTarget(claim, SlaOwnerLevel.PILOTE, 9L);
        assertThat(t4.user()).isSameAs(de);
        assertThat(t4.level()).isEqualTo(SlaOwnerLevel.DE);
        assertThat(t4.copy()).isEmpty();

        assertThat(hierarchy.nextTarget(claim, SlaOwnerLevel.DE, 11L)).isNull(); // plus de niveau
    }

    @Test
    void uneAgenceSansRaRemonteDirectementALaDirection() {
        when(userRepo.findRaByServicePointId(10L)).thenReturn(Optional.empty());

        Target t = hierarchy.nextTarget(claim, SlaOwnerLevel.AGENT, 5L);

        assertThat(t.user()).isSameAs(directionRa);
        assertThat(t.level()).isEqualTo(SlaOwnerLevel.DIRECTION);
    }

    @Test
    void uneAgenceSansDirectionRemonteAuPilote() {
        agency.setDirection_id(null);

        Target t = hierarchy.nextTarget(claim, SlaOwnerLevel.RA, 7L);

        assertThat(t.user()).isSameAs(pilote);
        assertThat(t.level()).isEqualTo(SlaOwnerLevel.PILOTE);
    }

    @Test
    void sansRaNiDirectionLePiloteReçoitLeDossier() {
        agency.setDirection_id(null);
        when(userRepo.findRaByServicePointId(10L)).thenReturn(Optional.empty());

        Target t = hierarchy.nextTarget(claim, SlaOwnerLevel.AGENT, 5L);

        assertThat(t.level()).isEqualTo(SlaOwnerLevel.PILOTE);
    }

    @Test
    void siLeDossierEstDejaChezLaPersonneDuNiveauSuivantOnPasseAuNiveauDApres() {
        // le dossier est détenu par le RA lui-même, alors que le compteur le croit encore au niveau agent
        Target t = hierarchy.nextTarget(claim, SlaOwnerLevel.AGENT, ra.getId());

        assertThat(t.user()).isSameAs(directionRa);
    }

    @Test
    void uneDirectionSansRaMonteDUnCranDansLaHierarchie() {
        ServicePoint grandDirection = ServicePoint.builder().id(30L).libelle("Direction générale").build();
        direction.setDirection_id(30L);
        when(userRepo.findRaByServicePointId(20L)).thenReturn(Optional.empty());
        when(spRepo.findById(30L)).thenReturn(Optional.of(grandDirection));
        User dgRa = User.builder().id(12L).firstandlastname("RA DG").isRa(true).servicePoint(grandDirection).build();
        when(userRepo.findRaByServicePointId(30L)).thenReturn(Optional.of(dgRa));

        assertThat(hierarchy.directionRa(claim)).isSameAs(dgRa);
    }

    @Test
    void uneBoucleDansLaHierarchieNeBloquePas() {
        direction.setDirection_id(10L); // 10 -> 20 -> 10 -> ...
        when(userRepo.findRaByServicePointId(20L)).thenReturn(Optional.empty());
        when(userRepo.findRaByServicePointId(10L)).thenReturn(Optional.empty());
        when(spRepo.findById(10L)).thenReturn(Optional.of(agency));

        assertThat(hierarchy.directionRa(claim)).isNull();
    }

    @Test
    void niveauDUnUtilisateurParRapportAUnePlainte() {
        assertThat(hierarchy.levelOf(de, claim)).isEqualTo(SlaOwnerLevel.DE);
        assertThat(hierarchy.levelOf(pilote, claim)).isEqualTo(SlaOwnerLevel.PILOTE);
        assertThat(hierarchy.levelOf(ra, claim)).isEqualTo(SlaOwnerLevel.RA);
        assertThat(hierarchy.levelOf(directionRa, claim)).isEqualTo(SlaOwnerLevel.DIRECTION);
        assertThat(hierarchy.levelOf(User.builder().id(5L).build(), claim)).isEqualTo(SlaOwnerLevel.AGENT);
    }

    @Test
    void sansPiloteNiDeLaRemonteSArreteALaDirection() {
        when(userRepo.findByAdditionalroleInAndIsDeleted(any(), eq(false))).thenReturn(List.of());

        assertThat(hierarchy.nextTarget(claim, SlaOwnerLevel.DIRECTION, 8L)).isNull();
    }
}
