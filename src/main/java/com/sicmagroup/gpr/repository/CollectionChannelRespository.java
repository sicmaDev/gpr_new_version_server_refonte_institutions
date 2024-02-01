package com.sicmagroup.gpr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.CollectionChannel;

public interface CollectionChannelRespository extends JpaRepository<CollectionChannel, Long> {
    List<CollectionChannel> findByIsDeleted(boolean deleted);

    Optional<CollectionChannel> findByIdAndIsDeleted(Long id, boolean deleted);
    
}
