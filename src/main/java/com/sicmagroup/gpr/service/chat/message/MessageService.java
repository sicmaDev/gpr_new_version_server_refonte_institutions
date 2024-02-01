package com.sicmagroup.gpr.service.chat.message;

import java.util.List;

import com.sicmagroup.gpr.api.chat.message.ChooseSolutionRequest;
import com.sicmagroup.gpr.api.chat.message.InitVoteRequest;
import com.sicmagroup.gpr.api.chat.message.MessageListRequest;
import com.sicmagroup.gpr.api.chat.message.NewMessageRequest;
import com.sicmagroup.gpr.api.chat.message.SessionJoinRequest;
import com.sicmagroup.gpr.api.chat.message.SessionJoinResponse;
import com.sicmagroup.gpr.api.chat.message.VoteRequest;
import com.sicmagroup.gpr.domain.dto.chat.VoteDto;
import com.sicmagroup.gpr.domain.model.chat.Chat;
import com.sicmagroup.gpr.domain.model.chat.Message;

public interface MessageService {
    
    Message send(NewMessageRequest request) throws Exception;

    List<Message> getList(MessageListRequest request) throws Exception;

    SessionJoinResponse joinSession(SessionJoinRequest request) throws Exception;

    VoteDto initVote(InitVoteRequest request) throws Exception;

    List<Message> vote(VoteRequest request) throws Exception;

    Chat chooseSolution(ChooseSolutionRequest request) throws Exception;
}
