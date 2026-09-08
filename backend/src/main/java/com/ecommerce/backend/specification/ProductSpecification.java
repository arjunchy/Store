package com.ecommerce.backend.specification;

import com.ecommerce.backend.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductSpecification {

    public Specification<Product> buildSearchSpecification(
            String name,
            String categoryId,
            java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice,
            Boolean isNewArrival,
            Float minRating) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.isBlank()) {
                String escaped = name.toLowerCase().replace("\\","\\\\").replace("%","\\%").replace("_","\\_");
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + escaped + "%", '\\'
                ));
            }

            if (categoryId != null && !categoryId.isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        root.get("category").get("id"),
                        categoryId
                ));
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("price"),
                        minPrice
                ));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("price"),
                        maxPrice
                ));
            }

            if (isNewArrival != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("isNewArrival"),
                        isNewArrival
                ));
            }

            if (minRating != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("rating"),
                        minRating
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
