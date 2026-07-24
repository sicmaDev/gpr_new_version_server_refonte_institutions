package com.sicmagroup.gpr.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sicmagroup.gpr.domain.model.Poste;
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
    List<User> findByAdditionalroleInAndIsDeleted(List<Role> roles, boolean isDeleted);

    List<User> findByIsEmailReceiver(boolean isEmailReceiver);
    List<User> findByIsEmailReceiverAndIsDeleted(boolean isEmailReceiver, boolean isDeleted);
    Optional<User> findByIsEmailReceiverAndIsRaAndServicePoint(boolean isEmailReceiver, boolean isRa, ServicePoint servicePoint);

    List<User> findByPosteAndIsDeleted(Poste poste,boolean isDeleted);
    
    List<User> findByChatsMemberIn(List<Chat> chats);
    List<User> findByChatsGuestIn(List<Chat> chats);

    Optional<User> findByServicePointAndIsRaTrue(ServicePoint servicePoint);

    List<User> findByServicePoint(ServicePoint servicePoint);
    @Query("SELECT u FROM User u WHERE u.servicePoint.id = :servicePointId AND u.isRa = true AND u.isDeleted = false")
    Optional<User> findRaByServicePointId(@Param("servicePointId") Long servicePointId);
}