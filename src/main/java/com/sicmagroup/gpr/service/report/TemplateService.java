package com.sicmagroup.gpr.service.report;

import java.util.List;

import com.sicmagroup.gpr.domain.model.Template;

public interface TemplateService {
    public Template createOrUpdate(Template template);
    public List<Template> findAll();
    public Template findById(Long id);
    public void delete(Long id);
}
