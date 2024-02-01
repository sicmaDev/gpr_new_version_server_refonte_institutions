package com.sicmagroup.gpr.service.chat;

import java.util.List;

import com.sicmagroup.gpr.api.chat.ChatInitRequest;
import com.sicmagroup.gpr.api.chat.EjectGuestRequest;
import com.sicmagroup.gpr.api.chat.InviteGuestRequest;
import com.sicmagroup.gpr.api.chat.message.SessionJoinRequest;
import com.sicmagroup.gpr.api.chat.message.SessionJoinResponse;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.domain.model.chat.Chat;

public interface ChatService {
    
    Chat init(ChatInitRequest request) throws Exception;

    Chat reInit(ChatInitRequest request) throws Exception;

    SessionJoinResponse inviteGuest(SessionJoinRequest request) throws Exception;

    SessionJoinResponse ejectGuest(SessionJoinRequest request) throws Exception;

    List<Chat> getChatByGuest(List<User> guestList); 

    Chat joinChat(SessionJoinRequest request) throws Exception;
}
