package com.sicmagroup.gpr.repository.chat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.chat.Chat;
import com.sicmagroup.gpr.domain.model.chat.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
    
    List<Message> findByChat(Chat chat);
}
