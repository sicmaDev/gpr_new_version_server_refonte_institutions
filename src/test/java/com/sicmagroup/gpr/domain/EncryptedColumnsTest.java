package com.sicmagroup.gpr.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;
import com.sicmagroup.gpr.utils.crypto.FieldEncryptor;

import jakarta.persistence.EntityManager;

/**
 * Étape 3 : les colonnes sensibles sont chiffrées en base et relues en clair.
 * Chaque test tourne dans une transaction annulée à la fin : rien ne reste en base.
 */
@SpringBootTest
@Transactional
class EncryptedColumnsTest {

    private static final String TEL = "97000000";

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private SuggestionRepository suggestionRepository;

    @Autowired
    private EntityManager em;

    private String colonneEnBase(String table, String colonne, Long id) {
        Object v = em.createNativeQuery("SELECT " + colonne + " FROM " + table + " WHERE id = ?1")
                .setParameter(1, id).getSingleResult();
        return v == null ? null : v.toString();
    }

    private Claim nouvelleReclamation(String tel) {
        return Claim.builder()
                .code("TEST-ENC-" + UUID.randomUUID())
                .type(ClaimType.CLAIM)
                .status(ClaimStatus.SAVED)
                .clientFirstAndLastName("Koffi Adjovi")
                .address("Akpakpa, Cotonou")
                .tel(tel)
                .email("koffi@example.com")
                .content("Mon compte a été débité deux fois 😡")
                .transmissionComment("Transmis au chef d'agence")
                .draftSolution("Brouillon de solution")
                .draftCommentaire("Brouillon de commentaire")
                .delete_reason("Motif de test")
                .build();
    }

    @Test
    void reclamationChiffreeEnBaseEtRelueEnClair() {
        Claim saved = claimRepository.saveAndFlush(nouvelleReclamation(TEL));
        Long id = saved.getId();

        for (String col : List.of("client_first_and_last_name", "address", "tel", "email", "content",
                "transmission_comment", "draft_solution", "draft_commentaire", "delete_reason")) {
            assertThat(colonneEnBase("gps_claim", col, id)).as(col).startsWith(FieldEncryptor.PREFIX);
        }
        assertThat(colonneEnBase("gps_claim", "tel_hash", id))
                .isEqualTo(FieldEncryptor.current().blindIndex(TEL));

        em.clear();
        Claim relue = claimRepository.findById(id).orElseThrow();
        assertThat(relue.getClientFirstAndLastName()).isEqualTo("Koffi Adjovi");
        assertThat(relue.getAddress()).isEqualTo("Akpakpa, Cotonou");
        assertThat(relue.getTel()).isEqualTo(TEL);
        assertThat(relue.getEmail()).isEqualTo("koffi@example.com");
        assertThat(relue.getContent()).isEqualTo("Mon compte a été débité deux fois 😡");
        assertThat(relue.getTransmissionComment()).isEqualTo("Transmis au chef d'agence");
        assertThat(relue.getDraftSolution()).isEqualTo("Brouillon de solution");
        assertThat(relue.getDraftCommentaire()).isEqualTo("Brouillon de commentaire");
        assertThat(relue.getDelete_reason()).isEqualTo("Motif de test");
    }

    @Test
    void reclamationRetrouveeParTelephone() {
        String tel = "9" + (System.nanoTime() % 10_000_000);
        Claim saved = claimRepository.saveAndFlush(nouvelleReclamation(tel));
        em.clear();

        assertThat(claimRepository.findByTelAndIsDeletedFalse(tel)).extracting(Claim::getId).containsExactly(saved.getId());
        // Les espaces sont ignorés
        assertThat(claimRepository.findByTelAndIsDeletedFalse(" " + tel + " ")).extracting(Claim::getId).containsExactly(saved.getId());
        assertThat(claimRepository.findByTelAndTypeAndStatus(tel, ClaimType.CLAIM, ClaimStatus.SAVED))
                .extracting(Claim::getId).containsExactly(saved.getId());
        assertThat(claimRepository.findByTelAndStatusInAndIsDeletedFalse(tel, List.of(ClaimStatus.SAVED)))
                .extracting(Claim::getId).containsExactly(saved.getId());
        assertThat(claimRepository.findByTelAndIsDeletedFalse(tel + "1")).isEmpty();
    }

    @Test
    void telephoneVideNeRenvoieRien() {
        claimRepository.saveAndFlush(nouvelleReclamation(null));
        assertThat(claimRepository.findByTelAndIsDeletedFalse(null)).isEmpty();
        assertThat(claimRepository.findByTelAndTypeAndStatus("  ", ClaimType.CLAIM, ClaimStatus.SAVED)).isEmpty();
    }

    @Test
    void ancienneValeurEnClairRelueTelleQuelle() {
        Claim saved = claimRepository.saveAndFlush(nouvelleReclamation(TEL));
        em.createNativeQuery("UPDATE gps_claim SET content = 'ancien texte en clair' WHERE id = ?1")
                .setParameter(1, saved.getId()).executeUpdate();
        em.clear();
        assertThat(claimRepository.findById(saved.getId()).orElseThrow().getContent()).isEqualTo("ancien texte en clair");
    }

    @Test
    void suggestionChiffreeEnBaseEtRelueEnClair() {
        Suggestion s = Suggestion.builder()
                .code("TEST-ENC-" + UUID.randomUUID())
                .status(ClaimStatus.SAVED)
                .clientFirstAndLastName("Afi Mensah")
                .address("Lomé")
                .tel(TEL)
                .email("afi@example.com")
                .content("Ouvrir le samedi")
                .commentaire("Bonne idée")
                .delete_reason("Motif")
                .build();
        Long id = suggestionRepository.saveAndFlush(s).getId();

        for (String col : List.of("client_first_and_last_name", "address", "tel", "email", "content", "commentaire", "delete_reason")) {
            assertThat(colonneEnBase("gps_suggestion", col, id)).as(col).startsWith(FieldEncryptor.PREFIX);
        }
        em.clear();
        Suggestion relue = suggestionRepository.findById(id).orElseThrow();
        assertThat(relue.getClientFirstAndLastName()).isEqualTo("Afi Mensah");
        assertThat(relue.getContent()).isEqualTo("Ouvrir le samedi");
        assertThat(relue.getCommentaire()).isEqualTo("Bonne idée");
    }

    @Test
    void colonnesAgrandiesEnBase() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT table_name, column_name, data_type, character_maximum_length FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND ((table_name IN ('gps_claim','gps_suggestion') "
                        + "AND column_name IN ('client_first_and_last_name','address','tel','email')) "
                        + "OR (table_name = 'gps_claim' AND column_name = 'tel_hash'))")
                .getResultList();
        assertThat(rows).hasSize(9);
        for (Object[] r : rows) {
            long max = ((Number) r[3]).longValue();
            if ("tel_hash".equals(r[1])) {
                assertThat(max).as("tel_hash").isGreaterThanOrEqualTo(64);
            } else {
                assertThat(max).as(r[0] + "." + r[1]).isGreaterThanOrEqualTo(1024);
            }
        }
    }
}
