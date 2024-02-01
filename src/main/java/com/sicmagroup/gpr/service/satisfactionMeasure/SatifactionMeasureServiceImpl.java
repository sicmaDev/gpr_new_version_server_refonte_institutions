package com.sicmagroup.gpr.service.satisfactionMeasure;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.model.SatisfactionMeasure;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.repository.SatisfactionMeasureRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SatifactionMeasureServiceImpl implements SatisfactionMeasureService{
    
    private final SatisfactionMeasureRepository repository;

    public SatisfactionMeasure saveSatisfactionMeasure(SatisfactionMeasure satisfactionMeasure){
        return repository.save(satisfactionMeasure);
    }

    @Override
    public SatisfactionMeasure getBytId(Long id) throws Exception {
        return repository.findById(id).orElseThrow(() -> new Exception("Mesure de satisfaction introuvable"));
    }

    @Override
    public SatisfactionMeasure getBySolution(Solution solution) throws Exception {
        return repository.findBySolution(solution).orElseThrow(() -> new Exception("Aucune mesure de satisfaction trouvée pour cette solution"));  
    }
}
