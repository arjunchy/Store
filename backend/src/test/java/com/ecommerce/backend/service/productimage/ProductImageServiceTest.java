package com.ecommerce.backend.service.productimage;

import com.ecommerce.backend.dto.request.ProductImageRequest;
import com.ecommerce.backend.dto.response.ProductImageResponse;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.ProductImage;
import com.ecommerce.backend.mapper.ProductImageMapper;
import com.ecommerce.backend.repository.ProductImageRepository;
import com.ecommerce.backend.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductImageServiceTest {

    @Mock private ProductImageRepository productImageRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductImageMapper productImageMapper;

    @InjectMocks
    private ProductImageService productImageService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder().id("prod-1").name("Laptop").build();
    }

    @Test
    void addImage_success() {
        ProductImageRequest request = new ProductImageRequest("https://example.com/img.jpg", "Laptop", 1, true);
        ProductImage saved = ProductImage.builder().id("img-1").product(product).url("https://example.com/img.jpg").altText("Laptop").displayOrder(1).isPrimary(true).build();
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(productImageRepository.save(any())).thenReturn(saved);
        when(productImageMapper.toResponse(saved)).thenReturn(
                new ProductImageResponse("img-1", "prod-1", "https://example.com/img.jpg", "Laptop", 1, true)
        );

        ProductImageResponse response = productImageService.addImage("prod-1", request);

        assertThat(response.id()).isEqualTo("img-1");
        verify(productImageRepository).unsetOtherPrimary("prod-1");
    }

    @Test
    void addImage_notPrimary_skipsUnset() {
        ProductImageRequest request = new ProductImageRequest("https://example.com/img2.jpg", "Alt", 2, false);
        ProductImage saved = ProductImage.builder().id("img-2").product(product).isPrimary(false).build();
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(productImageRepository.save(any())).thenReturn(saved);
        when(productImageMapper.toResponse(saved)).thenReturn(
                new ProductImageResponse("img-2", "prod-1", "https://example.com/img2.jpg", "Alt", 2, false)
        );

        productImageService.addImage("prod-1", request);

        verify(productImageRepository, never()).unsetOtherPrimary(anyString());
    }

    @Test
    void addImage_productNotFound_throws() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.addImage("missing",
                new ProductImageRequest("url", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void getImagesForProduct_success() {
        ProductImage img = ProductImage.builder().id("img-1").product(product).url("url").build();
        when(productRepository.existsById("prod-1")).thenReturn(true);
        when(productImageRepository.findByProductId("prod-1")).thenReturn(List.of(img));
        when(productImageMapper.toResponse(img)).thenReturn(
                new ProductImageResponse("img-1", "prod-1", "url", null, 0, false)
        );

        List<ProductImageResponse> result = productImageService.getImagesForProduct("prod-1");

        assertThat(result).hasSize(1);
    }

    @Test
    void getImagesForProduct_empty() {
        when(productRepository.existsById("prod-1")).thenReturn(true);
        when(productImageRepository.findByProductId("prod-1")).thenReturn(List.of());

        List<ProductImageResponse> result = productImageService.getImagesForProduct("prod-1");

        assertThat(result).isEmpty();
    }

    @Test
    void getImagesForProduct_productNotFound_throws() {
        when(productRepository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> productImageService.getImagesForProduct("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void deleteImage_success() {
        ProductImage img = ProductImage.builder().id("img-1").product(product).build();
        when(productImageRepository.findById("img-1")).thenReturn(Optional.of(img));

        productImageService.deleteImage("img-1");

        verify(productImageRepository).delete(img);
    }

    @Test
    void deleteImage_notFound_throws() {
        when(productImageRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.deleteImage("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product image not found");
    }
}
