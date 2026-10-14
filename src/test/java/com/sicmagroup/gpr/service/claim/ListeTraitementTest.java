package com.sicmagroup.gpr.service.claim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.GravityLevel;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.domain.model.Poste;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ClaimRepository;

/**
 * Liste de traitement : un agent retrouve les plaintes dont il a rédigé la solution et qui ont été
 * désapprouvées, dans la limite des niveaux de risque qu'il peut traiter (H2 mineur, H3 moyen, H4 grave).
 */
class ListeTraitementTest {

    private User agent(String habilitations) {
        Poste poste = new Poste();
        poste.setHabilitations(habilitations);
        User u = new User();
        u.setId(7L);
        u.setPoste(poste);
        return u;
    }

    private Claim claim(long id, GravityLevel risque) {
        Claim c = new Claim();
        c.setId(id);
        c.setObjet(Objet.builder().risqueLevel(risque).build());
        return c;
    }

    private ClaimServiceImpl service(User agent, ClaimType type, Claim... desapprouvees) {
        ClaimServiceImpl service = mock(ClaimServiceImpl.class, CALLS_REAL_METHODS);
        ClaimRepository repo = mock(ClaimRepository.class);
        when(repo.findDisapprovedAuthoredBy(type, agent)).thenReturn(List.of(desapprouvees));
        ReflectionTestUtils.setField(service, "repository", repo);
        return service;
    }

    @Test
    void lAgentRetrouveSaPlainteDesapprouveeDontIlPeutTraiterLeRisque() {
        User agent = agent("H2,H3");
        Claim moyen = claim(1, GravityLevel.MOYEN);

        List<Claim> res = service(agent, ClaimType.CLAIM, moyen).addDisapprovedClaimsAuthoredBy(agent, ClaimType.CLAIM, List.of());

        assertThat(res).containsExactly(moyen);
    }

    @Test
    void unRisqueQueSonPosteNePeutPasTraiterResteCache() {
        User agent = agent("H2");
        Claim grave = claim(1, GravityLevel.GRAVE);

        List<Claim> res = service(agent, ClaimType.CLAIM, grave).addDisapprovedClaimsAuthoredBy(agent, ClaimType.CLAIM, List.of());

        assertThat(res).isEmpty();
    }

    @Test
    void unePlainteDejaDansLaListeNEstPasAjouteeDeuxFois() {
        User agent = agent("H3");
        Claim moyen = claim(1, GravityLevel.MOYEN);

        List<Claim> res = service(agent, ClaimType.CLAIM, moyen).addDisapprovedClaimsAuthoredBy(agent, ClaimType.CLAIM, List.of(moyen));

        assertThat(res).containsExactly(moyen);
    }
}
