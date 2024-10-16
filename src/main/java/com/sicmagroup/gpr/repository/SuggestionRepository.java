package com.sicmagroup.gpr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.projection.ClaimPerCanalPerSpPjt;
import com.sicmagroup.gpr.repository.projection.ClaimPerGenderAndAgencePrjt;
import com.sicmagroup.gpr.repository.projection.ClaimPerGenderPjt;
import com.sicmagroup.gpr.repository.projection.ObjectPerCanalProjection;
import com.sicmagroup.gpr.repository.projection.ObjectTotalPerStatusProjection;
import com.sicmagroup.gpr.repository.projection.SuggestPerServicePointProjection;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;

public interface SuggestionRepository extends JpaRepository<Suggestion, Long>, SuggestionRepositoryCustom {

    List<Suggestion> findByStatus(ClaimStatus status);

    List<Suggestion> findByStatusIn(List<ClaimStatus> statuses);

    List<Suggestion> findByStatusNot(ClaimStatus status);
    List<Suggestion> findByServiceIndexeInAndStatusNot(List<ServicePoint> servicePoint, ClaimStatus status);
    List<Suggestion> findByStatusNotAndReceiptDateTimeBetween(ClaimStatus status, LocalDateTime start, LocalDateTime end);

    Optional<Suggestion> findByCode(String code);

    List<Suggestion> findByCodeStartsWith(String code);

    List<Suggestion> findByCollecteurAndStatus(User collecteur, ClaimStatus status);

    List<Suggestion> findByCollecteurAndStatusOrCodeStartsWithAndStatus(User collecteur, ClaimStatus status,String start,ClaimStatus status2);

    Long countByStatusNot(ClaimStatus status);

    @Query("SELECT sp.id as serviceIndexeId, " +
            "CASE " +
            "WHEN sp.id IS NULL THEN 'Non défini' " +
            "ELSE sp.libelle " +
            "END AS libelle," +
            " COUNT(s.code) as total " +
            "FROM Suggestion s LEFT JOIN ServicePoint sp ON s.serviceIndexe.id = sp.id " +
            "WHERE s.status != 'TEMP_SAVED' GROUP BY sp.id, sp.libelle, COALESCE(sp.id, 'null')")
    List<SuggestPerServicePointProjection> countSuggestPerServicePoint();

    @Query("SELECT s.status as status, s.accepted as accepted, COUNT(s.code) as total FROM Suggestion s WHERE s.status != 'TEMP_SAVED' GROUP BY s.status, s.accepted  ")
    List<ObjectTotalPerStatusProjection> countSuggestPerStatus();

    @Query("SELECT cl.id as id, cl.libelle as libelle, COUNT(s.code) as total FROM Suggestion s RIGHT JOIN CollectionChannel cl ON s.canal.id = cl.id "
            +
            "WHERE s.status != 'TEMP_SAVED' GROUP BY cl.id, cl.libelle")
    List<ObjectPerCanalProjection> countSuggestPerCanal();

    @Query("SELECT cl.id as canalId, cl.libelle as canalLibelle, " +
            " CASE WHEN sp.id IS NULL THEN 0 ELSE sp.id END as spId, CASE WHEN sp.id IS NULL THEN 'Non défini' ELSE sp.libelle END as spLibelle, "
            +
            " COUNT(s.code) as total FROM Suggestion s LEFT JOIN CollectionChannel cl ON s.canal.id = cl.id LEFT JOIN ServicePoint sp ON s.serviceIndexe.id = sp.id  "
            +
            "WHERE  s.status != 'TEMP_SAVED' GROUP BY cl.id, canalId, cl.libelle, spLibelle, spId,  COALESCE(sp.id, 'null')")
    List<ClaimPerCanalPerSpPjt> countSuggestPerCanalAndAgence();

    @Query("SELECT  COUNT(s.code) as total, CASE WHEN s.gender IS NULL THEN 'NON_DEFINI' ELSE s.gender END as gender FROM Suggestion s WHERE s.status != 'TEMP_SAVED' GROUP BY gender, COALESCE(gender, 'null')")
    List<ClaimPerGenderPjt> countSuggestPerGender();

      @Query("SELECT  CASE WHEN s.serviceIndexe.id IS NULL THEN 0 ELSE sp.id END as id,  CASE WHEN s.serviceIndexe.id IS NULL THEN 'Non defini' ELSE  sp.libelle END as libelle, COUNT(s.code) as total, CASE WHEN s.gender IS NULL THEN 'NON_DEFINI' ELSE s.gender END as gender FROM Suggestion s  LEFT JOIN ServicePoint sp ON s.serviceIndexe.id = sp.id WHERE s.status != 'TEMP_SAVED' GROUP BY libelle, id, gender, COALESCE(gender, 'null'), COALESCE(sp.id, 'null')")
    List<ClaimPerGenderAndAgencePrjt> countSuggestPerGenderAndAgence();

    Long countByStatusNotAndReceiptDateTimeBetween(ClaimStatus status, LocalDateTime start, LocalDateTime end);

}
