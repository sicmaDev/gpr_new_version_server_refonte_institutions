package com.sicmagroup.gpr.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sicmagroup.gpr.api.claimAudio.ClaimAudioResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ClaimAudio;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.service.claimAudio.ClaimAudioService;
import com.sicmagroup.gpr.service.jwt.JwtServiceImpl;
import com.sicmagroup.gpr.service.media.MediaService;
import com.sicmagroup.gpr.utils.crypto.FieldEncryptor;
import com.sicmagroup.gpr.utils.crypto.FileEncryptor;

/**
 * Étape 6 : un fichier envoyé puis téléchargé est identique, illisible sur le disque, et l'audio se relit.
 * Les fichiers créés sont supprimés à la fin ; la transaction est annulée (rien ne reste en base).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EncryptedFilesTest {

    @Autowired private MediaService mediaService;
    @Autowired private ClaimAudioService claimAudioService;
    @Autowired private ClaimRepository claimRepository;
    @Autowired private FieldEncryptor fieldEncryptor;
    @Autowired private JwtServiceImpl jwtService;
    @Autowired private MockMvc mockMvc;
    @MockBean private UserDetailsService userDetailsService;

    private final UserDetails agent = User.withUsername("agent.fichiers@gpr.local").password("x")
            .authorities("H1", "MOLDUE").build();
    private final List<Path> aSupprimer = new ArrayList<>();
    private final byte[] contenu = new byte[150_000];
    private final String suffixe = UUID.randomUUID().toString().substring(0, 8);

    @BeforeEach
    void setUp() {
        // D'autres tests installent des clés jetables : on remet celle de l'application
        FieldEncryptor.install(fieldEncryptor);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(agent);
        new SecureRandom().nextBytes(contenu);
        byte[] motif = "PIECE JOINTE CONFIDENTIELLE".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(motif, 0, contenu, 500, motif.length);
    }

    @AfterEach
    void nettoyer() throws Exception {
        for (Path p : aSupprimer) {
            Files.deleteIfExists(p);
        }
    }

    private Claim reclamation() {
        return claimRepository.saveAndFlush(Claim.builder()
                .code("TEST-FICHIER-" + UUID.randomUUID())
                .type(ClaimType.CLAIM).status(ClaimStatus.SAVED).build());
    }

    private String bearer() {
        return "Bearer " + jwtService.generateToken(agent);
    }

    @Test
    void pieceJointeEnvoyeePuisTelechargee() throws Exception {
        MultipartFile fichier = new MockMultipartFile("files", "test-enc-" + suffixe + ".pdf", "application/pdf", contenu);
        Media media = mediaService.store(new MultipartFile[] { fichier }, reclamation()).get(0);
        Path surDisque = Paths.get(media.getPath());
        aSupprimer.add(surDisque);

        // Illisible sur le disque
        byte[] brut = Files.readAllBytes(surDisque);
        assertThat(FileEncryptor.isEncrypted(brut)).isTrue();
        assertThat(new String(brut, StandardCharsets.ISO_8859_1)).doesNotContain("PIECE JOINTE CONFIDENTIELLE");

        // Téléchargement par la route du frontend : identique à l'original
        byte[] telecharge = mockMvc.perform(get("/api/v1/media/download/" + media.getId())
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(telecharge).isEqualTo(contenu);
    }

    @Test
    void audioEnvoyePuisEcoute() throws Exception {
        Claim claim = reclamation();
        MultipartFile vocal = new MockMultipartFile("audios", "test-enc-" + suffixe + ".ogg", "audio/ogg", contenu);
        ClaimAudio audio = claimAudioService.store(new MultipartFile[] { vocal }, claim).get(0);
        Path surDisque = Paths.get(audio.getPath());
        aSupprimer.add(surDisque);

        assertThat(FileEncryptor.isEncrypted(surDisque)).isTrue();

        // Lecture par la route du lecteur audio
        byte[] telecharge = mockMvc.perform(get("/api/v1/claimaudio/download/" + audio.getId())
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(telecharge).isEqualTo(contenu);

        // Liste des audios d'une réclamation (contenu renvoyé directement)
        List<ClaimAudioResponse> liste = claimAudioService.getAudioByClaim(claim);
        assertThat(liste).extracting(ClaimAudioResponse::getId).contains(audio.getId());
        assertThat(liste.stream().filter(a -> a.getId().equals(audio.getId())).findFirst().orElseThrow().getData())
                .isEqualTo(contenu);
    }

    @Test
    void ancienFichierEnClairToujoursTelechargeable() throws Exception {
        MultipartFile fichier = new MockMultipartFile("files", "test-enc-clair-" + suffixe + ".pdf", "application/pdf", contenu);
        Media media = mediaService.store(new MultipartFile[] { fichier }, reclamation()).get(0);
        Path surDisque = Paths.get(media.getPath());
        aSupprimer.add(surDisque);
        // Simule un fichier déposé avant l'étape 6 (en clair)
        Files.write(surDisque, contenu);

        byte[] telecharge = mockMvc.perform(get("/api/v1/media/download/" + media.getId())
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(telecharge).isEqualTo(contenu);
    }
}
