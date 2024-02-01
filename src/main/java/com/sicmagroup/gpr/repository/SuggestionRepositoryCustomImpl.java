package com.sicmagroup.gpr.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.sicmagroup.gpr.api.report.FilterRequest;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.Gender;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.CollectionChannel;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.domain.model.Product;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerCanalAndSpPro;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerGenderAndAgencePro;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerGenderPro;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerServicePointPro;
import com.sicmagroup.gpr.repository.projection.custom.ObjectPerCanalPro;
import com.sicmagroup.gpr.repository.projection.custom.ObjectPerYear;
import com.sicmagroup.gpr.repository.projection.custom.ObjectTotalPerStatusPro;
import com.sicmagroup.gpr.repository.projection.custom.SuggestTotalPerStatusPro;
import com.sicmagroup.gpr.utils.Utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaBuilder.Case;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public class SuggestionRepositoryCustomImpl implements SuggestionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public long countSuggestByCriterias(FilterRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Suggestion> query = cb.createQuery(Suggestion.class);
        Root<Suggestion> suggestion = query.from(Suggestion.class);
        List<Predicate> predicates = new ArrayList<>();

        if (request.getReceiveStart() != null && request.getReceiveEnd() != null) {

            predicates
                    .add(cb.between(suggestion.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }

        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(suggestion.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {
            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(suggestion.get("produit").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(suggestion.get("collecteur").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(suggestion.get("serviceIndexe").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {
            predicates.add(suggestion.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(suggestion.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel)
                    .where(collectionChannel.get("id").in(request.getCanals()));

            predicates.add(suggestion.get("canal").in(subquery));
        }

        query.select(suggestion).where(predicates.toArray(new Predicate[predicates.size()]));
        List<Suggestion> resultat = entityManager.createQuery(query).getResultList();

        return resultat.size();
    }

    @Override
    public List<ClaimPerServicePointPro> countSuggestByCriteriaAndServicePoint(FilterRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ClaimPerServicePointPro> query = cb.createQuery(ClaimPerServicePointPro.class);
        Root<Suggestion> suggestion = query.from(Suggestion.class);
        Root<ServicePoint> servicePoint = query.from(ServicePoint.class);

        // selection des colones

        query.multiselect(

                servicePoint.get("id").alias("servicePointId"),
                cb.selectCase(servicePoint.get("id"))
                        .when(servicePoint.get("id").isNull(), "Non défini")
                        .otherwise(servicePoint.get("libelle"))
                        .alias("libelle"),
                cb.count(suggestion.get("code")).alias("total"));

        Join<Suggestion, ServicePoint> joinServiceSuggest = suggestion.join("serviceIndexe");

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(servicePoint.get("id"), joinServiceSuggest.get("id")));

        if (request.getReceiveStart() != null && request.getReceiveEnd() != null) {

            predicates
                    .add(cb.between(suggestion.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }

        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(suggestion.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {
            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(suggestion.get("produit").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(suggestion.get("collecteur").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(suggestion.get("serviceIndexe").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {
            predicates.add(suggestion.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(suggestion.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel)
                    .where(collectionChannel.get("id").in(request.getCanals()));

            predicates.add(suggestion.get("canal").in(subquery));
        }

        query.groupBy(
                cb.coalesce(servicePoint.get("id"), 0),
                // servicePoint.get("id"),
                servicePoint.get("libelle"));

        query.where(predicates.toArray(new Predicate[predicates.size()]));
        TypedQuery<ClaimPerServicePointPro> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    @Override
    public List<SuggestTotalPerStatusPro> countSuggestByCriteriaAndStatus(FilterRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SuggestTotalPerStatusPro> query = cb.createQuery(SuggestTotalPerStatusPro.class);
        Root<Suggestion> suggestion = query.from(Suggestion.class);

        query.multiselect(
                suggestion.get("status"),
                suggestion.get("accepted"),
                cb.count(suggestion.get("code")).alias("total"));
        List<Predicate> predicates = new ArrayList<>();

        if (request.getReceiveStart() != null && request.getReceiveEnd() != null) {

            predicates
                    .add(cb.between(suggestion.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }

        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(suggestion.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {
            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(suggestion.get("produit").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(suggestion.get("collecteur").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(suggestion.get("serviceIndexe").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {
            predicates.add(suggestion.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(suggestion.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel)
                    .where(collectionChannel.get("id").in(request.getCanals()));

            predicates.add(suggestion.get("canal").in(subquery));
        }

        query.groupBy(
                suggestion.get("status"), suggestion.get("accepted"));
        query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<SuggestTotalPerStatusPro> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    @Override
    public List<ObjectPerCanalPro> countSuggestByCriteriaAndCanal(FilterRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ObjectPerCanalPro> query = cb.createQuery(ObjectPerCanalPro.class);
        Root<Suggestion> suggestion = query.from(Suggestion.class);
        Root<CollectionChannel> collectionChannel = query.from(CollectionChannel.class);
        List<Predicate> predicates = new ArrayList<>();
        query.multiselect(
                collectionChannel.get("id").alias("id"),
                collectionChannel.get("libelle").alias("libelle"),
                cb.count(suggestion.get("code")).alias("total"));

        Join<Suggestion, CollectionChannel> joinSuggestCl = suggestion.join("canal");

        predicates.add(cb.equal(collectionChannel.get("id"), joinSuggestCl.get("id")));

        if (request.getReceiveStart() != null && request.getReceiveEnd() != null) {

            predicates
                    .add(cb.between(suggestion.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }

        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(suggestion.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {
            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(suggestion.get("produit").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(suggestion.get("collecteur").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(suggestion.get("serviceIndexe").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {
            predicates.add(suggestion.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(suggestion.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel2 = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel2)
                    .where(collectionChannel2.get("id").in(request.getCanals()));

            predicates.add(suggestion.get("canal").in(subquery));
        }

        query.groupBy(
                collectionChannel.get("id"));

        query.where(predicates.toArray(new Predicate[0]));
        TypedQuery<ObjectPerCanalPro> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();

    }

    @Override
    public List<ClaimPerCanalAndSpPro> countSuggestByCriteriaAndCanalAndSp(FilterRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ClaimPerCanalAndSpPro> query = cb.createQuery(ClaimPerCanalAndSpPro.class);
        Root<Suggestion> suggestion = query.from(Suggestion.class);
        Root<CollectionChannel> collectionChannel = query.from(CollectionChannel.class);
        Root<ServicePoint> servicePoint = query.from(ServicePoint.class);
        List<Predicate> predicates = new ArrayList<>();

        query.multiselect(
                collectionChannel.get("id").alias("canalId"),
                collectionChannel.get("libelle").alias("canalLibelle"),
                servicePoint.get("id").alias("spId"),
                servicePoint.get("libelle").alias("spLibelle"),
                cb.count(suggestion.get("code")).alias("total"));

        Join<Suggestion, CollectionChannel> joinSuggestCl = suggestion.join("canal");
        predicates.add(cb.equal(collectionChannel.get("id"), joinSuggestCl.get("id")));
        Join<Suggestion, ServicePoint> joinServiceSuggest = suggestion.join("serviceIndexe");
        predicates.add(cb.equal(servicePoint.get("id"), joinServiceSuggest.get("id")));

        if (request.getReceiveStart() != null && request.getReceiveEnd() != null) {

            predicates
                    .add(cb.between(suggestion.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }

        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(suggestion.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {
            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(suggestion.get("produit").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(suggestion.get("collecteur").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(suggestion.get("serviceIndexe").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {
            predicates.add(suggestion.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(suggestion.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel2 = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel2)
                    .where(collectionChannel2.get("id").in(request.getCanals()));

            predicates.add(suggestion.get("canal").in(subquery));
        }

        query.groupBy(
                collectionChannel.get("id"),
                cb.coalesce(servicePoint.get("id"), 0)

        );

        query.where(predicates.toArray(new Predicate[0]));
        TypedQuery<ClaimPerCanalAndSpPro> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    @Override
    public List<ClaimPerGenderPro> countSuggestByCriteriaAndGender(FilterRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ClaimPerGenderPro> query = cb.createQuery(ClaimPerGenderPro.class);
        Root<Suggestion> suggestion = query.from(Suggestion.class);
        List<Predicate> predicates = new ArrayList<>();
        query.multiselect(
                cb.count(suggestion.get("code")).alias("total"),
                cb.selectCase(suggestion.get("gender"))
                        .when(suggestion.get("gender").isNull(), Gender.NON_DEFINI.name())
                        .otherwise(suggestion.get("gender")).alias("gender"));

        if (request.getReceiveStart() != null && request.getReceiveEnd() != null) {

            predicates
                    .add(cb.between(suggestion.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }

        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(suggestion.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {
            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(suggestion.get("produit").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(suggestion.get("collecteur").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(suggestion.get("serviceIndexe").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {
            predicates.add(suggestion.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(suggestion.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel2 = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel2)
                    .where(collectionChannel2.get("id").in(request.getCanals()));

            predicates.add(suggestion.get("canal").in(subquery));
        }

        query.groupBy(
                cb.coalesce(suggestion.get("gender"), Gender.NON_DEFINI));

        query.where(predicates.toArray(new Predicate[0]));
        TypedQuery<ClaimPerGenderPro> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    @Override
    public List<ClaimPerGenderAndAgencePro> countSuggestByCriteriaAndGenderAndAgence(FilterRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ClaimPerGenderAndAgencePro> query = cb.createQuery(ClaimPerGenderAndAgencePro.class);
        Root<Suggestion> suggestion = query.from(Suggestion.class);
        Root<ServicePoint> servicePoint = query.from(ServicePoint.class);
        List<Predicate> predicates = new ArrayList<>();

        Join<Suggestion, ServicePoint> joinSuggestSp = suggestion.join("serviceIndexe", JoinType.LEFT);
        predicates.add(cb.equal(servicePoint.get("id"), joinSuggestSp.get("id")));
        query.multiselect(
            // servicePoint.get("id").as(Long.class).alias("id"),
            // servicePoint.get("libelle").as(String.class).alias("libelle"),
            cb.count(suggestion.get("code")).as(Long.class).alias("total"),
            suggestion.get("gender").alias("gender"),
            // cb.coalesce(suggestion.get("gender"), Gender.NON_DEFINI).as(Gender.class).alias("gender"),
                cb.selectCase()
                        .when(cb.isNull(suggestion.get("serviceIndexe")), 0)
                        .otherwise(servicePoint.get("id")).alias("id"),
                cb.selectCase()
                        .when(cb.isNull(suggestion.get("serviceIndexe")), "Non défini")
                        .otherwise(servicePoint.get("libelle")).alias("libelle")
                // cb.count(suggestion.get("code")).alias("total"),
                // cb.selectCase()
                //         .when(cb.isNull(suggestion.get("gender")), "NON_DEFINI")
                //         .otherwise(suggestion.get("gender")).alias("gender")
                    );

        if (request.getReceiveStart() != null && request.getReceiveEnd() != null) {

            predicates
                    .add(cb.between(suggestion.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }

        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(suggestion.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {
            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(suggestion.get("produit").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(suggestion.get("collecteur").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(suggestion.get("serviceIndexe").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {
            predicates.add(suggestion.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(suggestion.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel2 = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel2)
                    .where(collectionChannel2.get("id").in(request.getCanals()));

            predicates.add(suggestion.get("canal").in(subquery));
        }

        query.groupBy(
            suggestion.get("gender"),

            cb.selectCase()
                        .when(cb.isNull(suggestion.get("serviceIndexe")), 0)
                        .otherwise(servicePoint.get("id"))
                        
                        );

        query.where(predicates.toArray(new Predicate[0]));
         
        TypedQuery<ClaimPerGenderAndAgencePro> typedQuery = entityManager.createQuery(query);
       List<ClaimPerGenderAndAgencePro> r =  typedQuery.getResultList();
    //    System.out.println("Resultat");
    //    System.out.println(r);
        return  r ;
    }

    @Override
    public List<Suggestion> countSuggestByCriteriaAndPeriode(FilterRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Suggestion> query = cb.createQuery(Suggestion.class);
        Root<Suggestion> suggestion = query.from(Suggestion.class);
        List<Predicate> predicates = new ArrayList<>();

        if (request.getReceiveStart() != null && request.getReceiveEnd() != null) {

            predicates
                    .add(cb.between(suggestion.get("receiptDateTime"),
                            Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                            Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
        }

        if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
            LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
            predicates.add(cb.between(suggestion.get("receiptDateTime"), start, end));
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {
            Subquery<Product> subquery = query.subquery(Product.class);
            Root<Product> product = subquery.from(Product.class);
            subquery.select(product)
                    .where(product.get("id").in(request.getProducts()));

            predicates.add(suggestion.get("produit").in(subquery));
        }

        if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
            Subquery<User> subquery = query.subquery(User.class);
            Root<User> user = subquery.from(User.class);
            subquery.select(user)
                    .where(user.get("id").in(request.getSavedBy()));

            predicates.add(suggestion.get("collecteur").in(subquery));
        }

        if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
            Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
            Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
            subquery.select(servicePoint2)
                    .where(servicePoint2.get("id").in(request.getServicePoints()));

            predicates.add(suggestion.get("serviceIndexe").in(subquery));
        }

        if (request.getEtats() != null && !request.getEtats().isEmpty()) {
            predicates.add(suggestion.get("status").in(request.getEtats()));
        } else {
            predicates.add(cb.notEqual(suggestion.get("status"), ClaimStatus.TEMP_SAVED));
        }

        if (request.getCanals() != null && !request.getCanals().isEmpty()) {
            Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
            Root<CollectionChannel> collectionChannel = subquery.from(CollectionChannel.class);
            subquery.select(collectionChannel)
                    .where(collectionChannel.get("id").in(request.getCanals()));

            predicates.add(suggestion.get("canal").in(subquery));
        }

        query.select(suggestion).where(predicates.toArray(new Predicate[0]));
        List<Suggestion> resultat = entityManager.createQuery(query).getResultList();
        return resultat;
    }

    @Override
    public List<ObjectPerYear> countSuggestByCriteriaAndYearAndSp(FilterRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ObjectPerYear> query = cb.createQuery(ObjectPerYear.class);
        Root<Suggestion> suggestion = query.from(Suggestion.class);
        Root<ServicePoint> servicePoint = query.from(ServicePoint.class);
        List<Predicate> predicates = new ArrayList<>();

        Join<Suggestion, ServicePoint> joinServiceSuggest = suggestion.join("serviceIndexe");
        predicates.add(cb.equal(servicePoint.get("id"), joinServiceSuggest.get("id")));
        query.multiselect(
                cb.function("YEAR", Integer.class, suggestion.get("receiptDateTime")).alias("year"),
                cb.count(suggestion.get("code")).alias("total"),
                cb.selectCase(servicePoint.get("id"))
                        .when(servicePoint.get("id").isNull(), 0)
                        .otherwise(servicePoint.get("id")).alias("spId"),
                cb.selectCase(servicePoint.get("id"))
                        .when(servicePoint.get("id").isNull(), "Non défini")
                        .otherwise(servicePoint.get("libelle")).alias("spLib"));

        if (request != null) {
            if (request.getReceiveStart() != null && request.getReceiveEnd() != null) {

                predicates
                        .add(cb.between(suggestion.get("receiptDateTime"),
                                Utils.convertStrToLocalDateTime(request.getReceiveStart()),
                                Utils.convertStrToLocalDateTime(request.getReceiveEnd())));
            }

            if (request.getYear() != null && !request.getYear().toString().isEmpty()) {
                LocalDateTime start = LocalDateTime.parse(request.getYear() + "-01-01T00:00:00");
                LocalDateTime end = LocalDateTime.parse(request.getYear() + "-12-31T23:59:59");
                predicates.add(cb.between(suggestion.get("receiptDateTime"), start, end));
            }

            if (request.getProducts() != null && !request.getProducts().isEmpty()) {
                Subquery<Product> subquery = query.subquery(Product.class);
                Root<Product> product = subquery.from(Product.class);
                subquery.select(product)
                        .where(product.get("id").in(request.getProducts()));

                predicates.add(suggestion.get("produit").in(subquery));
            }

            if (request.getSavedBy() != null && !request.getSavedBy().isEmpty()) {
                Subquery<User> subquery = query.subquery(User.class);
                Root<User> user = subquery.from(User.class);
                subquery.select(user)
                        .where(user.get("id").in(request.getSavedBy()));

                predicates.add(suggestion.get("collecteur").in(subquery));
            }

            if (request.getServicePoints() != null && !request.getServicePoints().isEmpty()) {
                Subquery<ServicePoint> subquery = query.subquery(ServicePoint.class);
                Root<ServicePoint> servicePoint2 = subquery.from(ServicePoint.class);
                subquery.select(servicePoint2)
                        .where(servicePoint2.get("id").in(request.getServicePoints()));

                predicates.add(suggestion.get("serviceIndexe").in(subquery));
            }

            if (request.getEtats() != null && !request.getEtats().isEmpty()) {
                predicates.add(suggestion.get("status").in(request.getEtats()));
            } else {
                predicates.add(cb.notEqual(suggestion.get("status"), ClaimStatus.TEMP_SAVED));
            }

            if (request.getCanals() != null && !request.getCanals().isEmpty()) {
                Subquery<CollectionChannel> subquery = query.subquery(CollectionChannel.class);
                Root<CollectionChannel> collectionChannel = subquery.from(CollectionChannel.class);
                subquery.select(collectionChannel)
                        .where(collectionChannel.get("id").in(request.getCanals()));

                predicates.add(suggestion.get("canal").in(subquery));
            }

        } else {
            predicates.add(cb.notEqual(suggestion.get("status"), ClaimStatus.TEMP_SAVED));
        }

        query.groupBy(
                cb.function("YEAR", Integer.class, suggestion.get("receiptDateTime")),
                servicePoint.get("libelle"));

        query.where(predicates.toArray(new Predicate[0]));
        List<ObjectPerYear> resultat = entityManager.createQuery(query).getResultList();
        return resultat;
    }

}
