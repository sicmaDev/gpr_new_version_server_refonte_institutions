package com.sicmagroup.gpr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sicmagroup.gpr.domain.model.UserInstruction;

@Repository
public interface UserInstructionRepository extends JpaRepository<UserInstruction, String> {

    List<UserInstruction> findByUserIdOrderByCreatedAtDesc(String userId);

    List<UserInstruction> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(String userId);

    long countByUserIdAndIsActiveTrue(String userId);

    Optional<UserInstruction> findByIdAndUserId(String id, String userId);

    void deleteByIdAndUserId(String id, String userId);
}
