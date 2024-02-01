package com.sicmagroup.gpr.service.solution;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.repository.SolutionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SolutionServiceImpl implements SolutionService {

    private final SolutionRepository repository;
    
    @Override
    public Solution saveSolution(Solution solution) {
        return repository.save(solution);
    }

    @Override
    public Solution getById(Long id) throws Exception {
       return repository.findById(id).orElseThrow(() -> new Exception("Solution introuvable"));
    }

    @Override
    public List<Solution> getAllByClaim(Claim claim) {
        return repository.findByClaim(claim);
    }
    
}
