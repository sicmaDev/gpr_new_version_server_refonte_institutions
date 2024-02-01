package com.sicmagroup.gpr.service.chat.message;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.api.chat.message.ChooseSolutionRequest;
import com.sicmagroup.gpr.api.chat.message.InitVoteRequest;
import com.sicmagroup.gpr.api.chat.message.MessageListRequest;
import com.sicmagroup.gpr.api.chat.message.NewMessageRequest;
import com.sicmagroup.gpr.api.chat.message.SessionJoinRequest;
import com.sicmagroup.gpr.api.chat.message.SessionJoinResponse;
import com.sicmagroup.gpr.api.chat.message.VoteRequest;
import com.sicmagroup.gpr.domain.dto.chat.UserVoteDto;
import com.sicmagroup.gpr.domain.dto.chat.VoteDto;
import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;
import com.sicmagroup.gpr.domain.enumeration.ChatStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.enumeration.SolutionStatus;
import com.sicmagroup.gpr.domain.enumeration.VoteType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.domain.model.chat.Chat;
import com.sicmagroup.gpr.domain.model.chat.Message;
import com.sicmagroup.gpr.domain.model.chat.UserVote;
import com.sicmagroup.gpr.domain.model.chat.Vote;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.SolutionRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.repository.chat.ChatRepository;
import com.sicmagroup.gpr.repository.chat.MessageRepository;
import com.sicmagroup.gpr.repository.chat.UserVoteRepository;
import com.sicmagroup.gpr.repository.chat.VoteRepository;
import com.sicmagroup.gpr.utils.Utils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageServiceImp implements MessageService {

    private final MessageRepository repository;

    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final ModelMapper modelMapper;
    private final UserVoteRepository userVoteRepository;
    private final MessageRepository messageRepository;
    private final VoteRepository voteRepository;
    private final SolutionRepository solutionRepository;

    @Override
    public Message send(NewMessageRequest request) throws Exception {
        // Chat chat = chatRepository.findById(request.getChatId()).orElseThrow(() ->
        // new Exception("Session de la réclamation introuvable"));
        Claim claim = claimRepository.findByCode(request.getClaimCode())
                .orElseThrow(() -> new Exception("La réclamation est introuvable"));
        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new Exception("L'utilisateur envoyant le message est introuvable"));
        Chat chat = chatRepository.findByClaim(claim)
                .orElseThrow(() -> new Exception("Session de la réclamation introuvable"));
        // List<User> members = userRepository.findByChatsMemberIn(Arrays.asList(chat));
        // List<User> guests = userRepository.findByChatsGuestIn(Arrays.asList(chat));
        // System.err.println(claim.getSession());
        // System.err.println(claim.getSession().getMembers());
        if (chat.getStatus().equals(ChatStatus.OPEN) && (chat.getMembers().contains(sender)
                || chat.getGuests().contains(sender))) {

            System.out.println("Test good damn fuck");
            Message message = Message
                    .builder()
                    .content(request.getContent())
                    .sender(sender)
                    .chat(chat)

                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            message = repository.save(message);
            if (request.isVote()) {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, String> resultat = objectMapper.readValue(request.getContent(), new TypeReference<>() {
                });
                message.setVote(true);

                Vote vote = Vote
                        .builder()
                        .author(sender)
                        .chat(chat)
                        .commentaire(resultat.get("commentaire"))
                        .contenu(resultat.get("contenu"))
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .userVotes(new ArrayList<>())
                        .message(message)
                        .build();
                vote = voteRepository.save(vote);
                message.setLinkedVote(vote);
                message = messageRepository.saveAndFlush(message);
            }

            return message;

            // else {
            // Message message = Message
            // .builder()
            // .content(request.getContent())
            // .sender(sender)
            // .chat(chat)
            // .createdAt(LocalDateTime.now())
            // .updatedAt(LocalDateTime.now())
            // .build();
            // message = repository.save(message);
            // // if(sender.getMessages() != null){
            // // sender.getMessages().add(message);

            // // } else {
            // // sender.setMessages(Arrays.asList(message));
            // // }
            // // userRepository.save(sender);
            // return message;
            // }

        }
        // else if (claim.getSession().getStatus().equals(ChatStatus.CLOSED)) {
        // throw new Exception("La session est fermée, veuillez actualiser svp!");
        // }
        else {

            throw new Exception("Vous n'êtes pas dans la session, vous ne pouvez pas interagir avec ce dernier.");
        }

    }

    @Override
    public List<Message> getList(MessageListRequest request) throws Exception {
        Chat chat = chatRepository.findById(request.getChatId())
                .orElseThrow(() -> new Exception("La session est introuvable"));
        List<Message> messages = repository.findByChat(chat);
        return messages;
    }

    @Override
    public SessionJoinResponse joinSession(SessionJoinRequest request) throws Exception {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new Exception("L'utilisateur envoyant le message est introuvable"));
        Claim claim = claimRepository.findByCode(request.getClaimCode())
                .orElseThrow(() -> new Exception("La réclamation est introuvable"));

        Chat chat = claim.getSession();
        SessionJoinResponse sessionJoinResponse = new SessionJoinResponse();
        if (chat != null) {
            sessionJoinResponse = SessionJoinResponse.builder()
                    .claimCode(claim.getCode())
                    .code(user.getCode())
                    .firstAndLastName(user.getFirstandlastname())
                    .role(user.getAdditionalrole())
                    .id(user.getId())
                    .status(request.getStatus())
                    .build();
        }

        return sessionJoinResponse;
    }

    @Override
    public VoteDto initVote(InitVoteRequest request) throws Exception {
        User user = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new Exception("L'utilisateur envoyant le message est introuvable"));
        Claim claim = claimRepository.findByCode(request.getClaimCode())
                .orElseThrow(() -> new Exception("La réclamation est introuvable"));

        Vote vote = Vote
                .builder()
                .author(user)
                .chat(claim.getSession())
                .commentaire(request.getCommentaire())
                .contenu(request.getContenu())
                .userVotes(new ArrayList<UserVote>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isChoosed(false)
                .build();

        return this.convertToDto(vote);

    }

    private VoteDto convertToDto(Vote vote) {
        VoteDto voteDto = modelMapper.map(vote, VoteDto.class);

        voteDto.setChatId(vote.getChat().getId());
        voteDto.setAuthor(this.convertToResponse(vote.getAuthor()));
        if (vote.getUserVotes() != null && !vote.getUserVotes().isEmpty()) {
            voteDto.setUserVote(vote.getUserVotes().stream().map(this::convertToDto).collect(Collectors.toList()));
        }
        if (vote.getMessage() != null) {
            voteDto.setMessageId(vote.getMessage().getId());
        }

        return voteDto;
    }

    private UserVoteDto convertToDto(UserVote userVote) {
        UserVoteDto userVoteDto = modelMapper.map(userVote, UserVoteDto.class);
        userVoteDto.setAuthor(this.convertToResponse(userVote.getUser()));
        return userVoteDto;
    }

    private UserResponse convertToResponse(User user) {
        UserResponse userResponse = modelMapper.map(user, UserResponse.class);
        return userResponse;
    }

    @Override
    public List<Message> vote(VoteRequest request) throws Exception {
        User user = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new Exception("L'utilisateur envoyant le message est introuvable"));
        Claim claim = claimRepository.findByCode(request.getClaimCode())
                .orElseThrow(() -> new Exception("La réclamation est introuvable"));
        Message message = messageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new Exception("Le vote est introuvable"));

        Vote vote = message.getLinkedVote();
        // if(vote.getUserVotes())
        Optional<UserVote> optUserVote = userVoteRepository.findByUserAndVote(user, vote);
        if (optUserVote.isPresent()) {
            UserVote userVote = optUserVote.get();
            // if (request.isRemoveVote()) {
            // userVoteRepository.delete(userVote);

            // vote.getUserVotes().remove(userVote);

            // } else {
            vote.getUserVotes().remove(userVote);
            userVote.setVoteType(request.isPour() ? VoteType.POUR : VoteType.CONTRE);
            userVote = userVoteRepository.save(userVote);

            vote.getUserVotes().add(userVote);
            // }
        } else {
            UserVote userVote = UserVote
                    .builder()
                    .user(user)
                    .vote(vote)
                    .voteType(request.isPour() ? VoteType.POUR : VoteType.CONTRE)
                    .build();
            userVote = userVoteRepository.save(userVote);
            if (vote.getUserVotes() != null) {
                vote.getUserVotes().add(userVote);
            } else {
                vote.setUserVotes(Arrays.asList(userVote));
            }
        }

        Chat chat = chatRepository.findByClaim(claim).orElseThrow(() -> new Exception("Le chat est introuvable"));

        List<Message> messages = messageRepository.findByChat(chat);

        return messages;

    }

    @Override
    public Chat chooseSolution(ChooseSolutionRequest request) throws Exception {
        Message message = messageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new Exception("Le vote est introuvable"));
        Claim claim = claimRepository.findByCode(request.getClaimCode())
                .orElseThrow(() -> new Exception("La réclamation est introuvable"));
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> resultat = objectMapper.readValue(message.getContent(), new TypeReference<>() {
        });
        Chat chat = chatRepository.findByClaim(claim).orElseThrow(() -> new Exception("Le chat est introuvable"));
        if (!claim.getStatus().equals(ClaimStatus.TREAT)) {
            Solution solution2 = Solution
                    .builder()
                    .author(message.getSender())
                    .commentaire(resultat.get("commentaire"))
                    .content(resultat.get("contenu"))
                    .createdAt(LocalDateTime.now())
                    .claim(claim)
                    .status(SolutionStatus.APPROVED)
                    .updatedAt(LocalDateTime.now())
                    .build();
            solution2 = solutionRepository.save(solution2);

            claim.setStatus(ClaimStatus.TREAT);
            claim.setUpdatedAt(LocalDateTime.now());
            claim.setTreatBy(message.getSender());
            if (claim.getSolutions() != null) {
                claim.getSolutions().add(solution2);
            }
            claim = claimRepository.save(claim);

            // chat.setStatus(ChatStatus.CLOSED);
            chat.setUpdatedAt(LocalDateTime.now());
            chat = chatRepository.save(chat);

            if (message.isVote() && message.getLinkedVote() != null) {
                Vote vote = message.getLinkedVote();
                vote.setChoosed(true);
                vote = voteRepository.save(vote);
            }

            Double apercuContent = claim.getContent().length() * 0.3;
            List<User> pilote = userRepository.findByAdditionalroleIn(Arrays.asList(Role.PILOTE));
            if (pilote != null && !pilote.isEmpty()) {
                String messageStr = "" +
                        "Cher(e) " + pilote.get(0).getFirstandlastname() + ", Pilote d'Assilassimé Solidarité.\n\n" +
                        "Le Comité de Gestion des Réclamations a examiné la réclamation portant le code : "
                        + claim.getCode()
                        + " et l'a traitée." + "\n\n" +
                        "Détails de la réclamation :" + "\n\n" +
                        "* Code de réclamation : " + claim.getCode() + "\n" +
                        "* Date d'enregistrement : " + Utils.convertLocalDateTimeToStr(claim.getReceiptDateTime())
                        + "\n" +
                        "* Aperçu du contenu : " + claim.getContent().substring(0, apercuContent.intValue()) + "...\n\n"
                        +
                        "La solution proposée par le CGR est la suivante : " + "\n" +
                        solution2.getContent() + "\n\n" +
                        "Nous vous invitons à communiquer la solution au pilote pour mesurer sa satisfaction ";

                Utils.sendmail(pilote.get(0).getEmail(), "Réclamation traitée",
                        messageStr, null, "reclamations@assilassime.org");
            }
        }

        return chat;

    };
}
