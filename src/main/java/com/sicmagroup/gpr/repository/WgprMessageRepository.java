package com.sicmagroup.gpr.repository;

import com.sicmagroup.gpr.domain.model.WgprMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WgprMessageRepository extends JpaRepository<WgprMessage, Long> {

    List<WgprMessage> findByFromNumber(String fromNumber);

    long countByRead(boolean read);

    List<WgprMessage> findTop50ByOrderByTimestampDesc();

    List<WgprMessage> findByStatusOrderByTimestampDesc(String status);

    @Query("SELECT m FROM WgprMessage m WHERE m.status IS NULL OR m.status <> :status ORDER BY m.timestamp DESC")
    List<WgprMessage> findNonConverted(@Param("status") String status);

    @Query("SELECT COUNT(m) FROM WgprMessage m WHERE m.read = false")
    long countUnread();
}