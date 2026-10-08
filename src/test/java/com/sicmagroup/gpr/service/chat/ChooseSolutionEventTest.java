package com.sicmagroup.gpr.service.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sicmagroup.gpr.api.chat.message.ChooseSolutionRequest;
import com.sicmagroup.gpr.domain.enumeration.ClaimEventType;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.domain.model.chat.Chat;
import com.sicmagroup.gpr.domain.model.chat.Message;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.SolutionRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.repository.chat.ChatRepository;
import com.sicmagroup.gpr.repository.chat.MessageRepository;
import com.sicmagroup.gpr.service.chat.message.MessageServiceImp;
import com.sicmagroup.gpr.service.claimEvent.ClaimEventService;

/**
 * SLA lot 1 : choisir une solution dans une session fait passer la plainte en TREAT ;
 * ce changement de statut doit apparaître dans l'historique (événement APPROVED).
 * Test unitaire : aucun accès à la base.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChooseSolutionEventTest {

    @Mock private MessageRepository messageRepository;
    @Mock private ClaimRepository claimRepository;
    @Mock private ChatRepository chatRepository;
    @Mock private SolutionRepository solutionRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClaimEventService claimEventService;

    @InjectMocks private MessageServiceImp service;

    private Claim claim;
    private ChooseSolutionRequest request;

    @BeforeEach
    void setUp() {
        User sender = new User();
        sender.setFirstandlastname("Agent Session");
        sender.setEmail("agent@gpr.local");

        Message message = mock(Message.class);
        when(message.getContent()).thenReturn("{\"commentaire\":\"c\",\"contenu\":\"la solution\"}");
        when(message.getSender()).thenReturn(sender);

        claim = new Claim();
        claim.setId(5L);
        claim.setCode("code-5");
        claim.setCodeClient("REC-5");
        claim.setType(ClaimType.CLAIM);
        claim.setContent("contenu de la plainte");

        request = mock(ChooseSolutionRequest.class);
        when(request.getMessageId()).thenReturn(1L);
        when(request.getClaimCode()).thenReturn("code-5");

        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(claimRepository.findByCode("code-5")).thenReturn(Optional.of(claim));
        when(chatRepository.findByClaim(claim)).thenReturn(Optional.of(new Chat()));
        when(solutionRepository.save(any(Solution.class))).thenAnswer(inv -> inv.getArgument(0));
        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chatRepository.save(any(Chat.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void choisirUneSolutionTraceUnEvenement() throws Exception {
        claim.setStatus(ClaimStatus.AFFECTED);

        service.chooseSolution(request);

        verify(claimEventService).log(eq(5L), eq("REC-5"), eq(ClaimType.CLAIM), eq(ClaimEventType.APPROVED),
                eq("Agent Session"), eq("agent@gpr.local"), eq("session"));
    }

    @Test
    void uneReclamationDejaTraiteeNeProduitPasDeDoublon() throws Exception {
        claim.setStatus(ClaimStatus.TREAT);

        service.chooseSolution(request);

        verify(claimEventService, never()).log(any(), any(), any(), any(), any(), any(), any());
    }
}
