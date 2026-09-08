package com.ecommerce.backend.specification;

import com.ecommerce.backend.entity.Product;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class ProductSpecificationTest {

    @InjectMocks
    private ProductSpecification productSpecification;

    private Root root;
    private CriteriaQuery query;
    private CriteriaBuilder cb;

    @BeforeEach
    void setUp() {
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);
    }

    @Test
    void buildSearchSpecification_noFilters_returnsTrue() {
        jakarta.persistence.criteria.Predicate truePred = mock(jakarta.persistence.criteria.Predicate.class);
        when(cb.and()).thenReturn(truePred);

        var spec = productSpecification.buildSearchSpecification(null, null, null, null, null, null);
        var predicate = spec.toPredicate(root, query, cb);

        assertThat(predicate).isEqualTo(truePred);
    }

    @Test
    void buildSearchSpecification_withName_addsLikePredicate() {
        Path namePath = mock(Path.class);
        jakarta.persistence.criteria.Expression<String> lowerPred = mock(jakarta.persistence.criteria.Expression.class);
        jakarta.persistence.criteria.Predicate likePred = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate truePred = mock(jakarta.persistence.criteria.Predicate.class);
        when(root.get("name")).thenReturn(namePath);
        when(cb.lower(namePath)).thenReturn(lowerPred);
        when(cb.like(lowerPred, "%laptop%")).thenReturn(likePred);
        when(cb.and(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(truePred);

        var spec = productSpecification.buildSearchSpecification("Laptop", null, null, null, null, null);
        spec.toPredicate(root, query, cb);

        verify(cb).like(lowerPred, "%laptop%");
    }

    @Test
    void buildSearchSpecification_withCategoryId_addsEqualPredicate() {
        Path categoryIdPath = mock(Path.class);
        Path categoryParentPath = mock(Path.class);
        jakarta.persistence.criteria.Predicate eqPred = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate truePred = mock(jakarta.persistence.criteria.Predicate.class);
        when(root.get("category")).thenReturn(categoryParentPath);
        when(categoryParentPath.get("id")).thenReturn(categoryIdPath);
        when(cb.equal(categoryIdPath, "cat-1")).thenReturn(eqPred);
        when(cb.and(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(truePred);

        var spec = productSpecification.buildSearchSpecification(null, "cat-1", null, null, null, null);
        spec.toPredicate(root, query, cb);

        verify(cb).equal(categoryIdPath, "cat-1");
    }

    @Test
    void buildSearchSpecification_withPriceRange_addsRangePredicates() {
        Path pricePath = mock(Path.class);
        jakarta.persistence.criteria.Predicate gtePred = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate ltePred = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate truePred = mock(jakarta.persistence.criteria.Predicate.class);
        when(root.get("price")).thenReturn(pricePath);
        when(cb.greaterThanOrEqualTo(pricePath, new BigDecimal("100"))).thenReturn(gtePred);
        when(cb.lessThanOrEqualTo(pricePath, new BigDecimal("500"))).thenReturn(ltePred);
        when(cb.and(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(truePred);

        var spec = productSpecification.buildSearchSpecification(null, null, new BigDecimal("100"), new BigDecimal("500"), null, null);
        spec.toPredicate(root, query, cb);

        verify(cb).greaterThanOrEqualTo(pricePath, new BigDecimal("100"));
        verify(cb).lessThanOrEqualTo(pricePath, new BigDecimal("500"));
    }

    @Test
    void buildSearchSpecification_withIsNewArrival_addsEqualPredicate() {
        Path isNewArrivalPath = mock(Path.class);
        jakarta.persistence.criteria.Predicate eqPred = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate truePred = mock(jakarta.persistence.criteria.Predicate.class);
        when(root.get("isNewArrival")).thenReturn(isNewArrivalPath);
        when(cb.equal(isNewArrivalPath, true)).thenReturn(eqPred);
        when(cb.and(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(truePred);

        var spec = productSpecification.buildSearchSpecification(null, null, null, null, true, null);
        spec.toPredicate(root, query, cb);

        verify(cb).equal(isNewArrivalPath, true);
    }

    @Test
    void buildSearchSpecification_withMinRating_addsGreaterThanOrEqualTo() {
        Path ratingPath = mock(Path.class);
        jakarta.persistence.criteria.Predicate gtePred = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate truePred = mock(jakarta.persistence.criteria.Predicate.class);
        when(root.get("rating")).thenReturn(ratingPath);
        when(cb.greaterThanOrEqualTo(ratingPath, 4.0f)).thenReturn(gtePred);
        when(cb.and(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(truePred);

        var spec = productSpecification.buildSearchSpecification(null, null, null, null, null, 4.0f);
        spec.toPredicate(root, query, cb);

        verify(cb).greaterThanOrEqualTo(ratingPath, 4.0f);
    }

    @Test
    void buildSearchSpecification_blankName_ignored() {
        jakarta.persistence.criteria.Predicate truePred = mock(jakarta.persistence.criteria.Predicate.class);
        when(cb.and()).thenReturn(truePred);

        var spec = productSpecification.buildSearchSpecification("   ", null, null, null, null, null);
        spec.toPredicate(root, query, cb);

        verify(root, never()).get("name");
    }

    @Test
    void buildSearchSpecification_blankCategoryId_ignored() {
        jakarta.persistence.criteria.Predicate truePred = mock(jakarta.persistence.criteria.Predicate.class);
        when(cb.and()).thenReturn(truePred);

        var spec = productSpecification.buildSearchSpecification(null, "  ", null, null, null, null);
        spec.toPredicate(root, query, cb);

        verify(root, never()).get("category");
    }

    @Test
    void buildSearchSpecification_allFilters_combined() {
        Path namePath = mock(Path.class);
        Path categoryPath = mock(Path.class);
        Path categoryIdPath = mock(Path.class);
        Path pricePath = mock(Path.class);
        Path isNewArrivalPath = mock(Path.class);
        Path ratingPath = mock(Path.class);
        jakarta.persistence.criteria.Predicate truePred = mock(jakarta.persistence.criteria.Predicate.class);

        when(root.get("name")).thenReturn(namePath);
        when(root.get("category")).thenReturn(categoryPath);
        when(categoryPath.get("id")).thenReturn(categoryIdPath);
        when(root.get("price")).thenReturn(pricePath);
        when(root.get("isNewArrival")).thenReturn(isNewArrivalPath);
        when(root.get("rating")).thenReturn(ratingPath);
        when(cb.lower(namePath)).thenReturn(namePath);
        when(cb.like(namePath, "%phone%")).thenReturn(mock(jakarta.persistence.criteria.Predicate.class));
        when(cb.equal(categoryIdPath, "cat-1")).thenReturn(mock(jakarta.persistence.criteria.Predicate.class));
        when(cb.greaterThanOrEqualTo(pricePath, new BigDecimal("50"))).thenReturn(mock(jakarta.persistence.criteria.Predicate.class));
        when(cb.lessThanOrEqualTo(pricePath, new BigDecimal("200"))).thenReturn(mock(jakarta.persistence.criteria.Predicate.class));
        when(cb.equal(isNewArrivalPath, false)).thenReturn(mock(jakarta.persistence.criteria.Predicate.class));
        when(cb.greaterThanOrEqualTo(ratingPath, 3.5f)).thenReturn(mock(jakarta.persistence.criteria.Predicate.class));
        when(cb.and(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(truePred);

        var spec = productSpecification.buildSearchSpecification("Phone", "cat-1",
                new BigDecimal("50"), new BigDecimal("200"), false, 3.5f);
        var result = spec.toPredicate(root, query, cb);

        assertThat(result).isEqualTo(truePred);
        verify(root).get("name");
        verify(root).get("category");
        verify(root, times(2)).get("price");
        verify(root).get("isNewArrival");
        verify(root).get("rating");
    }
}
