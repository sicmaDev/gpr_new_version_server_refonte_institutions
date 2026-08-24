package com.sicmagroup.gpr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sicmagroup.gpr.domain.model.InboxMessage;
import com.sicmagroup.gpr.domain.model.Inbox;

public interface InboxMessageRepository extends JpaRepository<InboxMessage, Long> {

    List<InboxMessage> findByChatId(String chatId);

    List<InboxMessage> findByInbox(Inbox inbox);
    List<InboxMessage> findByInboxId(Long inbox);

    // Le nom du champ Java (message_id) contient déjà un underscore, ce qui rend
    // findByMessage_id() ambigu pour le dériveur de requêtes Spring Data (confusion
    // possible avec une navigation de propriété imbriquée) - JPQL explicite à la place.
    @Query("SELECT m FROM InboxMessage m WHERE m.message_id = :messageId")
    Optional<InboxMessage> findByMessageIdExact(@Param("messageId") String messageId);

    @Query("SELECT m FROM InboxMessage m WHERE m.connectedNumber = :connectedNumber ORDER BY m.createdAt DESC")
    List<InboxMessage> findByConnectedNumberOrderByCreatedAtDesc(@Param("connectedNumber") String connectedNumber);

}
