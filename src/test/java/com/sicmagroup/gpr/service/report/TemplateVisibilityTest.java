package com.sicmagroup.gpr.service.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Poste;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.Template;
import com.sicmagroup.gpr.domain.model.User;

/**
 * Modèles du Rapport Global : chacun voit les siens ; le RA voit ceux de ses agents (agence + rattachées) ;
 * l'agent voit ceux que son RA partage ; Pilote, DE et H14 voient tout. Agence 10, rattachée 11 (fille de 10).
 */
class TemplateVisibilityTest {

    private static ServicePoint sp(long id) {
        ServicePoint s = new ServicePoint();
        s.setId(id);
        return s;
    }

    private static User user(long id, String habilitations, Role role, boolean ra, long servicePointId) {
        Poste poste = new Poste();
        poste.setHabilitations(habilitations);
        User u = new User();
        u.setId(id);
        u.setPoste(poste);
        u.setAdditionalrole(role);
        u.setRa(ra);
        u.setServicePoint(sp(servicePointId));
        return u;
    }

    private static Template template(Long authorId, boolean shared, boolean isDefault) {
        Template t = new Template();
        t.setAuthorId(authorId);
        t.setSharedWithAgents(shared);
        t.setDefault(isDefault);
        return t;
    }

    private final User raAgence = user(1, "H11", Role.MOLDUE, true, 10);
    private final User agentAgence = user(2, "H11", Role.MOLDUE, false, 10);
    private final User agentRattache = user(3, "H11", Role.MOLDUE, false, 11);
    private final User autreAgent = user(4, "H11", Role.MOLDUE, false, 10);
    private final User agentAilleurs = user(5, "H11", Role.MOLDUE, false, 20);
    private final User pilote = user(6, "H11", Role.PILOTE, false, 20);
    private final User de = user(7, "H11", Role.DE, false, 20);
    private final User h14 = user(8, "H11,H14", Role.MOLDUE, false, 20);

    /** Périmètres tels que calculés par TemplateServiceImpl. */
    private boolean voit(User viewer, Template t, User author) {
        Set<Long> raScope = viewer.isRa() ? Set.of(10L, 11L) : Set.of(viewer.getServicePoint().getId());
        long sp = viewer.getServicePoint().getId();
        Set<Long> selfAndParents = sp == 11 ? Set.of(11L, 10L) : Set.of(sp);
        return TemplateVisibility.canSee(t, viewer, author, raScope, selfAndParents);
    }

    @Test
    void chacunVoitSesPropresModeles() {
        assertThat(voit(agentAgence, template(2L, false, false), agentAgence)).isTrue();
        assertThat(voit(raAgence, template(1L, false, false), raAgence)).isTrue();
    }

    @Test
    void unAgentNeVoitPasLesModelesDeSesCollegues() {
        assertThat(voit(agentAgence, template(4L, false, false), autreAgent)).isFalse();
    }

    @Test
    void leRaVoitLesModelesDeSesAgentsYComprisDesAgencesRattachees() {
        assertThat(voit(raAgence, template(2L, false, false), agentAgence)).isTrue();
        assertThat(voit(raAgence, template(3L, false, false), agentRattache)).isTrue();
        assertThat(voit(raAgence, template(5L, false, false), agentAilleurs)).isFalse();
    }

    @Test
    void lAgentNeVoitLeModeleDuRaQueSiLeRaLaPartage() {
        assertThat(voit(agentAgence, template(1L, true, false), raAgence)).isTrue();
        assertThat(voit(agentAgence, template(1L, false, false), raAgence)).isFalse();
        // agence rattachée : le partage du RA de l'agence principale s'applique aussi
        assertThat(voit(agentRattache, template(1L, true, false), raAgence)).isTrue();
        assertThat(voit(agentRattache, template(1L, false, false), raAgence)).isFalse();
        // hors périmètre : jamais
        assertThat(voit(agentAilleurs, template(1L, true, false), raAgence)).isFalse();
    }

    @Test
    void unModelePartageParUnNonRaNeDevientPasVisible() {
        assertThat(voit(agentAgence, template(4L, true, false), autreAgent)).isFalse();
    }

    @Test
    void piloteDeEtH14VoientToutMemeLesAnciensModelesSansAuteur() {
        Template ancien = template(null, false, false);
        for (User u : new User[] { pilote, de, h14 }) {
            assertThat(voit(u, template(2L, false, false), agentAgence)).isTrue();
            assertThat(voit(u, ancien, null)).isTrue();
            assertThat(TemplateVisibility.canEdit(template(2L, false, false), u)).isTrue();
        }
        assertThat(voit(agentAgence, ancien, null)).isFalse();
        assertThat(voit(raAgence, ancien, null)).isFalse();
    }

    @Test
    void leModeleParDefautEstVisibleParTous() {
        assertThat(voit(agentAilleurs, template(1L, false, true), raAgence)).isTrue();
    }

    @Test
    void seulsLAuteurEtPiloteDeH14PeuventModifier() {
        Template duRa = template(1L, true, false);
        assertThat(TemplateVisibility.canEdit(duRa, raAgence)).isTrue();
        assertThat(TemplateVisibility.canEdit(duRa, agentAgence)).isFalse();
        // le RA voit le modèle de son agent mais ne peut pas le modifier
        assertThat(TemplateVisibility.canEdit(template(2L, false, false), raAgence)).isFalse();
        assertThat(TemplateVisibility.canEdit(template(null, false, false), raAgence)).isFalse();
    }
}
