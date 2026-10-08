package com.sicmagroup.gpr.sla.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.sla.domain.ClaimSla;
import com.sicmagroup.gpr.sla.domain.SlaCycleKind;
import com.sicmagroup.gpr.sla.domain.SlaPhase;
import com.sicmagroup.gpr.sla.dto.SlaRows.ItemRow;
import com.sicmagroup.gpr.sla.dto.SlaRows.OverdueRow;
import com.sicmagroup.gpr.sla.dto.SlaRows.StatsRow;

/**
 * Les listes, comptes et indicateurs ne lisent que la table du SLA : la plainte y est recopiée (point de
 * service, collecteur, personnes concernées, suppression, échéance en vigueur). Aucune jointure avec la table
 * des plaintes pour filtrer : c'est ce qui garde les écrans rapides sur des dizaines de milliers de plaintes.
 * Périmètre : all (Pilote, DE) ; sinon la plainte est dans l'un des points de service, affectée ou transmise
 * à l'utilisateur, ou saisie par lui (agents seulement).
 */
public interface ClaimSlaRepository extends JpaRepository<ClaimSla, Long> {

    String SCOPE = "(:all = true or s.servicePointId in :spIds or s.affectedToId = :uid "
            + "or s.transmittedToId = :uid or s.collectorId = :collectorUid)";

    // --- Lecture d'un compteur -------------------------------------------------------------------------------

    /** Compteur courant d'une plainte : le dernier cycle qui n'est pas annulé. */
    Optional<ClaimSla> findFirstByTargetTypeAndClaimIdAndPhaseNotOrderByCycleDesc(ClaimType targetType, Long claimId,
            SlaPhase excluded);

    List<ClaimSla> findByTargetTypeAndClaimIdInAndPhaseNot(ClaimType targetType, Collection<Long> claimIds,
            SlaPhase excluded);

    List<ClaimSla> findByTargetTypeAndClaimId(ClaimType targetType, Long claimId);

    // --- Tâche planifiée ----------------------------------------------------------------------------------------

    /** Compteurs dont le prochain contrôle est arrivé (ouverts, ou en pause pour l'échéance réglementaire). */
    @Query("select s from ClaimSla s where s.phase in (com.sicmagroup.gpr.sla.domain.SlaPhase.OPEN, "
            + "com.sicmagroup.gpr.sla.domain.SlaPhase.PAUSED) "
            + "and s.nextCheckAt is not null and s.nextCheckAt <= :now order by s.nextCheckAt")
    List<ClaimSla> findDue(@Param("now") LocalDateTime now, Pageable pageable);

    /** Dossiers en retard arrivés au dernier niveau de remontée (récapitulatif quotidien Pilote et DE). */
    List<ClaimSla> findByPhaseAndStuckNotifiedTrue(SlaPhase phase);

    /** Plaintes sans compteur ouvert (nouvelles, hors ligne, robot, restaurées) : à rattraper. */
    @Query("select c.id from Claim c where c.isDeleted = false and c.type in :types "
            + "and not exists (select 1 from ClaimSla s where s.claimId = c.id and s.targetType = c.type "
            + "and s.phase <> com.sicmagroup.gpr.sla.domain.SlaPhase.CANCELLED)")
    List<Long> findClaimIdsWithoutSla(@Param("types") Collection<ClaimType> types, Pageable pageable);

    @Query("select g.id from Suggestion g where g.isDeleted = false "
            + "and not exists (select 1 from ClaimSla s where s.claimId = g.id "
            + "and s.targetType = com.sicmagroup.gpr.domain.enumeration.ClaimType.SUGGESTION "
            + "and s.phase <> com.sicmagroup.gpr.sla.domain.SlaPhase.CANCELLED)")
    List<Long> findSuggestionIdsWithoutSla(Pageable pageable);

    /** Plaintes supprimées (suppression douce) : leur compteur sort des listes et indicateurs. */
    @Modifying
    @Query("update ClaimSla s set s.claimDeleted = true where s.claimDeleted = false and ("
            + "(s.targetType <> com.sicmagroup.gpr.domain.enumeration.ClaimType.SUGGESTION and s.claimId in "
            + "(select c.id from Claim c where c.isDeleted = true and c.type = s.targetType)) "
            + "or (s.targetType = com.sicmagroup.gpr.domain.enumeration.ClaimType.SUGGESTION and s.claimId in "
            + "(select g.id from Suggestion g where g.isDeleted = true)))")
    int markDeleted();

    /** Plaintes restaurées : elles reviennent dans les listes. */
    @Modifying
    @Query("update ClaimSla s set s.claimDeleted = false where s.claimDeleted = true and ("
            + "(s.targetType <> com.sicmagroup.gpr.domain.enumeration.ClaimType.SUGGESTION and s.claimId in "
            + "(select c.id from Claim c where c.isDeleted = false and c.type = s.targetType)) "
            + "or (s.targetType = com.sicmagroup.gpr.domain.enumeration.ClaimType.SUGGESTION and s.claimId in "
            + "(select g.id from Suggestion g where g.isDeleted = false)))")
    int markRestored();

    // --- Listes et comptes dans le périmètre --------------------------------------------------------------------

    /**
     * Liste paginée filtrée par état. L'état se déduit de la phase et de l'échéance en vigueur : DEPASSE =
     * ouvert et échéance passée ; A_RISQUE = ouvert, dans les délais, seuil des 75 % atteint ; EN_COURS =
     * ouvert, dans les délais ; SUSPENDU ; RESPECTE ; HORS_DELAI (échéance réglementaire). « ALL » = tout.
     */
    @Query("select s from ClaimSla s where s.claimDeleted = false "
            + "and s.phase <> com.sicmagroup.gpr.sla.domain.SlaPhase.CANCELLED "
            + "and (:type is null or s.targetType = :type) "
            + "and (:state = 'ALL' "
            + "  or (:state = 'OPEN' and s.phase = com.sicmagroup.gpr.sla.domain.SlaPhase.OPEN) "
            + "  or (:state = 'DEPASSE' and s.phase = com.sicmagroup.gpr.sla.domain.SlaPhase.OPEN "
            + "      and s.regulatoryBreached = false and s.dueAt < :now) "
            + "  or (:state = 'A_RISQUE' and s.phase = com.sicmagroup.gpr.sla.domain.SlaPhase.OPEN "
            + "      and s.regulatoryBreached = false and s.reminder2Sent = true and s.dueAt >= :now) "
            + "  or (:state = 'EN_COURS' and s.phase = com.sicmagroup.gpr.sla.domain.SlaPhase.OPEN "
            + "      and s.regulatoryBreached = false and s.reminder2Sent = false and s.dueAt >= :now) "
            + "  or (:state = 'SUSPENDU' and s.phase in (com.sicmagroup.gpr.sla.domain.SlaPhase.SUSPENDED, "
            + "      com.sicmagroup.gpr.sla.domain.SlaPhase.PAUSED)) "
            + "  or (:state = 'RESPECTE' and s.phase = com.sicmagroup.gpr.sla.domain.SlaPhase.DONE "
            + "      and s.regulatoryBreached = false and s.resolvedAt <= s.resolutionDueAt) "
            + "  or (:state = 'HORS_DELAI' and s.regulatoryBreached = true)) "
            + "and " + SCOPE + " order by s.dueAt asc")
    Slice<ClaimSla> findInScope(@Param("state") String state, @Param("type") ClaimType type,
            @Param("now") LocalDateTime now, @Param("all") boolean all, @Param("spIds") Collection<Long> spIds,
            @Param("uid") Long uid, @Param("collectorUid") Long collectorUid, Pageable pageable);

    /** Même liste, limitée à des plaintes précises (recherche par code : on trouve d'abord les identifiants). */
    @Query("select s from ClaimSla s where s.claimDeleted = false "
            + "and s.phase <> com.sicmagroup.gpr.sla.domain.SlaPhase.CANCELLED "
            + "and (:type is null or s.targetType = :type) and s.claimId in :ids "
            + "and (:state = 'ALL' "
            + "  or (:state = 'OPEN' and s.phase = com.sicmagroup.gpr.sla.domain.SlaPhase.OPEN) "
            + "  or (:state = 'DEPASSE' and s.phase = com.sicmagroup.gpr.sla.domain.SlaPhase.OPEN "
            + "      and s.regulatoryBreached = false and s.dueAt < :now) "
            + "  or (:state = 'A_RISQUE' and s.phase = com.sicmagroup.gpr.sla.domain.SlaPhase.OPEN "
            + "      and s.regulatoryBreached = false and s.reminder2Sent = true and s.dueAt >= :now) "
            + "  or (:state = 'EN_COURS' and s.phase = com.sicmagroup.gpr.sla.domain.SlaPhase.OPEN "
            + "      and s.regulatoryBreached = false and s.reminder2Sent = false and s.dueAt >= :now) "
            + "  or (:state = 'SUSPENDU' and s.phase in (com.sicmagroup.gpr.sla.domain.SlaPhase.SUSPENDED, "
            + "      com.sicmagroup.gpr.sla.domain.SlaPhase.PAUSED)) "
            + "  or (:state = 'RESPECTE' and s.phase = com.sicmagroup.gpr.sla.domain.SlaPhase.DONE "
            + "      and s.regulatoryBreached = false and s.resolvedAt <= s.resolutionDueAt) "
            + "  or (:state = 'HORS_DELAI' and s.regulatoryBreached = true)) "
            + "and " + SCOPE + " order by s.dueAt asc")
    Page<ClaimSla> findInScopeByIds(@Param("state") String state, @Param("type") ClaimType type,
            @Param("ids") Collection<Long> ids, @Param("now") LocalDateTime now, @Param("all") boolean all,
            @Param("spIds") Collection<Long> spIds, @Param("uid") Long uid, @Param("collectorUid") Long collectorUid,
            Pageable pageable);

    /** Identifiants des plaintes dont le code (ou le code client) contient le texte cherché. */
    @Query("select c.id from Claim c where c.isDeleted = false "
            + "and (lower(c.code) like :q or lower(coalesce(c.codeClient, '')) like :q)")
    List<Long> claimIdsMatching(@Param("q") String q, Pageable pageable);

    /**
     * Tous les chiffres du résumé en UNE seule lecture : [total, ouvertes, en cours, à risque, dépassées,
     * suspendues, respectées, hors délai réglementaire, traitées non mesurées, limite réglementaire proche].
     */
    @Query("select count(s), "
            + "sum(case when s.phase = com.sicmagroup.gpr.sla.domain.SlaPhase.OPEN then 1 else 0 end), "
            + "sum(case when s.phase = com.sicmagroup.gpr.sla.domain.SlaPhase.OPEN and s.regulatoryBreached = false "
            + "    and s.reminder2Sent = false and s.dueAt >= :now then 1 else 0 end), "
            + "sum(case when s.phase = com.sicmagroup.gpr.sla.domain.SlaPhase.OPEN and s.regulatoryBreached = false "
            + "    and s.reminder2Sent = true and s.dueAt >= :now then 1 else 0 end), "
            + "sum(case when s.phase = com.sicmagroup.gpr.sla.domain.SlaPhase.OPEN and s.regulatoryBreached = false "
            + "    and s.dueAt < :now then 1 else 0 end), "
            + "sum(case when s.phase in (com.sicmagroup.gpr.sla.domain.SlaPhase.SUSPENDED, "
            + "    com.sicmagroup.gpr.sla.domain.SlaPhase.PAUSED) then 1 else 0 end), "
            + "sum(case when s.phase = com.sicmagroup.gpr.sla.domain.SlaPhase.DONE and s.regulatoryBreached = false "
            + "    and s.resolvedAt <= s.resolutionDueAt then 1 else 0 end), "
            + "sum(case when s.regulatoryBreached = true then 1 else 0 end), "
            + "sum(case when s.phase = com.sicmagroup.gpr.sla.domain.SlaPhase.OPEN and s.resolvedAt is not null "
            + "    and s.targetType = com.sicmagroup.gpr.domain.enumeration.ClaimType.CLAIM then 1 else 0 end), "
            + "sum(case when s.phase in (com.sicmagroup.gpr.sla.domain.SlaPhase.OPEN, "
            + "    com.sicmagroup.gpr.sla.domain.SlaPhase.PAUSED) and s.resolvedAt is null "
            + "    and s.regulatoryBreached = false and s.regulatoryDueAt >= :now and s.regulatoryDueAt <= :limit "
            + "    and s.targetType <> com.sicmagroup.gpr.domain.enumeration.ClaimType.SUGGESTION then 1 else 0 end) "
            + "from ClaimSla s where s.claimDeleted = false "
            + "and s.phase <> com.sicmagroup.gpr.sla.domain.SlaPhase.CANCELLED "
            + "and (:type is null or s.targetType = :type) and " + SCOPE)
    List<Object[]> summaryCounts(@Param("type") ClaimType type, @Param("now") LocalDateTime now,
            @Param("limit") LocalDateTime limit, @Param("all") boolean all, @Param("spIds") Collection<Long> spIds,
            @Param("uid") Long uid, @Param("collectorUid") Long collectorUid);

    // --- Lectures légères (colonnes utiles seulement) -----------------------------------------------------------

    /** Plaintes de ce type en retard, tout périmètre : page Alertes et tableau de bord. */
    @Query("select new com.sicmagroup.gpr.sla.dto.SlaRows$OverdueRow(c.id, c.type, s.dueAt, c.code, c.codeClient, "
            + "c.clientFirstAndLastName, c.status, c.receiptDateTime, o.libelle, o.risqueLevel, sp.libelle) "
            + "from ClaimSla s, Claim c left join c.objet o left join c.servicePoint sp "
            + "where s.claimId = c.id and s.targetType = c.type and s.claimDeleted = false "
            + "and s.phase = com.sicmagroup.gpr.sla.domain.SlaPhase.OPEN and s.targetType = :type "
            + "and s.regulatoryBreached = false and s.dueAt < :now order by s.dueAt asc")
    List<OverdueRow> overdueRows(@Param("type") ClaimType type, @Param("now") LocalDateTime now);

    /** Attributs d'affichage des plaintes d'une page de la liste. */
    @Query("select new com.sicmagroup.gpr.sla.dto.SlaRows$ItemRow(c.id, c.type, c.code, c.codeClient, c.status, "
            + "o.libelle, o.risqueLevel, sp.libelle, c.receiptDateTime) "
            + "from Claim c left join c.objet o left join c.servicePoint sp where c.id in :ids")
    List<ItemRow> itemRows(@Param("ids") Collection<Long> ids);

    /** Compteurs principaux reçus pendant une période, dans le périmètre, avec les attributs de la plainte. */
    @Query("select new com.sicmagroup.gpr.sla.dto.SlaRows$StatsRow(s, o.risqueLevel, cat.libelle, o.libelle, "
            + "sp.libelle, u.firstandlastname, ch.libelle) "
            + "from ClaimSla s, Claim c left join c.objet o left join o.categorie cat left join c.servicePoint sp "
            + "left join c.treatmentAffectedTo u left join c.collectionChannel ch "
            + "where s.claimId = c.id and s.targetType = c.type and s.claimDeleted = false "
            + "and s.phase <> com.sicmagroup.gpr.sla.domain.SlaPhase.CANCELLED "
            + "and s.cycleKind = com.sicmagroup.gpr.sla.domain.SlaCycleKind.MAIN "
            + "and s.targetType in :types and s.receivedAt >= :from and s.receivedAt < :to and " + SCOPE)
    List<StatsRow> statsRows(@Param("types") Collection<ClaimType> types, @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to, @Param("all") boolean all, @Param("spIds") Collection<Long> spIds,
            @Param("uid") Long uid, @Param("collectorUid") Long collectorUid);

    /** Plaintes réouvertes (cycle « assurance satisfaction ») pendant une période. */
    @Query("select count(distinct s.claimId) from ClaimSla s where s.cycleKind = :kind "
            + "and s.targetType = com.sicmagroup.gpr.domain.enumeration.ClaimType.CLAIM and s.claimDeleted = false "
            + "and s.createdAt >= :from and s.createdAt < :to and " + SCOPE)
    long countReopened(@Param("kind") SlaCycleKind kind, @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to, @Param("all") boolean all, @Param("spIds") Collection<Long> spIds,
            @Param("uid") Long uid, @Param("collectorUid") Long collectorUid);
}
