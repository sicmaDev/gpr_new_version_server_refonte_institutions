package com.sicmagroup.gpr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.InboxMessage;
import com.sicmagroup.gpr.domain.model.Inbox;

public interface InboxMessageRepository extends JpaRepository<InboxMessage, Long> {

    List<InboxMessage> findByChatId(String chatId);

    List<InboxMessage> findByInbox(Inbox inbox);
    List<InboxMessage> findByInboxId(Long inbox);

}
