package com.sicmagroup.gpr.service.satisfactionMeasure;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.model.SatisfactionMeasure;
import com.sicmagroup.gpr.domain.model.Solution;

import lombok.RequiredArgsConstructor;


public interface SatisfactionMeasureService {
    
    public SatisfactionMeasure saveSatisfactionMeasure(SatisfactionMeasure satisfactionMeasure);

    public SatisfactionMeasure getBytId(Long id) throws Exception;

    public SatisfactionMeasure getBySolution(Solution solution) throws Exception;
}
