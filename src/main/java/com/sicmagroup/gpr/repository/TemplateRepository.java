package com.sicmagroup.gpr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.sicmagroup.gpr.domain.model.Template;

public interface TemplateRepository extends JpaRepository<Template, Long> {
}
