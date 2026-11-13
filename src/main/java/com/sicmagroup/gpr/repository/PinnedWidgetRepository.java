package com.sicmagroup.gpr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sicmagroup.gpr.domain.model.PinnedWidget;

@Repository
public interface PinnedWidgetRepository extends JpaRepository<PinnedWidget, String> {

    List<PinnedWidget> findByUserIdOrderByDisplayOrderAsc(String userId);

    Optional<PinnedWidget> findByIdAndUserId(String id, String userId);

    void deleteByIdAndUserId(String id, String userId);

    void deleteByUserId(String userId);
}
