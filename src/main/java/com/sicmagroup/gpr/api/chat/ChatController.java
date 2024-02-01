package com.sicmagroup.gpr.api.chat;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.api.chat.message.SessionJoinRequest;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.dto.chat.ChatDto;
import com.sicmagroup.gpr.domain.dto.chat.MessageDto;
import com.sicmagroup.gpr.domain.dto.chat.UserVoteDto;
import com.sicmagroup.gpr.domain.dto.chat.VoteDto;
import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.domain.model.chat.Chat;
import com.sicmagroup.gpr.domain.model.chat.Message;
import com.sicmagroup.gpr.domain.model.chat.UserVote;
import com.sicmagroup.gpr.domain.model.chat.Vote;
import com.sicmagroup.gpr.service.chat.ChatServiceImpl;
import com.sicmagroup.gpr.utils.Utils;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;


@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatServiceImpl serviceImpl;
     private final ModelMapper modelMapper;

    @PostMapping("/init")
    public  ResponseEntity<ApiResponseDto> initSession(@RequestBody ChatInitRequest request) {
        ApiResponseDto apiResponseDto = new ApiResponseDto();
        try {
            Chat chat = serviceImpl.init(request);
            apiResponseDto.setStatus(true);
            apiResponseDto.setContent(this.convertToDto(chat));
            return ResponseEntity.ok(apiResponseDto);
        } catch (Exception e) {
             apiResponseDto.setStatus(false);
            apiResponseDto.setContent(ErrorResponse.builder().title("Something goes wrong").message(e.getMessage()).build());
            return ResponseEntity.ok(apiResponseDto);
        }
    }

    @PutMapping("/reinit")
    public ResponseEntity<ApiResponseDto> reStartSession(@RequestBody ChatInitRequest request) {
        ApiResponseDto apiResponseDto = new ApiResponseDto();
        try {
            Chat chat = serviceImpl.reInit(request);
            apiResponseDto.setStatus(true);
            apiResponseDto.setContent(this.convertToDto(chat));
            return ResponseEntity.ok(apiResponseDto);
        } catch (Exception e) {
             apiResponseDto.setStatus(false);
            apiResponseDto.setContent(ErrorResponse.builder().title("Something goes wrong").message(e.getMessage()).build());
            return ResponseEntity.ok(apiResponseDto);
        }
    }

    // @PutMapping("/invite_guest")
    // public ResponseEntity<ApiResponseDto>  inviteGuest(@RequestBody InviteGuestRequest request) {
    //     ApiResponseDto apiResponseDto = new ApiResponseDto();
        
    //     try {
    //         Chat chat = serviceImpl.inviteGuest(request);
    //         apiResponseDto.setStatus(true);
    //         apiResponseDto.setContent(this.convertToDto(chat));
    //         return ResponseEntity.ok(apiResponseDto);
    //     } catch (Exception e) {
    //          apiResponseDto.setStatus(false);
    //         apiResponseDto.setContent(ErrorResponse.builder().title("Something goes wrong").message(e.getMessage()).build());
    //         return ResponseEntity.ok(apiResponseDto);
    //     }
    // }

    // @PutMapping("/eject_guest")
    // public ResponseEntity<ApiResponseDto> ejectGuest(@RequestBody EjectGuestRequest request) {
    //      ApiResponseDto apiResponseDto = new ApiResponseDto();
    //       try {
    //         Chat chat = serviceImpl.ejectGuest(request);
    //         apiResponseDto.setStatus(true);
    //         apiResponseDto.setContent(this.convertToDto(chat));
    //         return ResponseEntity.ok(apiResponseDto);
    //     } catch (Exception e) {
    //          apiResponseDto.setStatus(false);
    //         apiResponseDto.setContent(ErrorResponse.builder().title("Something goes wrong").message(e.getMessage()).build());
    //         return ResponseEntity.ok(apiResponseDto);
    //     }
    // }

    @PutMapping("/join")
    public ResponseEntity<ApiResponseDto>  joinSession( @RequestBody SessionJoinRequest request) {
          ApiResponseDto apiResponseDto = new ApiResponseDto();
          try {
            Chat chat = serviceImpl.joinChat(request);
            apiResponseDto.setStatus(true);
            apiResponseDto.setContent(this.convertToDto(chat));
            return ResponseEntity.ok(apiResponseDto);
        } catch (Exception e) {
             apiResponseDto.setStatus(false);
            apiResponseDto.setContent(ErrorResponse.builder().title("Something goes wrong").message(e.getMessage()).build());
            return ResponseEntity.ok(apiResponseDto);
        }
    }

    private ChatDto convertToDto(Chat chat) {
        ChatDto chatDto = modelMapper.map(chat, ChatDto.class);
        if(chat.getMessages() != null && !chat.getMessages().isEmpty()){
            chatDto.setMessages(chat.getMessages().stream().map(this::convertToDto).collect(Collectors.toList()));
        } 

        if(chat.getMembers() != null && !chat.getMembers().isEmpty()){
            chatDto.setMembers(chat.getMembers().stream().map(this::convertToResponse).collect(Collectors.toList()));
        }

        if(chat.getGuests() != null && !chat.getGuests().isEmpty()){
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
        if(message.getChat() != null){
            messageDto.setChatId(message.getChat().getId());
        }
         if(message.getCreatedAt() != null){
            messageDto.setCreatedAt(Utils.convertLocalDateTimeToStr(message.getCreatedAt()));
        }
        if(message.isVote()){
            messageDto.setVote(message.isVote());
        }
        if(message.getLinkedVote() != null){
            messageDto.setVoteDto(convertToDto(message.getLinkedVote()));
        }
        return messageDto;
    }

     private VoteDto convertToDto(Vote vote) {
        VoteDto voteDto = modelMapper.map(vote, VoteDto.class);
        
        if(vote.getChat() != null){
            voteDto.setChatId(vote.getChat().getId());
        }
        if(vote.getUserVotes() != null && !vote.getUserVotes().isEmpty()){
            voteDto.setUserVote(vote.getUserVotes().stream().map(this::convertToDto).collect(Collectors.toList()));
        }

        if(vote.getMessage() != null){
            voteDto.setMessageId(vote.getMessage().getId());
        }
        return voteDto;
    }
       private UserVoteDto convertToDto(UserVote userVote) {
        UserVoteDto userVoteDto = modelMapper.map(userVote, UserVoteDto.class);
        userVoteDto.setAuthor(this.convertToResponse(userVote.getUser()));
        return userVoteDto;
    }

}
