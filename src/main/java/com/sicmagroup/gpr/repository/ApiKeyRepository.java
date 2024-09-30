package com.sicmagroup.gpr.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.ApiKey;
import java.util.List;


public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
   List<ApiKey> findByCle(String key);
}
