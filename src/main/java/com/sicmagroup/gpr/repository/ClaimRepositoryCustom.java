package com.sicmagroup.gpr.repository;

import java.util.List;

import com.sicmagroup.gpr.api.report.FilterRequest;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.repository.projection.ClaimPerServicePointProjection;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerCanalAndSpPro;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerGenderAndAgencePro;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerGenderPro;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerObjLevelAndSpPro;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerObjLevelPro;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerServicePointPro;
import com.sicmagroup.gpr.repository.projection.custom.ObjectPerCanalPro;
import com.sicmagroup.gpr.repository.projection.custom.ObjectPerObjPro;
import com.sicmagroup.gpr.repository.projection.custom.ObjectPerYear;
import com.sicmagroup.gpr.repository.projection.custom.ObjectTotalPerStatusPro;

public interface ClaimRepositoryCustom {
    long countClaimByCriterias(FilterRequest request, ClaimType type);

    List<ClaimPerServicePointPro> countClaimByCriteriaAndByServicePoint(FilterRequest request, ClaimType type);

    List<ObjectTotalPerStatusPro> countClaimByCriteriaAndStatus(FilterRequest request, ClaimType type);

    List<ObjectPerCanalPro> countClaimByCriteriaAndCanal(FilterRequest request, ClaimType type);

    List<ClaimPerCanalAndSpPro> countClaimByCriteriaAndCanalAndSp(FilterRequest request, ClaimType type);

    List<ObjectPerCanalPro> countClaimByCriteriaAndObjet(FilterRequest request, ClaimType type);

    List<ObjectPerCanalPro> countObjectByCriteriaAndObjet(FilterRequest request);

    List<ObjectPerObjPro> countObjByCriteriaAndByObjetAndAgence(FilterRequest request);

    List<ObjectPerObjPro> countClaimByCriteriaAndByObjetAndAgence(FilterRequest request, ClaimType type);

    List<ClaimPerGenderPro> countClaimByCriteriaAndByGender(FilterRequest request);

    List<ClaimPerGenderAndAgencePro> countClaimByCriteriaAndByGenderAndAgence(FilterRequest request);

    List<ClaimPerObjLevelPro> countClaimByCriteriaAndByObjLevel(FilterRequest request, ClaimType type);

    List<ClaimPerObjLevelAndSpPro> countClaimByCriteriaAndByObjLevelAndSp(FilterRequest request, ClaimType type);

    List<ObjectTotalPerStatusPro> countClaimByCriteriaAndSatisfaction(FilterRequest request);

    List<Claim> findClaimByCriteriaAndTypeAndStatus(FilterRequest request, List<ClaimStatus> status);

    List<Claim> countClaimByCriteriaAndTypeAndStatusNotAndReceiptDateTimeBetween(FilterRequest request, ClaimType type);

    List<Claim> countClaimByCriteriaAndTypeAndStatusIn(FilterRequest request, ClaimType type, List<ClaimStatus> status);

    List<ObjectPerYear> countClaimByCriteriaAndYearAndSp(FilterRequest request, ClaimType type);

    List<Claim> countClaimByCriteriaAndStatusNot(FilterRequest request, ClaimType type);

        List<Claim> countClaimByCriteriaAndStatusIn(FilterRequest request, ClaimType type);

}
