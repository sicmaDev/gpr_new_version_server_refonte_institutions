package com.sicmagroup.gpr.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.projection.ObjectPerCanalProjection;
import com.sicmagroup.gpr.repository.projection.ObjectPerObjProjction;
import com.sicmagroup.gpr.repository.projection.ClaimPerCanalPerSpPjt;
import com.sicmagroup.gpr.repository.projection.ClaimPerGenderAndAgencePrjt;
import com.sicmagroup.gpr.repository.projection.ClaimPerGenderPjt;
import com.sicmagroup.gpr.repository.projection.ClaimPerObjLevelAndAgenceProjection;
import com.sicmagroup.gpr.repository.projection.ClaimPerObjLevelProjection;
import com.sicmagroup.gpr.repository.projection.ClaimPerServicePointProjection;
import com.sicmagroup.gpr.repository.projection.ClaimPerStatusSatisfactionProjection;
import com.sicmagroup.gpr.repository.projection.ObjectTotalPerStatusProjection;

import java.time.LocalDateTime;
import java.util.List;

import com.sicmagroup.gpr.domain.dto.reports.ClaimPerServicePoint;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.SolutionStatus;

public interface ClaimRepository extends JpaRepository<Claim, Long>, ClaimRepositoryCustom {

        Optional<Claim> findByCode(String code);

        List<Claim> findByTypeAndCodeStartsWith(ClaimType type,String code);

        List<Claim> findByTelAndTypeAndStatus(String tel, ClaimType type, ClaimStatus status);

        Optional<Claim> findByTypeAndCode(ClaimType type, String code);

        List<Claim> findByStatus(ClaimStatus status);

        List<Claim> findByType(ClaimType type);

        List<Claim> findByTypeAndServicePointIn(ClaimType type,List<ServicePoint> servicePoints);
        
        List<Claim> findByServicePointIn(List<ServicePoint> servicePoints);

        List<Claim> findByTypeAndStatus(ClaimType type, ClaimStatus status);

        List<Claim> findByTypeAndStatusNot(ClaimType type, ClaimStatus status);
        List<Claim> findByTypeAndStatusNotAndReceiptDateTimeBetween(ClaimType type, ClaimStatus status, LocalDateTime start, LocalDateTime end);
        List<Claim> findByTypeAndStatusInAndReceiptDateTimeBetween(ClaimType type, List<ClaimStatus> status, LocalDateTime start, LocalDateTime end);

        List<Claim> findByTypeAndStatusAndCollector(ClaimType type, ClaimStatus status, User collector);

        List<Claim> findByTypeAndStatusIn(ClaimType type, List<ClaimStatus> statusList);


        List<Claim> findByTypeAndCollectorAndStatusOrTypeAndTreatmentAffectedToAndStatusIn(ClaimType type,
                        User collector,
                        ClaimStatus status, ClaimType type2, User affectedTo, List<ClaimStatus> statusList);

        @Query("SELECT c, s FROM Claim c " +
                        "LEFT JOIN Solution s ON c.id = s.claim.id " +
                        "WHERE c.type = :type AND c.status IN :statuses " +
                        "AND s.id IN (SELECT MAX(s2.id) FROM Solution s2 where s2.claim.id = c.id AND s2.status = :solutionStatus)")
        List<Claim> findClaimsWithLatestSolutionByTypeAndStatusIn(@Param("type") ClaimType type,
                        @Param("statuses") List<ClaimStatus> statuses,
                        @Param("solutionStatus") SolutionStatus solutionStatus);

        @Query("SELECT DISTINCT c.code FROM Claim c " +
                        "LEFT JOIN Solution s ON c.id = s.claim.id " +
                        "WHERE c.type = :type AND (c.status = :status OR (c.treatmentAffectedTo = :affectedTo AND c.status IN :statuses)) ")
        List<Claim> findClaimsWithLatestSolutionByTypeAndStatusOrTypeAndTreatmentAffectedToAndStatusIn(
                        @Param("type") ClaimType type, @Param("status") ClaimStatus status,
                        @Param("affectedTo") User affectedTo,
                        @Param("statuses") List<ClaimStatus> statuses);

        @Query("SELECT c, s FROM Claim c " +
                        "LEFT JOIN FETCH c.solutions s " +
                        "WHERE c.type = :type AND (c.status = :status OR (c.treatmentAffectedTo = :affectedTo AND c.status IN :statuses)) "
                        +
                        " ORDER BY c.id DESC, s.id DESC"

        )
        List<Claim> findClaimsBySolutionByTypeAndStatusOrTreatmentAffectedToAndStatusIn(
                        @Param("type") ClaimType type, @Param("status") ClaimStatus status,
                        @Param("affectedTo") User affectedTo,
                        @Param("statuses") List<ClaimStatus> statuses);

        @Query("SELECT DISTINCT c, s FROM Claim c " +
                        "LEFT JOIN FETCH c.solutions s " +
                        "WHERE c.type = :type AND c.status IN :statuses "
                        +
                        "ORDER BY c.id DESC, s.id DESC"

        )
        List<Claim> findClaimsBySolutionDescAndByTypeAndStatusIn(
                        @Param("type") ClaimType type,
                        @Param("statuses") List<ClaimStatus> statuses);

        @Query("SELECT c FROM Claim c WHERE type = :type AND status NOT IN :statuses")
        List<Claim> findByTypeAndStatusNotIn(@Param("type") ClaimType type,
                        @Param("statuses") List<ClaimStatus> statuses);

        Long countByTypeAndStatusNot(ClaimType type, ClaimStatus status);

        Long countByTypeAndStatusIn(ClaimType type, List<ClaimStatus> allStatus);

        @Query("SELECT sp.id as servicePointId, sp.libelle as libelle, COUNT(c.code) as total " +
                        "FROM ServicePoint sp, Claim c " +
                        "WHERE c.servicePoint.id = sp.id AND c.type = :type AND c.status != 'TEMP_SAVED' GROUP BY sp.id, sp.libelle")
        List<ClaimPerServicePointProjection> countClaimPerServicePoint(@Param("type") ClaimType type);

        @Query("SELECT c.status as status, COUNT(c.code) as total FROM Claim c WHERE c.status != 'TEMP_SAVED' AND c.type = :type GROUP BY c.status  ")
        List<ObjectTotalPerStatusProjection> countClaimPerStatus(@Param("type") ClaimType type);

        @Query("SELECT cl.id as id, cl.libelle as libelle, COUNT(c.code) as total FROM Claim c RIGHT JOIN CollectionChannel cl ON c.collectionChannel.id = cl.id WHERE " +
                "c.type = :type AND c.status != 'TEMP_SAVED' GROUP BY cl.id, cl.libelle"
        )
        List<ObjectPerCanalProjection> countClaimPerCanal(@Param("type") ClaimType type);


        @Query("SELECT cl.id as canalId, sp.id as spId,  cl.libelle as canalLibelle, sp.libelle as spLibelle, COUNT(c.code) as total" +
        " FROM Claim c, CollectionChannel cl, ServicePoint sp  WHERE c.collectionChannel.id = cl.id AND c.servicePoint.id = sp.id AND c.type = :type AND c.status != 'TEMP_SAVED' GROUP BY cl.id, sp.id, cl.libelle, sp.libelle")
        List<ClaimPerCanalPerSpPjt> countClaimPerCanalAndAgence(@Param("type") ClaimType type);

        @Query("SELECT o.id as id, COUNT(c.code) as total, o.libelle as libelle FROM Claim c LEFT JOIN Objet o ON c.objet.id = o.id WHERE c.type = :type AND c.status != 'TEMP_SAVED' GROUP BY o.id, o.libelle")
        List<ObjectPerCanalProjection> countClaimPerObjet(@Param("type") ClaimType type);

        @Query("SELECT o.id as id, COUNT(c.code) as total, o.libelle as libelle FROM Claim c LEFT JOIN Objet o ON c.objet.id = o.id WHERE c.status != 'TEMP_SAVED' GROUP BY o.id, o.libelle")
        List<ObjectPerCanalProjection> countObjPerObjet();

        @Query("SELECT o.id as idObj, COUNT(c.code) as total, o.libelle as libelleObj, sp.libelle as libelleSp, sp.id as idSp FROM Claim c LEFT JOIN Objet o ON c.objet.id = o.id LEFT JOIN ServicePoint sp ON c.servicePoint.id = sp.id WHERE c.status != 'TEMP_SAVED' GROUP BY o.id, sp.id, o.libelle, sp.libelle ")
        List<ObjectPerObjProjction> countObjtPerObjetPerAgence();

         @Query("SELECT o.id as idObj, COUNT(c.code) as total, o.libelle as libelleObj, sp.libelle as libelleSp, sp.id as idSp FROM Claim c LEFT JOIN Objet o ON c.objet.id = o.id LEFT JOIN ServicePoint sp ON c.servicePoint.id = sp.id WHERE c.type = :type AND c.status != 'TEMP_SAVED' GROUP BY o.id, sp.id, o.libelle, sp.libelle ")
        List<ObjectPerObjProjction> countClaimPerObjetPerAgence(@Param("type") ClaimType type);

        @Query("SELECT COUNT(c.code) as total, c.gender as gender FROM Claim c WHERE c.status != 'TEMP_SAVED' AND c.type= :type GROUP BY gender, c.gender")
        List<ClaimPerGenderPjt> countClaimPerGender(@Param("type") ClaimType type);

        @Query("SELECT sp.id as id, sp.libelle as libelle, COUNT(c.code) as total, c.gender as gender FROM Claim c LEFT JOIN ServicePoint sp ON c.servicePoint.id = sp.id WHERE c.status != 'TEMP_SAVED' AND c.type= 'CLAIM' GROUP BY gender, id, sp.libelle")
        List<ClaimPerGenderAndAgencePrjt> countClaimPerGenderAndAgence();

        @Query("SELECT o.id as objtId, o.libelle as objLibelle, o.risqueLevel as objNiveau, COUNT(c.code) as total FROM Claim c LEFT JOIN Objet o ON c.objet.id = o.id WHERE c.type = :type AND c.status != 'TEMP_SAVED' GROUP BY objtId, objLibelle, objNiveau ")
        List<ClaimPerObjLevelProjection> countClaimPerObjLevel(@Param("type") ClaimType type);

        @Query("SELECT sp.id as spId, sp.libelle as spLib, o.id as objtId, o.libelle as objLib, o.risqueLevel as objNiveau, COUNT(c.code) as total FROM Claim c LEFT JOIN Objet o ON c.objet.id = o.id LEFT JOIN ServicePoint sp ON c.servicePoint.id = sp.id WHERE c.type = :type AND c.status != 'TEMP_SAVED' GROUP BY sp.id, spLib, objtId, sp.libelle, o.id, o.libelle, objNiveau")
        List<ClaimPerObjLevelAndAgenceProjection> countClaimPerObjLevelAndAgence(@Param("type") ClaimType type);

        // @Query("SELECT c.status as status, COUNT(c.code) as total FROM Claim c WHERE c.type = 'CLAIM' AND c.status IN ('SATISFIED', 'UNSATISFIED', 'PARTIAL_SATISFIED','CLASSED','LITIGATION') GROUP BY status")
        // List<ClaimPerStatusSatisfactionProjection> countClaimPerSatisfaction(); 

        @Query("SELECT " +
        "CASE " +
        "WHEN c.status IN ('CLASSED', 'LITIGATION') THEN 'UNSATISFIED' " +
        "ELSE c.status END as status, " +
        "COUNT(c.code) as total " +
        "FROM Claim c " +
        "WHERE c.type = 'CLAIM' " +
        "AND c.status IN ('SATISFIED', 'UNSATISFIED', 'PARTIAL_SATISFIED', 'CLASSED', 'LITIGATION') " +
        "GROUP BY CASE " +
        "WHEN c.status IN ('CLASSED', 'LITIGATION') THEN 'UNSATISFIED' " +
        "ELSE c.status END")
        List<ClaimPerStatusSatisfactionProjection> countClaimPerSatisfaction();


        Long countByTypeAndStatusNotAndReceiptDateTimeBetween(ClaimType type, ClaimStatus status, LocalDateTime start, LocalDateTime end);

        // Long countByTypeAndStatusNotAndObjetIn(ClaimType type, ClaimStatus status, );

         @Query("SELECT o.id as objtId, o.libelle as objLibelle, o.risqueLevel as objNiveau, COUNT(c.code) as total FROM Claim c LEFT JOIN Objet o ON c.objet.id = o.id WHERE c.type = :type AND c.status = 'SAVED' GROUP BY objtId, objLibelle, objNiveau ")
        List<ClaimPerObjLevelProjection> countClaimSavedPerObjLevel(@Param("type") ClaimType type);



}