package com.sicmagroup.gpr.service.existingSolution;

import java.util.List;

import com.sicmagroup.gpr.api.config.existingSolution.AddExistingSolutionRequest;
import com.sicmagroup.gpr.api.config.existingSolution.UpdateExistingSolutionRequest;
import com.sicmagroup.gpr.domain.model.ExistingSolution;

public interface ExistingSolutionService {

    List<ExistingSolution> getAll();

    ExistingSolution saveOne(AddExistingSolutionRequest request) throws Exception;

    ExistingSolution updateOne(UpdateExistingSolutionRequest request) throws Exception;

    void removeOne(Long id) throws Exception;

    List<ExistingSolution> getAllByObjet(Long id) throws Exception;
}
