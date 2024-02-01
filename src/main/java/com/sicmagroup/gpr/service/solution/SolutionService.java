package com.sicmagroup.gpr.service.solution;

import java.util.List;

import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Solution;

public interface SolutionService {
    
    public Solution saveSolution(Solution solution);

    public Solution getById(Long id) throws Exception;

    public List<Solution> getAllByClaim(Claim claim);

}
