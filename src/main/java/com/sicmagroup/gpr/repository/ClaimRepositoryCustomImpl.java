package com.sicmagroup.gpr.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sicmagroup.gpr.api.report.FilterRequest;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.CollectionChannel;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.domain.model.Product;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.projection.ClaimPerObjLevelAndAgenceProjection;
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
import com.sicmagroup.gpr.utils.Utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.CriteriaBuilder.In;

public class ClaimRepositoryCustomImpl implements ClaimRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public long countClaimByCriterias(FilterRequest request, ClaimType type) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Claim> query = cb.createQuery(Claim.class);
        Root<Claim> claim = query.from(Claim.class);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(claim.get("type"), type));

        predicates.addAll(predicateBasedOnFilter(request, cb, query, claim));

        query.select(claim).where(predicates.toArray(new Predicate[predicates.size()]));
        List<Claim> resultat = entityManager.createQuery(query).getResultList();
        System.out.println(request);

        return resultat.size();
    }

    @Override
    public List<ClaimPerServicePointPro> countClaimByCriteriaAndByServicePoint(FilterRequest request,
            ClaimType type) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ClaimPerServicePointPro> query = cb.createQuery(ClaimPerServicePointPro.class);
        Root<Claim> claim = query.from(Claim.class);
        Root<ServicePoint> servicePoint = query.from(ServicePoint.class);

        // selection des colones
        query.multiselect(
                servicePoint.get("id").alias("servicePointId"),
                servicePoint.get("libelle").alias("libelle"),
                cb.count(claim.get("code")).alias("total"));

        Join<Claim, ServicePoint> joinServiceClaim = claim.join("servicePoint");

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(claim.get("type"), type));
        predicates.add(cb.equal(servicePoint.get("id"), joinServiceClaim.get("id")));

        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet = subquery.from(Objet.class);
            subquery.select(objet)
                    .where(objet.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            // System.out.println(request.getReceiveStart());
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {

            predicates.add(claim.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel)
                    .where(collectionChannel.get("id").in(request.getCanals()));
            predicates.add(claim.get("collectionChannel").in(subquery));
        }

        query.groupBy(
                servicePoint.get("id"),
                servicePoint.get("libelle"));

        query.where(predicates.toArray(new Predicate[predicates.size()]));
        TypedQuery<ClaimPerServicePointPro> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    private List<Predicate> predicateBasedOnFilter(FilterRequest request, CriteriaBuilder cb,
            CriteriaQuery<Claim> query, Root<Claim> claim) {
        List<Predicate> predicates = new ArrayList<>();
        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet = subquery.from(Objet.class);
            subquery.select(objet)
                    .where(objet.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            // System.out.println(request.getReceiveStart());
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint = subquery.from(ServicePoint.class);
            subquery.select(servicePoint)
                    .where(servicePoint.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {
            predicates.add(claim.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel)
                    .where(collectionChannel.get("id").in(request.getCanals()));

            predicates.add(claim.get("collectionChannel").in(subquery));
        }
        return predicates;

    }

    @Override
    public List<ObjectTotalPerStatusPro> countClaimByCriteriaAndStatus(FilterRequest request, ClaimType type) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ObjectTotalPerStatusPro> query = cb.createQuery(ObjectTotalPerStatusPro.class);
        Root<Claim> claim = query.from(Claim.class);

        query.multiselect(
                claim.get("status").alias("status"),
                cb.count(claim.get("code")).alias("total"));
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(claim.get("type"), type));
        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet = subquery.from(Objet.class);
            subquery.select(objet)
                    .where(objet.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {

            predicates.add(claim.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel)
                    .where(collectionChannel.get("id").in(request.getCanals()));
            predicates.add(claim.get("collectionChannel").in(subquery));
        }

        query.groupBy(
                claim.get("status"));

        query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<ObjectTotalPerStatusPro> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    @Override
    public List<ObjectPerCanalPro> countClaimByCriteriaAndCanal(FilterRequest request, ClaimType type) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ObjectPerCanalPro> query = cb.createQuery(ObjectPerCanalPro.class);
        Root<Claim> claim = query.from(Claim.class);
        Root<CollectionChannel> collectionChannel = query.from(CollectionChannel.class);

        query.multiselect(
                collectionChannel.get("id").alias("id"),
                collectionChannel.get("libelle").alias("libelle"),
                cb.count(claim.get("code")).alias("total"));

        Join<Claim, CollectionChannel> joinClaimCollect = claim.join("collectionChannel");

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(claim.get("type"), type));
        predicates.add(cb.equal(collectionChannel.get("id"), joinClaimCollect.get("id")));

        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet = subquery.from(Objet.class);
            subquery.select(objet)
                    .where(objet.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {

            predicates.add(claim.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel2 = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel2)
                    .where(collectionChannel2.get("id").in(request.getCanals()));
            predicates.add(claim.get("collectionChannel").in(subquery));
        }

        query.groupBy(
                collectionChannel.get("id"));

        query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<ObjectPerCanalPro> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();

    }

    @Override
    public List<ClaimPerCanalAndSpPro> countClaimByCriteriaAndCanalAndSp(FilterRequest request, ClaimType type) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ClaimPerCanalAndSpPro> query = cb.createQuery(ClaimPerCanalAndSpPro.class);
        Root<Claim> claim = query.from(Claim.class);
        Root<CollectionChannel> collectionChannel = query.from(CollectionChannel.class);
        Root<ServicePoint> servicePoint = query.from(ServicePoint.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(claim.get("type"), type));

        query.multiselect(
                collectionChannel.get("id").alias("canalId"),
                collectionChannel.get("libelle").alias("canalLibelle"),
                servicePoint.get("id").alias("spId"),
                servicePoint.get("libelle").alias("spLibelle"),
                cb.count(claim.get("code")).alias("total"));

        // jointure
        Join<Claim, CollectionChannel> joinClaimCollect = claim.join("collectionChannel");
        predicates.add(cb.equal(collectionChannel.get("id"), joinClaimCollect.get("id")));
        Join<Claim, ServicePoint> joinServiceClaim = claim.join("servicePoint");
        predicates.add(cb.equal(servicePoint.get("id"), joinServiceClaim.get("id")));
           
        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet = subquery.from(Objet.class);
            subquery.select(objet)
                    .where(objet.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {
           
            predicates.add(claim.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel2 = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel2)
                    .where(collectionChannel2.get("id").in(request.getCanals()));
            predicates.add(claim.get("collectionChannel").in(subquery));
        }

        query.groupBy(
                servicePoint.get("id"),
                collectionChannel.get("id"));

        query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<ClaimPerCanalAndSpPro> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();

    }

    @Override
    public List<ObjectPerCanalPro> countClaimByCriteriaAndObjet(FilterRequest request, ClaimType type) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ObjectPerCanalPro> query = cb.createQuery(ObjectPerCanalPro.class);
        Root<Claim> claim = query.from(Claim.class);
        Root<Objet> objet = query.from(Objet.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(claim.get("type"), type));

        query.multiselect(
                objet.get("id").alias("id"),
                objet.get("libelle").alias("libelle"),
                cb.count(claim.get("code")).alias("total"));
        Join<Claim, Objet> joinClaimObj = claim.join("objet");
        predicates.add(cb.equal(objet.get("id"), joinClaimObj.get("id")));
        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet2 = subquery.from(Objet.class);
            subquery.select(objet2)
                    .where(objet2.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {

            predicates.add(claim.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel2 = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel2)
                    .where(collectionChannel2.get("id").in(request.getCanals()));
            predicates.add(claim.get("collectionChannel").in(subquery));
        }

        query.groupBy(
                objet.get("id"));

        query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<ObjectPerCanalPro> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    @Override
    public List<ObjectPerCanalPro> countObjectByCriteriaAndObjet(FilterRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ObjectPerCanalPro> query = cb.createQuery(ObjectPerCanalPro.class);
        Root<Claim> claim = query.from(Claim.class);
        Root<Objet> objet = query.from(Objet.class);
        List<Predicate> predicates = new ArrayList<>();

        query.multiselect(
                objet.get("id").alias("id"),
                objet.get("libelle").alias("libelle"),
                cb.count(claim.get("code")).alias("total"));
        Join<Claim, Objet> joinClaimObj = claim.join("objet");
        predicates.add(cb.equal(objet.get("id"), joinClaimObj.get("id")));
        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet2 = subquery.from(Objet.class);
            subquery.select(objet2)
                    .where(objet2.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {

            predicates.add(claim.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel2 = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel2)
                    .where(collectionChannel2.get("id").in(request.getCanals()));
            predicates.add(claim.get("collectionChannel").in(subquery));
        }

        query.groupBy(
                objet.get("id"));

        query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<ObjectPerCanalPro> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    @Override
    public List<ObjectPerObjPro> countObjByCriteriaAndByObjetAndAgence(FilterRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ObjectPerObjPro> query = cb.createQuery(ObjectPerObjPro.class);
        Root<Claim> claim = query.from(Claim.class);
        Root<Objet> objet = query.from(Objet.class);
        Root<ServicePoint> servicePoint = query.from(ServicePoint.class);
        List<Predicate> predicates = new ArrayList<>();

        query.multiselect(
                objet.get("id").alias("idObj"),
                objet.get("libelle").alias("libelleObj"),
                servicePoint.get("id").alias("idSp"),
                servicePoint.get("libelle").alias("libelleSp"),
                cb.count(claim.get("code")).alias("total"));
        Join<Claim, Objet> joinClaimObj = claim.join("objet");
        predicates.add(cb.equal(objet.get("id"), joinClaimObj.get("id")));
        Join<Claim, ServicePoint> joinServiceClaim = claim.join("servicePoint");
        predicates.add(cb.equal(servicePoint.get("id"), joinServiceClaim.get("id")));

        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet2 = subquery.from(Objet.class);
            subquery.select(objet2)
                    .where(objet2.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {

            predicates.add(claim.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel2 = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel2)
                    .where(collectionChannel2.get("id").in(request.getCanals()));
            predicates.add(claim.get("collectionChannel").in(subquery));
        }

        query.groupBy(
                objet.get("id"),
                servicePoint.get("id"));

        query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<ObjectPerObjPro> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    @Override
    public List<ObjectPerObjPro> countClaimByCriteriaAndByObjetAndAgence(FilterRequest request, ClaimType type) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ObjectPerObjPro> query = cb.createQuery(ObjectPerObjPro.class);
        Root<Claim> claim = query.from(Claim.class);
        Root<Objet> objet = query.from(Objet.class);
        Root<ServicePoint> servicePoint = query.from(ServicePoint.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(claim.get("type"), type));
        query.multiselect(
                objet.get("id").alias("idObj"),
                objet.get("libelle").alias("libelleObj"),
                servicePoint.get("id").alias("idSp"),
                servicePoint.get("libelle").alias("libelleSp"),
                cb.count(claim.get("code")).alias("total"));
        Join<Claim, Objet> joinClaimObj = claim.join("objet");
        predicates.add(cb.equal(objet.get("id"), joinClaimObj.get("id")));
        Join<Claim, ServicePoint> joinServiceClaim = claim.join("servicePoint");
        predicates.add(cb.equal(servicePoint.get("id"), joinServiceClaim.get("id")));

        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet2 = subquery.from(Objet.class);
            subquery.select(objet2)
                    .where(objet2.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {

            predicates.add(claim.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel2 = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel2)
                    .where(collectionChannel2.get("id").in(request.getCanals()));
            predicates.add(claim.get("collectionChannel").in(subquery));
        }

        query.groupBy(
                objet.get("id"),
                servicePoint.get("id"));

        query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<ObjectPerObjPro> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    @Override
    public List<ClaimPerGenderPro> countClaimByCriteriaAndByGender(FilterRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ClaimPerGenderPro> query = cb.createQuery(ClaimPerGenderPro.class);
        Root<Claim> claim = query.from(Claim.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(claim.get("type"), ClaimType.CLAIM));
        query.multiselect(
                claim.get("gender").alias("gender"),
                cb.count(claim.get("code")).alias("total"));

        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet2 = subquery.from(Objet.class);
            subquery.select(objet2)
                    .where(objet2.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {

            predicates.add(claim.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel2 = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel2)
                    .where(collectionChannel2.get("id").in(request.getCanals()));
            predicates.add(claim.get("collectionChannel").in(subquery));
        }

        query.groupBy(
                claim.get("gender"));

        query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<ClaimPerGenderPro> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();

    }

    @Override
    public List<ClaimPerGenderAndAgencePro> countClaimByCriteriaAndByGenderAndAgence(FilterRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ClaimPerGenderAndAgencePro> query = cb.createQuery(ClaimPerGenderAndAgencePro.class);
        Root<Claim> claim = query.from(Claim.class);
        Root<ServicePoint> servicePoint = query.from(ServicePoint.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(claim.get("type"), ClaimType.CLAIM));
        query.multiselect(
                servicePoint.get("id").alias("id"),
                servicePoint.get("libelle").alias("libelle"),
                claim.get("gender").alias("gender"),
                cb.count(claim.get("code")).alias("total"));

        Join<Claim, ServicePoint> joinClaimSp = claim.join("servicePoint");
        predicates.add(cb.equal(servicePoint.get("id"), joinClaimSp.get("id")));

        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet2 = subquery.from(Objet.class);
            subquery.select(objet2)
                    .where(objet2.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {

            predicates.add(claim.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel2 = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel2)
                    .where(collectionChannel2.get("id").in(request.getCanals()));
            predicates.add(claim.get("collectionChannel").in(subquery));
        }

        query.groupBy(
                claim.get("gender"), servicePoint.get("id"));

        query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<ClaimPerGenderAndAgencePro> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }

    @Override
    public List<ClaimPerObjLevelPro> countClaimByCriteriaAndByObjLevel(FilterRequest request, ClaimType type) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ClaimPerObjLevelPro> query = cb.createQuery(ClaimPerObjLevelPro.class);
        Root<Claim> claim = query.from(Claim.class);
        Root<Objet> objet = query.from(Objet.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(claim.get("type"), type));

        Join<Claim, Objet> joinClaimObj = claim.join("objet");
        predicates.add(cb.equal(objet.get("id"), joinClaimObj.get("id")));

        query.multiselect(
                objet.get("id").alias("objId"),
                objet.get("libelle").alias("objLibelle"),
                objet.get("risqueLevel").alias("objNiveau"),
                cb.count(claim.get("code")).alias("total"));

        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet2 = subquery.from(Objet.class);
            subquery.select(objet2)
                    .where(objet2.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {

            predicates.add(claim.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel2 = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel2)
                    .where(collectionChannel2.get("id").in(request.getCanals()));
            predicates.add(claim.get("collectionChannel").in(subquery));
        }

        query.groupBy(
                objet.get("risqueLevel"));

        query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<ClaimPerObjLevelPro> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();

    }

    @Override
    public List<ClaimPerObjLevelAndSpPro> countClaimByCriteriaAndByObjLevelAndSp(FilterRequest request,
            ClaimType type) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ClaimPerObjLevelAndSpPro> query = cb.createQuery(ClaimPerObjLevelAndSpPro.class);
        Root<Claim> claim = query.from(Claim.class);
        Root<Objet> objet = query.from(Objet.class);
        Root<ServicePoint> servicePoint = query.from(ServicePoint.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(claim.get("type"), type));

        Join<Claim, Objet> joinClaimObj = claim.join("objet");
        predicates.add(cb.equal(objet.get("id"), joinClaimObj.get("id")));
        Join<Claim, ServicePoint> joinServiceClaim = claim.join("servicePoint");
        predicates.add(cb.equal(servicePoint.get("id"), joinServiceClaim.get("id")));

        query.multiselect(
                objet.get("id").alias("objId"),
                objet.get("libelle").alias("objLib"),
                servicePoint.get("id").alias("spId"),
                servicePoint.get("libelle").alias("spLib"),
                objet.get("risqueLevel").alias("objNiveau"),
                cb.count(claim.get("code")).alias("total"));

        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet2 = subquery.from(Objet.class);
            subquery.select(objet2)
                    .where(objet2.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {

            predicates.add(claim.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel2 = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel2)
                    .where(collectionChannel2.get("id").in(request.getCanals()));
            predicates.add(claim.get("collectionChannel").in(subquery));
        }

        query.groupBy(
                objet.get("risqueLevel"),
                servicePoint.get("libelle"));

        query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<ClaimPerObjLevelAndSpPro> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    @Override
    public List<ObjectTotalPerStatusPro> countClaimByCriteriaAndSatisfaction(FilterRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ObjectTotalPerStatusPro> query = cb.createQuery(ObjectTotalPerStatusPro.class);
        Root<Claim> claim = query.from(Claim.class);

        query.multiselect(
                claim.get("status").alias("status"),
                cb.count(claim.get("code")).alias("total"));
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(claim.get("type"), ClaimType.CLAIM));
        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet = subquery.from(Objet.class);
            subquery.select(objet)
                    .where(objet.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }

        // if (request.getEtats() != null && !request.getEtats().isEmpty()) {
        predicates.add(claim.get("status")
                .in(Arrays.asList(ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED, ClaimStatus.PARTIAL_SATISFIED)));
        // } else {
        // predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        // }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel)
                    .where(collectionChannel.get("id").in(request.getCanals()));
            predicates.add(claim.get("collectionChannel").in(subquery));
        }

        query.groupBy(
                claim.get("status"));

        query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<ObjectTotalPerStatusPro> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    @Override
    public List<Claim> findClaimByCriteriaAndTypeAndStatus(FilterRequest request, List<ClaimStatus> status) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Claim> query = cb.createQuery(Claim.class);
        Root<Claim> claim = query.from(Claim.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(claim.get("type"), ClaimType.CLAIM));
        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet = subquery.from(Objet.class);
            subquery.select(objet)
                    .where(objet.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }

        // if (request.getEtats() != null && !request.getEtats().isEmpty()) {
        predicates.add(claim.get("status").in(status));
        // } else {
        // predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        // }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel)
                    .where(collectionChannel.get("id").in(request.getCanals()));
            predicates.add(claim.get("collectionChannel").in(subquery));
        }
        query.select(claim).where(predicates.toArray(new Predicate[0]));
        List<Claim> resultat = entityManager.createQuery(query).getResultList();
        return resultat;
    }

    @Override
    public List<Claim> countClaimByCriteriaAndTypeAndStatusNotAndReceiptDateTimeBetween(FilterRequest request,
            ClaimType type) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Claim> query = cb.createQuery(Claim.class);
        Root<Claim> claim = query.from(Claim.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(claim.get("type"), type));
        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet = subquery.from(Objet.class);
            subquery.select(objet)
                    .where(objet.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }
        if (request.getEtats() != null && !request.getEtats().isEmpty()) {
            predicates.add(claim.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel)
                    .where(collectionChannel.get("id").in(request.getCanals()));
            predicates.add(claim.get("collectionChannel").in(subquery));
        }
        query.select(claim).where(predicates.toArray(new Predicate[0]));
        List<Claim> resultat = entityManager.createQuery(query).getResultList();
        return resultat;
    }

    @Override
    public List<Claim> countClaimByCriteriaAndTypeAndStatusIn(FilterRequest request, ClaimType type,
            List<ClaimStatus> status) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Claim> query = cb.createQuery(Claim.class);
        Root<Claim> claim = query.from(Claim.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(claim.get("type"), type));
        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet = subquery.from(Objet.class);
            subquery.select(objet)
                    .where(objet.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }

        predicates.add(claim.get("status").in(status));

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel)
                    .where(collectionChannel.get("id").in(request.getCanals()));
            predicates.add(claim.get("collectionChannel").in(subquery));
        }
        query.select(claim).where(predicates.toArray(new Predicate[0]));
        List<Claim> resultat = entityManager.createQuery(query).getResultList();
        return resultat;
    }

    @Override
    public List<ObjectPerYear> countClaimByCriteriaAndYearAndSp(FilterRequest request, ClaimType type) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ObjectPerYear> query = cb.createQuery(ObjectPerYear.class);
        Root<Claim> claim = query.from(Claim.class);
        Root<ServicePoint> servicePoint = query.from(ServicePoint.class);
        List<Predicate> predicates = new ArrayList<>();
        Join<Claim, ServicePoint> joinServiceClaim = claim.join("servicePoint");
        predicates.add(cb.equal(servicePoint.get("id"), joinServiceClaim.get("id")));

        query.multiselect(
                cb.function("YEAR", Integer.class, claim.get("receiptDateTime")).alias("year"),
                cb.count(claim.get("code")).alias("total"),
                servicePoint.get("id").alias("spId"),
                servicePoint.get("libelle").alias("spLib"));

        predicates.add(cb.equal(claim.get("type"), type));
        if (request != null) {
            if (request.getObjets() != null && !request.getObjets().isEmpty()) {

                Subquery<Objet> subquery = query.subquery(Objet.class);
                Root<Objet> objet = subquery.from(Objet.class);
                subquery.select(objet)
                        .where(objet.get("id").in(request.getObjets()));

                predicates.add(claim.get("objet").in(subquery));
            }
            if (request.getReceiveStart() != null && request.getReceiveEnd() != null
                    && request.getReceiveStart() != "") {
                predicates
                        .add(cb.between(claim.get("receiptDateTime"),
                                Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                                Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
            }
            if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
                LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
                LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
                predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
            }

            if (request.getProducts() != null && !request.getProducts().isEmpty()) {

                Subquery<Product> subquery = query.subquery(Product.class);
                Root<Product> product = subquery.from(Product.class);
                subquery.select(product)
                        .where(product.get("id").in(request.getProducts()));

                predicates.add(claim.get("product").in(subquery));
            }

            if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
                Subquery<User> subquery = query.subquery(User.class);
                Root<User> user = subquery.from(User.class);
                subquery.select(user)
                        .where(user.get("id").in(request.getSavedBy()));

                predicates.add(claim.get("collector").in(subquery));
            }

            if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
                Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
                Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
                subquery.select(servicePoint2)
                        .where(servicePoint2.get("id").in(request.getServicePoints()));

                predicates.add(claim.get("servicePoint").in(subquery));
            }
            if (request.getEtats() != null && !request.getEtats().isEmpty()) {
                predicates.add(claim.get("status").in(request.getEtats()));
            } else {
                predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
            }

            if (request.getCanals() != null && !request.getCanals().isEmpty()) {
                Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
                Root<CollectionChannel> collectionChannel = subquery.from(CollectionChannel.class);
                subquery.select(collectionChannel)
                        .where(collectionChannel.get("id").in(request.getCanals()));
                predicates.add(claim.get("collectionChannel").in(subquery));
            }
        } else {
            predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        }

        query.groupBy(
                cb.function("YEAR", Integer.class, claim.get("receiptDateTime")),
                servicePoint.get("libelle"));

        query.where(predicates.toArray(new Predicate[0]));
        List<ObjectPerYear> resultat = entityManager.createQuery(query).getResultList();
        return resultat;
    }

    @Override
    public   List<Claim> countClaimByCriteriaAndStatusNot(FilterRequest request, ClaimType type) {
         CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Claim> query = cb.createQuery(Claim.class);
        Root<Claim> claim = query.from(Claim.class);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(claim.get("type"), type));

        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet = subquery.from(Objet.class);
            subquery.select(objet)
                    .where(objet.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            // System.out.println(request.getReceiveStart());
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint = subquery.from(ServicePoint.class);
            subquery.select(servicePoint)
                    .where(servicePoint.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {
            predicates.add(claim.get("status").in(request.getEtats()).not());
        } else {
            predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel)
                    .where(collectionChannel.get("id").in(request.getCanals()));

            predicates.add(claim.get("collectionChannel").in(subquery));
        }
        

        

        query.select(claim).where(predicates.toArray(new Predicate[predicates.size()]));
        List<Claim> resultat = entityManager.createQuery(query).getResultList();
        // System.out.println(resultat.size());

        return resultat;
    }

    @Override
    public List<Claim> countClaimByCriteriaAndStatusIn(FilterRequest request, ClaimType type) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Claim> query = cb.createQuery(Claim.class);
        Root<Claim> claim = query.from(Claim.class);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(claim.get("type"), type));

        if (request.getObjets() != null && !request.getObjets().isEmpty()) {

            Subquery<Objet> subquery = query.subquery(Objet.class);
            Root<Objet> objet = subquery.from(Objet.class);
            subquery.select(objet)
                    .where(objet.get("id").in(request.getObjets()));

            predicates.add(claim.get("objet").in(subquery));
        }
        if (request.getReceiveStart() != null && request.getReceiveEnd() != null && request.getReceiveStart() != "") {
            // System.out.println(request.getReceiveStart());
            predicates
                    .add(cb.between(claim.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }
        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(claim.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {

            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(claim.get("product").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(claim.get("collector").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint = subquery.from(ServicePoint.class);
            subquery.select(servicePoint)
                    .where(servicePoint.get("id").in(request.getServicePoints()));

            predicates.add(claim.get("servicePoint").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {
            predicates.add(claim.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(claim.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel)
                    .where(collectionChannel.get("id").in(request.getCanals()));

            predicates.add(claim.get("collectionChannel").in(subquery));
        }
        

        

        query.select(claim).where(predicates.toArray(new Predicate[predicates.size()]));
        List<Claim> resultat = entityManager.createQuery(query).getResultList();
        // System.out.println(resultat.size());

        return resultat;
    }

}
