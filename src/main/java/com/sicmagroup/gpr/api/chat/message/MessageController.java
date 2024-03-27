package com.sicmagroup.gpr.api.chat.message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.chat.ChatDto;
import com.sicmagroup.gpr.domain.dto.chat.MessageDto;
import com.sicmagroup.gpr.domain.dto.chat.UserVoteDto;
import com.sicmagroup.gpr.domain.dto.chat.VoteDto;
import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;
import com.sicmagroup.gpr.domain.enumeration.MessageStatus;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.domain.model.chat.Chat;
import com.sicmagroup.gpr.domain.model.chat.Message;
import com.sicmagroup.gpr.domain.model.chat.UserVote;
import com.sicmagroup.gpr.domain.model.chat.Vote;
import com.sicmagroup.gpr.service.chat.ChatServiceImpl;
import com.sicmagroup.gpr.service.chat.message.MessageServiceImp;
import com.sicmagroup.gpr.utils.Utils;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final SimpMessagingTemplate simpleMessaging;
    private final MessageServiceImp service;
    private final ChatServiceImpl chatServiceImpl;
    private final SimpUserRegistry simpUserRegistry;
    private final ModelMapper modelMapper;

   

    @MessageMapping("/session/{sessionId}")
    @SendTo("/topic/session/{sessionId}")
    public MessageDto receiveMessage(@DestinationVariable String sessionId, @Payload NewMessageRequest message) {
        System.out.println("message#vote");
        System.out.println(message);
        Message message2 = new Message();
        try {
            message2 = service.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        MessageDto messageDto = this.convertToDto(message2);
        messageDto.setStatus(message.getStatus());
        return messageDto;
    }

    @MessageMapping("/session/join/{sessionId}")
    @SendTo("/topic/session/{sessionId}")
    public SessionJoinResponse joinSession(@DestinationVariable String sessionId,
            @Payload SessionJoinRequest userResponse) throws Exception {
        // System.out.println("message");
        // System.out.println(userResponse);
        // System.out.println(simpUserRegistry.getUsers());
        SessionJoinResponse sessionJoinResponse = new SessionJoinResponse();
        try {
            sessionJoinResponse = service.joinSession(userResponse);
            return sessionJoinResponse;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @MessageMapping("/session/join/guest/{sessionId}")
    @SendTo("/topic/session/{sessionId}")
    public SessionJoinResponse joinGuestSession(@DestinationVariable String sessionId,
            @Payload SessionJoinRequest request) throws Exception {
        // System.out.println("message");
        // System.out.println(userResponse);
        // System.out.println(simpUserRegistry.getUsers());

        SessionJoinResponse sessionJoinResponse = new SessionJoinResponse();
        try {
            sessionJoinResponse = chatServiceImpl.inviteGuest(request);
            sessionJoinResponse.setStatus(MessageStatus.INVITATION);

            return sessionJoinResponse;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @MessageMapping("/session/eject/guest/{sessionId}")
    @SendTo("/topic/session/{sessionId}")
    public SessionJoinResponse ejectGuestSession(@DestinationVariable String sessionId,
            @Payload SessionJoinRequest request) throws Exception {
        // System.out.println("message");
        // System.out.println(userResponse);
        // System.out.println(simpUserRegistry.getUsers());

        SessionJoinResponse sessionJoinResponse = new SessionJoinResponse();
        try {
            sessionJoinResponse = chatServiceImpl.ejectGuest(request);
             sessionJoinResponse.setStatus(MessageStatus.EJECTION);
            return sessionJoinResponse;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    // @MessageMapping("/session/init-vote/{sessionId}")
    // @SendTo("/topic/session/{sessionId}")
    // public VoteDto createVote(@DestinationVariable String sessionId,
    // @Payload InitVoteRequest request) throws Exception {
    // // System.out.println("message");
    // // System.out.println(userResponse);
    // // System.out.println(simpUserRegistry.getUsers());
    // VoteDto voteDto = new VoteDto();
    // try {
    // voteDto = service.initVote(request);
    // return voteDto;

    // } catch (Exception e) {
    // e.printStackTrace();
    // throw e;
    // }
    // }

    @MessageMapping("/session/vote/{sessionId}")
    @SendTo("/topic/session/{sessionId}")
    public HashMap<String, Object> vote(@DestinationVariable String sessionId,
            @Payload VoteRequest request) throws Exception {
        // System.out.println("message");
        // System.out.println(userResponse);
        // System.out.println(simpUserRegistry.getUsers());
        try {
            List<Message> messages = service.vote(request);
            List<MessageDto> messagesDto = messages.stream().map(this::convertToDto).collect(Collectors.toList());
            HashMap<String, Object> resultat = new HashMap<>();
            resultat.put("status", "VOTE");
            resultat.put("messages", messagesDto);
            return resultat;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @MessageMapping("/session/confirm-solution/{sessionId}")
    @SendTo("/topic/session/{sessionId}")
    public HashMap<String, Object> confirmSolution(@DestinationVariable String sessionId,
            @Payload ChooseSolutionRequest request) throws Exception {
       //TODO faire ça
        try {
            Chat chat = service.chooseSolution(request);
         
            HashMap<String, Object> resultat = new HashMap<>();
            resultat.put("status", "CONFIRME_SOLUTION");
            resultat.put("messages", this.convertToDto( chat));
            return resultat;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private ChatDto convertToDto(Chat chat) {
        ChatDto chatDto = modelMapper.map(chat, ChatDto.class);
        if (chat.getMessages() != null && !chat.getMessages().isEmpty()) {
            chatDto.setMessages(chat.getMessages().stream().map(this::convertToDto).collect(Collectors.toList()));
        }

        if (chat.getMembers() != null && !chat.getMembers().isEmpty()) {
            chatDto.setMembers(chat.getMembers().stream().map(this::convertToResponse).collect(Collectors.toList()));
        }

        if (chat.getGuests() != null && !chat.getGuests().isEmpty()) {
            chatDto.setGuests(chat.getGuests().stream().map(this::convertToResponse).collect(Collectors.toList()));
        }

        if (chat.getVote() != null && !chat.getVote().isEmpty()) {
            chatDto.setVote(chat.getVote().stream().map(this::convertToDto).collect(Collectors.toList()));
        }
        return chatDto;
    }

    private UserResponse convertToResponse(User user) {
        UserResponse userResponse = modelMapper.map(user, UserResponse.class);
        return userResponse;
    };

    private MessageDto convertToDto(Message message) {
        MessageDto messageDto = modelMapper.map(message, MessageDto.class);
        if (message.getChat() != null) {
            messageDto.setChatId(message.getChat().getId());
        }
        if (message.getCreatedAt() != null) {
            messageDto.setCreatedAt(Utils.convertLocalDateTimeToStr(message.getCreatedAt()));
        }

        if (message.getLinkedVote() != null) {
            messageDto.setVoteDto(convertToDto(message.getLinkedVote()));
        }
        if (message.isVote()) {
            messageDto.setVote(message.isVote());
        }

        return messageDto;
    }

    private VoteDto convertToDto(Vote vote) {
        VoteDto voteDto = modelMapper.map(vote, VoteDto.class);
        if (vote.getChat() != null) {
            voteDto.setChatId(vote.getChat().getId());
        }

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

    // @MessageMapping("/session/add-guest/{sessionId}")
    // @SendTo("/topic/session/{sessionId}")
    // public SessionJoinResponse addGuest(@DestinationVariable String sessionId,
    // @Payload SessionJoinRequest userResponse) {

    // }

    // @MessageMapping("/message")
    // public NewMessageRequest receiveSessionMessage(@Payload NewMessageRequest
    // message) {
    // System.out.println(simpUserRegistry.getUsers());
    // if(message.getClaimCode() != null){
    // simpleMessaging.convertAndSendToUser(message.getClaimCode(),
    // "/secured/session", message); // /api/v1/session/rec555/private
    // }

    // System.out.println(message);
    // // try {
    // // service.send(message);
    // // } catch (Exception e) {
    // // e.printStackTrace();
    // // }
    // return message;
    // }

}
