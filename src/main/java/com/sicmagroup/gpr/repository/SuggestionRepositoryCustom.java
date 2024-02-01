package com.sicmagroup.gpr.repository;

import java.util.List;

import com.sicmagroup.gpr.api.report.FilterRequest;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.repository.projection.ClaimPerGenderAndAgencePrjt;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerCanalAndSpPro;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerGenderAndAgencePro;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerGenderPro;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerServicePointPro;
import com.sicmagroup.gpr.repository.projection.custom.ObjectPerCanalPro;
import com.sicmagroup.gpr.repository.projection.custom.ObjectPerYear;
import com.sicmagroup.gpr.repository.projection.custom.SuggesPerGenderAndAgencePro;
import com.sicmagroup.gpr.repository.projection.custom.SuggestTotalPerStatusPro;

public interface SuggestionRepositoryCustom {
	long countSuggestByCriterias(FilterRequest request);
	List<ClaimPerServicePointPro> countSuggestByCriteriaAndServicePoint(FilterRequest request);
	List<SuggestTotalPerStatusPro> countSuggestByCriteriaAndStatus(FilterRequest request);
	List<ObjectPerCanalPro> countSuggestByCriteriaAndCanal(FilterRequest request);
	List<ClaimPerCanalAndSpPro> countSuggestByCriteriaAndCanalAndSp(FilterRequest request);
	List<ClaimPerGenderPro> countSuggestByCriteriaAndGender(FilterRequest request);
	List<ClaimPerGenderAndAgencePro> countSuggestByCriteriaAndGenderAndAgence(FilterRequest request);
	List<Suggestion> countSuggestByCriteriaAndPeriode(FilterRequest request);
	List<ObjectPerYear> countSuggestByCriteriaAndYearAndSp(FilterRequest request);
}
