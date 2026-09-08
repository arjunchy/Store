package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.response.ProductResponse;
import com.ecommerce.backend.entity.Category;
import com.ecommerce.backend.entity.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapper();

    @Test
    void toResponse_mapsAllFields() {
        Category category = Category.builder().id("cat-1").name("Electronics").build();
        LocalDateTime now = LocalDateTime.now();
        Product product = Product.builder()
                .id("prod-1")
                .name("Laptop")
                .description("High performance")
                .price(new BigDecimal("999.99"))
                .stockQuantity(50)
                .isNewArrival(true)
                .rating(4.5f)
                .reviewCount(10)
                .category(category)
                .createdAt(now)
                .build();

        ProductResponse response = mapper.toResponse(product);

        assertThat(response.id()).isEqualTo("prod-1");
        assertThat(response.name()).isEqualTo("Laptop");
        assertThat(response.description()).isEqualTo("High performance");
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("999.99"));
        assertThat(response.stockQuantity()).isEqualTo(50);
        assertThat(response.isNewArrival()).isTrue();
        assertThat(response.rating()).isEqualTo(4.5f);
        assertThat(response.reviewCount()).isEqualTo(10);
        assertThat(response.categoryId()).isEqualTo("cat-1");
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void toResponse_handlesNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void toResponse_handlesNullCategory() {
        Product product = Product.builder()
                .id("prod-2")
                .name("Phone")
                .price(BigDecimal.TEN)
                .stockQuantity(1)
                .isNewArrival(false)
                .rating(0.0f)
                .reviewCount(0)
                .category(null)
                .createdAt(LocalDateTime.now())
                .build();

        ProductResponse response = mapper.toResponse(product);

        assertThat(response.categoryId()).isNull();
        assertThat(response.name()).isEqualTo("Phone");
    }
}
