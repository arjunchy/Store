package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.request.ProductRequest;
import com.ecommerce.backend.dto.response.ProductResponse;
import com.ecommerce.backend.service.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private ProductResponse sampleResponse;
    private ProductRequest sampleRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        sampleResponse = new ProductResponse(
                "prod-1",
                "Gaming Laptop",
                "High-performance",
                new BigDecimal("1500.00"),
                10,
                true,
                4.5f,
                12,
                "cat-1",
                LocalDateTime.now()
        );
        sampleRequest = new ProductRequest(
                "Gaming Laptop",
                "High-performance",
                new BigDecimal("1500.00"),
                10,
                true,
                "cat-1"
        );
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void create_success_returns201() {
        when(productService.create(any(ProductRequest.class))).thenReturn(sampleResponse);

        ResponseEntity<ProductResponse> response = productController.create(sampleRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(sampleResponse);
        verify(productService).create(sampleRequest);
    }

    @Test
    void create_withoutCategory_delegates() {
        ProductRequest req = new ProductRequest("No Cat", "Desc", new BigDecimal("100"), 1, false, null);
        ProductResponse resp = new ProductResponse("prod-2", "No Cat", "Desc", new BigDecimal("100"), 1, false, 0.0f, 0, null, LocalDateTime.now());
        when(productService.create(req)).thenReturn(resp);

        ResponseEntity<ProductResponse> response = productController.create(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().categoryId()).isNull();
    }

    @Test
    void create_propagatesCategoryNotFound() {
        ProductRequest req = new ProductRequest("Laptop", "Desc", new BigDecimal("100"), 1, false, "bad-cat");
        when(productService.create(any())).thenThrow(new IllegalArgumentException("Category not found"));

        assertThatThrownBy(() -> productController.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void getAll_success_returnsPage() {
        Page<ProductResponse> page = new PageImpl<>(List.of(sampleResponse), pageable, 1);
        when(productService.getAll(pageable)).thenReturn(page);

        ResponseEntity<Page<ProductResponse>> response = productController.getAll(pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).hasSize(1);
        verify(productService).getAll(pageable);
    }

    @Test
    void getAll_empty_returnsEmptyPage() {
        Page<ProductResponse> empty = new PageImpl<>(List.of(), pageable, 0);
        when(productService.getAll(pageable)).thenReturn(empty);

        ResponseEntity<Page<ProductResponse>> response = productController.getAll(pageable);

        assertThat(response.getBody().getContent()).isEmpty();
    }

    @Test
    void getByCategory_success() {
        Page<ProductResponse> page = new PageImpl<>(List.of(sampleResponse), pageable, 1);
        when(productService.getByCategory("cat-1", pageable)).thenReturn(page);

        ResponseEntity<Page<ProductResponse>> response = productController.getByCategory("cat-1", pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent().get(0).categoryId()).isEqualTo("cat-1");
        verify(productService).getByCategory("cat-1", pageable);
    }

    @Test
    void getByCategory_empty_returnsEmpty() {
        Page<ProductResponse> empty = new PageImpl<>(List.of(), pageable, 0);
        when(productService.getByCategory("cat-1", pageable)).thenReturn(empty);

        ResponseEntity<Page<ProductResponse>> response = productController.getByCategory("cat-1", pageable);

        assertThat(response.getBody().getContent()).isEmpty();
    }

    @Test
    void getById_success_returns200() {
        when(productService.getById("prod-1")).thenReturn(sampleResponse);

        ResponseEntity<ProductResponse> response = productController.getById("prod-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().id()).isEqualTo("prod-1");
        verify(productService).getById("prod-1");
    }

    @Test
    void getById_notFound_propagates() {
        when(productService.getById("missing")).thenThrow(new IllegalArgumentException("Product not found"));

        assertThatThrownBy(() -> productController.getById("missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_success_returns200() {
        ProductRequest updateReq = new ProductRequest("Updated", "New Desc", new BigDecimal("1400"), 5, false, "cat-1");
        ProductResponse updated = new ProductResponse("prod-1", "Updated", "New Desc", new BigDecimal("1400"), 5, false, 4.5f, 12, "cat-1", LocalDateTime.now());
        when(productService.update(eq("prod-1"), any(ProductRequest.class))).thenReturn(updated);

        ResponseEntity<ProductResponse> response = productController.update("prod-1", updateReq);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo("Updated");
        verify(productService).update("prod-1", updateReq);
    }

    @Test
    void update_propagatesCategoryNotFound() {
        ProductRequest req = new ProductRequest("New", "Desc", BigDecimal.ONE, 1, false, "bad-cat");
        when(productService.update(anyString(), any())).thenThrow(new IllegalArgumentException("Category not found"));

        assertThatThrownBy(() -> productController.update("prod-1", req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_propagatesProductNotFound() {
        ProductRequest req = new ProductRequest("New", "Desc", BigDecimal.ONE, 1, false, null);
        when(productService.update(eq("missing"), any())).thenThrow(new IllegalArgumentException("Product not found"));

        assertThatThrownBy(() -> productController.update("missing", req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_success_returns204() {
        doNothing().when(productService).delete("prod-1");

        ResponseEntity<Void> response = productController.delete("prod-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(productService).delete("prod-1");
    }

    @Test
    void delete_notFound_propagates() {
        doThrow(new IllegalArgumentException("Product not found")).when(productService).delete("missing");

        assertThatThrownBy(() -> productController.delete("missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_delegatesCorrectId() {
        doNothing().when(productService).delete("prod-99");

        productController.delete("prod-99");

        verify(productService).delete("prod-99");
    }
}
