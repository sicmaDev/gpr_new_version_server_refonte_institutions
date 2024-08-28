package com.sicmagroup.gpr.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.domain.model.chat.Chat;
import com.sicmagroup.gpr.domain.enumeration.Role;


// you need to import the entity class arccording to your context 
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndIsDeleted(String email,boolean isDeleted);

    Optional<User> findByCode(String code);

    List<User> findByIsDeleted(boolean deleted);

    Optional<User> findByIdAndIsDeleted(Long id, boolean deleted);

    List<User> findByAdditionalroleIn(List<Role> roles);

    List<User> findByIsEmailReceiver(boolean isEmailReceiver);
    Optional<User> findByIsEmailReceiverAndIsRaAndServicePoint(boolean isEmailReceiver, boolean isRa, ServicePoint servicePoint);

    List<User> findByChatsMemberIn(List<Chat> chats);
    List<User> findByChatsGuestIn(List<Chat> chats);

}