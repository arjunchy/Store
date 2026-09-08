package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.response.ProductImageResponse;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.ProductImage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductImageMapperTest {

    private final ProductImageMapper mapper = new ProductImageMapper();

    @Test
    void toResponse_mapsAllFields() {
        Product product = Product.builder().id("prod-1").name("Laptop").build();
        ProductImage image = ProductImage.builder()
                .id("img-1")
                .product(product)
                .url("https://example.com/img.jpg")
                .altText("Laptop image")
                .displayOrder(1)
                .isPrimary(true)
                .build();

        ProductImageResponse response = mapper.toResponse(image);

        assertThat(response.id()).isEqualTo("img-1");
        assertThat(response.productId()).isEqualTo("prod-1");
        assertThat(response.url()).isEqualTo("https://example.com/img.jpg");
        assertThat(response.altText()).isEqualTo("Laptop image");
        assertThat(response.displayOrder()).isEqualTo(1);
        assertThat(response.isPrimary()).isTrue();
    }

    @Test
    void toResponse_handlesNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void toResponse_handlesNullProduct() {
        ProductImage image = ProductImage.builder()
                .id("img-2")
                .product(null)
                .url("https://example.com/img2.jpg")
                .altText(null)
                .displayOrder(0)
                .isPrimary(false)
                .build();

        ProductImageResponse response = mapper.toResponse(image);

        assertThat(response.productId()).isNull();
        assertThat(response.altText()).isNull();
        assertThat(response.isPrimary()).isFalse();
    }
}
