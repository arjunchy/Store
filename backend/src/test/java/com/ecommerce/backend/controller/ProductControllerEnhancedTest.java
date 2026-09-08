package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.request.ProductRequest;
import com.ecommerce.backend.dto.response.ProductDetailResponse;
import com.ecommerce.backend.dto.response.ProductImageResponse;
import com.ecommerce.backend.dto.response.ProductResponse;
import com.ecommerce.backend.service.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerEnhancedTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private ProductRequest request;
    private ProductResponse productResponse;
    private ProductDetailResponse detailResponse;

    @BeforeEach
    void setUp() {
        request = new ProductRequest("Test", "Desc", new BigDecimal("100.00"), 10, false, "cat-1");
        productResponse = new ProductResponse("prod-1", "Test", "Desc", new BigDecimal("100.00"), 10, false, 0.0f, 0, "cat-1", LocalDateTime.now());
        detailResponse = new ProductDetailResponse(
                "prod-1", "Test", "Desc", new BigDecimal("100.00"), 10, false, 0.0f, 0, "cat-1",
                List.of(new ProductImageResponse("img-1", "prod-1", "http://localhost:8080/uploads/products/prod-1/file.jpg", "alt", 0, true)),
                LocalDateTime.now()
        );
    }

    @Test
    void createWithImages_success_returns201() {
        when(productService.createProductWithImages(any(), anyList())).thenReturn(productResponse);
        MockMultipartFile file = new MockMultipartFile("files", "test.jpg", "image/jpeg", "content".getBytes());
        ResponseEntity<ProductResponse> resp = productController.createWithImages(request, List.of(file));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().id()).isEqualTo("prod-1");
        verify(productService).createProductWithImages(request, List.of(file));
    }

    @Test
    void createWithImages_noFiles_returns201() {
        when(productService.createProductWithImages(any(), isNull())).thenReturn(productResponse);
        ResponseEntity<ProductResponse> resp = productController.createWithImages(request, null);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void createWithImages_serviceThrows_propagates() {
        when(productService.createProductWithImages(any(), any())).thenThrow(new IllegalArgumentException("Category not found"));
        MockMultipartFile file = new MockMultipartFile("files", "test.jpg", "image/jpeg", "content".getBytes());
        assertThatThrownBy(() -> productController.createWithImages(request, List.of(file)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateWithImages_success_returns200() {
        when(productService.updateProductWithImages(eq("prod-1"), any(), anyList())).thenReturn(productResponse);
        MockMultipartFile file = new MockMultipartFile("files", "test.jpg", "image/jpeg", "content".getBytes());
        ResponseEntity<ProductResponse> resp = productController.updateWithImages("prod-1", request, List.of(file));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().id()).isEqualTo("prod-1");
    }

    @Test
    void getProductDetail_success_returns200() {
        when(productService.getProductDetail("prod-1")).thenReturn(detailResponse);
        ResponseEntity<ProductDetailResponse> resp = productController.getProductDetail("prod-1");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().images()).hasSize(1);
        assertThat(resp.getBody().id()).isEqualTo("prod-1");
    }

    @Test
    void getProductDetail_notFound_propagates() {
        when(productService.getProductDetail("missing")).thenThrow(new IllegalArgumentException("Product not found"));
        assertThatThrownBy(() -> productController.getProductDetail("missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getStats_success_returns200() {
        when(productService.getTotalProducts()).thenReturn(10L);
        when(productService.getOutOfStockProducts()).thenReturn(2L);
        ResponseEntity<Map<String, Long>> resp = productController.getStats();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("totalProducts")).isEqualTo(10L);
        assertThat(resp.getBody().get("outOfStockProducts")).isEqualTo(2L);
    }
}
