package com.sicmagroup.gpr.service.claim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sicmagroup.gpr.domain.dto.AlertDto;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Objet;

/**
 * SLA lot 1 : calcul des alertes de retard (/api/v1/alert/*).
 * Test unitaire : la liste des plaintes est simulée, aucune base de données n'est utilisée.
 */
class AlertesRetardTest {

    private Claim claim(long id, ClaimType type, int delaiJours, int recueIlYaJours) {
        Claim c = new Claim();
        c.setId(id);
        c.setCode("code-" + id);
        c.setCodeClient("REC-" + id);
        c.setClientFirstAndLastName("Jean Test");
        c.setType(type);
        c.setStatus(ClaimStatus.SAVED);
        c.setReceiptDateTime(LocalDateTime.now().minusDays(recueIlYaJours));
        c.setObjet(Objet.builder().processingTime(delaiJours).build());
        return c;
    }

    private List<AlertDto> alertes(ClaimType type, Claim... claims) {
        ClaimServiceImpl service = mock(ClaimServiceImpl.class, CALLS_REAL_METHODS);
        doReturn(List.of(claims)).when(service).getAllByTypeAndStatusNotIn(any(), any());
        return service.getAllAlertDtosByType(type);
    }

    @Test
    void plainteEnRetardDonneUneAlerte() {
        assertThat(alertes(ClaimType.CLAIM, claim(1, ClaimType.CLAIM, 2, 5))).hasSize(1);
    }

    @Test
    void plainteDansLesDelaisNeDonnePasDAlerte() {
        assertThat(alertes(ClaimType.CLAIM, claim(1, ClaimType.CLAIM, 10, 2))).isEmpty();
    }

    @Test
    void objetSansDelaiNeDonnePasDAlerte() {
        assertThat(alertes(ClaimType.CLAIM, claim(1, ClaimType.CLAIM, 0, 30))).isEmpty();
    }

    @Test
    void plainteSupprimeeNeDonnePasDAlerte() {
        Claim supprimee = claim(1, ClaimType.CLAIM, 2, 5);
        supprimee.setDeleted(true);
        assertThat(alertes(ClaimType.CLAIM, supprimee)).isEmpty();
    }

    @Test
    void lePlaignantDUneDenonciationEstMasque() {
        List<AlertDto> res = alertes(ClaimType.DENUNCIACION, claim(1, ClaimType.DENUNCIACION, 2, 5));
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getClaimClient()).isNull();
    }

    @Test
    void lePlaignantDUneReclamationResteAffiche() {
        List<AlertDto> res = alertes(ClaimType.CLAIM, claim(1, ClaimType.CLAIM, 2, 5));
        assertThat(res.get(0).getClaimClient()).isEqualTo("Jean Test");
    }

    @Test
    void leNouveauStatutExiste() {
        assertThat(ClaimStatus.valueOf("WAITING_CUSTOMER")).isNotNull();
    }
}
