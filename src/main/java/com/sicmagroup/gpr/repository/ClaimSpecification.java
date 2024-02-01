package com.sicmagroup.gpr.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.CollectionChannel;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.domain.model.Product;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.User;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

public interface ClaimSpecification extends Specification<Claim> {
    static Specification<Claim> hasType(ClaimType type) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("type"), type);
    }

    static Specification<Claim> isNotTempSaved() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.notEqual(root.get("status"), ClaimStatus.TEMP_SAVED);
    }

    static Specification<Claim> withObjetIn(List<Long> objetIds) {
        return (root, query, criteriaBuilder) -> {
            Join<Objet, Claim> joinEntity = root.join("objet", JoinType.INNER);

            return joinEntity.get("id").in(objetIds);
        };
    }

    static Specification<Claim> receivedBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("receiptDateTime"), start, end);
    }

    static Specification<Claim> receiveByYear(long year) {
        LocalDateTime start = LocalDateTime.parse(year + "-01-01T00:00:00");
        LocalDateTime end = LocalDateTime.parse(year + "-12-31T23:59:59");

        return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("receiptDateTime"), start, end);
    }

    static Specification<Claim> withProductIn(List<Long> productIds) {
        return (root, query, criteriaBuilder) -> {

            Join<Product, Claim> joinEntity = root.join("product", JoinType.INNER);
            return joinEntity.get("id").in(productIds);
        };
    }

    static Specification<Claim> withCollectorsIs(List<Long> collectorsId) {
        return (root, query, criteriaBuilder) -> {

            Join<User, Claim> joinEntity = root.join("collector", JoinType.INNER);
            return joinEntity.get("id").in(collectorsId);
        };
    }

    static Specification<Claim> withServicePointIn(List<Long> servicePointsId) {
        return (root, query, criteriaBuilder) -> {

            Join<ServicePoint, Claim> joinEntity = root.join("servicePoint", JoinType.INNER);
            return joinEntity.get("id").in(servicePointsId);
        };
    }

    static Specification<Claim> withStatusIn(List<ClaimStatus> status) {
        return (root, query, criteriaBuilder) -> root.get("status").in(status);
    }

    static Specification<Claim> withCanalIn(List<Long> canalIds) {
        return (root, query, criteriaBuilder) -> {

            Join<CollectionChannel, Claim> joinEntity = root.join("collectionChannel", JoinType.INNER);
            return joinEntity.get("id").in(canalIds);
        };
    }

}
