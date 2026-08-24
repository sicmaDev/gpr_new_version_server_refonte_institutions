package com.sicmagroup.gpr.service.extra;

import java.util.List;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;

import com.sicmagroup.gpr.api.extra.ExtraRequest;
import com.sicmagroup.gpr.domain.model.ExtraContent;
import com.sicmagroup.gpr.domain.model.User;

public interface ExtraContentService {


    public List<ExtraContent> getAll(ExtraContent extraContent);

    public ExtraContent getById(Long id) throws NotFoundException;

    public ExtraContent saveExtraContent(ExtraContent extraContent) throws Exception;

    public void deleteExtraContent(Long id, User connectedUser) throws Exception;
}
