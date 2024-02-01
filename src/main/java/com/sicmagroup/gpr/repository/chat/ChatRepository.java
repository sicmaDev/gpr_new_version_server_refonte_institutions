package com.sicmagroup.gpr.repository.chat;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.chat.Chat;
import com.sicmagroup.gpr.domain.model.User;


public interface ChatRepository extends JpaRepository<Chat, Long> {
    Optional<Chat> findByClaim(Claim claim);

    List<Chat> findByGuestsIn(List<User> guests);

    

    
}
