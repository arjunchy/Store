package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.request.ProductImageReorderRequest;
import com.ecommerce.backend.dto.response.ProductImageResponse;
import com.ecommerce.backend.service.productimage.ProductImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImageControllerEnhancedTest {

    @Mock
    private ProductImageService productImageService;

    @InjectMocks
    private ProductImageController productImageController;

    private ProductImageResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new ProductImageResponse("img-1", "prod-1", "http://localhost:8080/uploads/products/prod-1/uuid.jpg", "alt", 0, true);
    }

    @Test
    void uploadImage_success_returns201() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());
        when(productImageService.uploadImage(eq("prod-1"), any(), eq("alt"), eq(0), eq(true))).thenReturn(sampleResponse);

        ResponseEntity<ProductImageResponse> resp = productImageController.uploadImage("prod-1", file, "alt", 0, true);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().id()).isEqualTo("img-1");
        verify(productImageService).uploadImage("prod-1", file, "alt", 0, true);
    }

    @Test
    void uploadImage_productNotFound_propagates() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());
        when(productImageService.uploadImage(anyString(), any(), any(), any(), anyBoolean())).thenThrow(new IllegalArgumentException("Product not found"));

        assertThatThrownBy(() -> productImageController.uploadImage("missing", file, null, null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setPrimaryImage_success_returns200() {
        when(productImageService.setPrimaryImage("prod-1", "img-1")).thenReturn(sampleResponse);

        ResponseEntity<ProductImageResponse> resp = productImageController.setPrimaryImage("prod-1", "img-1");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().id()).isEqualTo("img-1");
    }

    @Test
    void setPrimaryImage_notFound_propagates() {
        when(productImageService.setPrimaryImage(anyString(), anyString())).thenThrow(new IllegalArgumentException("Product image not found"));
        assertThatThrownBy(() -> productImageController.setPrimaryImage("prod-1", "missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reorderImages_success_returns200() {
        List<ProductImageReorderRequest> req = List.of(new ProductImageReorderRequest("img-1", 0), new ProductImageReorderRequest("img-2", 1));
        List<ProductImageResponse> mockList = List.of(sampleResponse);
        when(productImageService.reorderImages("prod-1", req)).thenReturn(mockList);

        ResponseEntity<List<ProductImageResponse>> resp = productImageController.reorderImages("prod-1", req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
    }

    @Test
    void reorderImages_productNotFound_propagates() {
        List<ProductImageReorderRequest> req = List.of(new ProductImageReorderRequest("img-1", 0));
        when(productImageService.reorderImages(anyString(), any())).thenThrow(new IllegalArgumentException("Product not found"));
        assertThatThrownBy(() -> productImageController.reorderImages("missing", req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getPrimaryImage_success_returns200() {
        when(productImageService.getPrimaryImage("prod-1")).thenReturn(sampleResponse);
        ResponseEntity<ProductImageResponse> resp = productImageController.getPrimaryImage("prod-1");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().isPrimary()).isTrue();
    }

    @Test
    void getPrimaryImage_notFound_propagates() {
        when(productImageService.getPrimaryImage(anyString())).thenThrow(new IllegalArgumentException("Primary image not found"));
        assertThatThrownBy(() -> productImageController.getPrimaryImage("prod-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
