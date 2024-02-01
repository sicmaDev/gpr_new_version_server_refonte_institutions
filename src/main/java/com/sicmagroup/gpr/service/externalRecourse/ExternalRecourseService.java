package com.sicmagroup.gpr.service.externalRecourse;

import java.util.List;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;

import com.sicmagroup.gpr.domain.model.ExternalRecourse;

public interface ExternalRecourseService {
    public List<ExternalRecourse> getAll();

    public List<ExternalRecourse> getAllDeleted();

    public ExternalRecourse getById(Long id) throws Exception ;

    public ExternalRecourse saveExternalRecourse(ExternalRecourse externalRecourse);

    public ExternalRecourse updateExternalRecourse(ExternalRecourse externalRecourse);

    public ExternalRecourse deleteTempExternalRecourse(Long id) throws NotFoundException;

    public void deleteExternalRecourse(ExternalRecourse externalRecourse) throws Exception ;

    public ExternalRecourse getDeletedById(Long id, boolean deleted) throws NotFoundException;
}
