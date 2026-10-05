package com.sicmagroup.gpr.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.sicmagroup.gpr.domain.model.ExtraContent;
import com.sicmagroup.gpr.domain.model.HistoriqueAffectation;
import com.sicmagroup.gpr.domain.model.HistoriqueTransmission;
import com.sicmagroup.gpr.domain.model.SatisfactionMeasure;
import com.sicmagroup.gpr.domain.model.Setting;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.domain.model.chat.Message;
import com.sicmagroup.gpr.domain.model.chat.Vote;
import com.sicmagroup.gpr.repository.ExtraContentRepository;
import com.sicmagroup.gpr.repository.HistoriqueAffectationRepository;
import com.sicmagroup.gpr.repository.HistoriqueTransmissionRepository;
import com.sicmagroup.gpr.repository.SatisfactionMeasureRepository;
import com.sicmagroup.gpr.repository.SettingRepository;
import com.sicmagroup.gpr.repository.SolutionRepository;
import com.sicmagroup.gpr.repository.chat.MessageRepository;
import com.sicmagroup.gpr.repository.chat.VoteRepository;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
import com.sicmagroup.gpr.utils.crypto.FieldEncryptor;

import jakarta.persistence.EntityManager;

/**
 * Étape 3 (suite) : les 7 autres tables chiffrées.
 * Chaque test tourne dans une transaction annulée à la fin : rien ne reste en base.
 */
@SpringBootTest
@Transactional
class EncryptedOtherTablesTest {

    @Autowired private ExtraContentRepository extraContentRepository;
    @Autowired private SolutionRepository solutionRepository;
    @Autowired private SatisfactionMeasureRepository satisfactionMeasureRepository;
    @Autowired private SettingRepository settingRepository;
    @Autowired private SettingServiceImpl settingService;
    @Autowired private HistoriqueAffectationRepository historiqueAffectationRepository;
    @Autowired private HistoriqueTransmissionRepository historiqueTransmissionRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private EntityManager em;

    private String enBase(String table, String colonne, Long id) {
        Object v = em.createNativeQuery("SELECT " + colonne + " FROM " + table + " WHERE id = ?1")
                .setParameter(1, id).getSingleResult();
        return v == null ? null : v.toString();
    }

    @Test
    void complementDePlainte() {
        Long id = extraContentRepository.saveAndFlush(ExtraContent.builder().contenu("Complément : relevé joint").build()).getId();
        assertThat(enBase("gps_extra_content", "contenu", id)).startsWith(FieldEncryptor.PREFIX);
        em.clear();
        assertThat(extraContentRepository.findById(id).orElseThrow().getContenu()).isEqualTo("Complément : relevé joint");
    }

    @Test
    void solutionEtMesureDeSatisfaction() {
        SatisfactionMeasure mesure = SatisfactionMeasure.builder().commentaire("Client satisfait, merci 🙏").build();
        Solution solution = Solution.builder()
                .content("Remboursement effectué")
                .commentaire("Validé par le DE")
                .motifDesaprobation("Montant à revoir")
                .satisfactionMeasure(mesure)
                .build();
        Long id = solutionRepository.saveAndFlush(solution).getId();
        Long mesureId = solution.getSatisfactionMeasure().getId();

        for (String col : new String[] { "content", "commentaire", "motif_desaprobation" }) {
            assertThat(enBase("gps_solution", col, id)).as(col).startsWith(FieldEncryptor.PREFIX);
        }
        assertThat(enBase("gps_satisfaction_measure", "commentaire", mesureId)).startsWith(FieldEncryptor.PREFIX);

        em.clear();
        Solution relue = solutionRepository.findById(id).orElseThrow();
        assertThat(relue.getContent()).isEqualTo("Remboursement effectué");
        assertThat(relue.getCommentaire()).isEqualTo("Validé par le DE");
        assertThat(relue.getMotifDesaprobation()).isEqualTo("Montant à revoir");
        assertThat(satisfactionMeasureRepository.findById(mesureId).orElseThrow().getCommentaire())
                .isEqualTo("Client satisfait, merci 🙏");
    }

    @Test
    void parametresMailLusParLeService() throws Exception {
        String libelle = "TEST-ENC-" + UUID.randomUUID();
        String json = "{\"host\":\"smtp.example.com\",\"port\":587,\"pwd\":\"mot-de-passe-de-test\"}";
        Long id = settingRepository.saveAndFlush(Setting.builder().libelle(libelle).value(json).build()).getId();

        String brut = enBase("gps_setting", "value", id);
        assertThat(brut).startsWith(FieldEncryptor.PREFIX).doesNotContain("mot-de-passe-de-test");

        em.clear();
        // Même chemin que MailService / AuthenticationServiceImpl
        assertThat(settingService.getbySlug(libelle).getValue()).isEqualTo(json);
    }

    @Test
    void historiqueAffectation() {
        Long id = historiqueAffectationRepository.saveAndFlush(HistoriqueAffectation.builder()
                .codePlainte("TEST-ENC")
                .contentMail("<p>Bonjour, la plainte de M. Koffi vous est affectée</p>")
                .build()).getId();
        assertThat(enBase("gps_historique_affectations", "content_mail", id)).startsWith(FieldEncryptor.PREFIX);
        em.clear();
        assertThat(historiqueAffectationRepository.findById(id).orElseThrow().getContentMail())
                .isEqualTo("<p>Bonjour, la plainte de M. Koffi vous est affectée</p>");
    }

    @Test
    void historiqueTransmission() {
        Long id = historiqueTransmissionRepository.saveAndFlush(HistoriqueTransmission.builder()
                .claimId(-1L)
                .commentaire("Merci de traiter en priorité")
                .build()).getId();
        assertThat(enBase("gps_historique_transmissions", "commentaire", id)).startsWith(FieldEncryptor.PREFIX);
        em.clear();
        assertThat(historiqueTransmissionRepository.findByClaimIdOrderByDateTransmissionDesc(-1L))
                .extracting(HistoriqueTransmission::getCommentaire).containsExactly("Merci de traiter en priorité");
    }

    @Test
    void messageEtVoteDuChat() {
        Message message = messageRepository.saveAndFlush(Message.builder().content("Je propose un geste commercial").build());
        Long voteId = voteRepository.saveAndFlush(Vote.builder()
                .contenu("Geste commercial de 5 000 F")
                .commentaire("D'accord")
                .message(message)
                .build()).getId();

        assertThat(enBase("gps_message", "content", message.getId())).startsWith(FieldEncryptor.PREFIX);
        assertThat(enBase("gps_vote", "contenu", voteId)).startsWith(FieldEncryptor.PREFIX);
        assertThat(enBase("gps_vote", "commentaire", voteId)).startsWith(FieldEncryptor.PREFIX);

        em.clear();
        assertThat(messageRepository.findById(message.getId()).orElseThrow().getContent()).isEqualTo("Je propose un geste commercial");
        Vote relu = voteRepository.findById(voteId).orElseThrow();
        assertThat(relu.getContenu()).isEqualTo("Geste commercial de 5 000 F");
        assertThat(relu.getCommentaire()).isEqualTo("D'accord");
    }
}
