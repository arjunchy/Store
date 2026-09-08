package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.request.ProductImageRequest;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImageControllerTest {

    @Mock
    private ProductImageService productImageService;

    @InjectMocks
    private ProductImageController productImageController;

    private ProductImageResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new ProductImageResponse("img-1", "prod-1", "https://example.com/img.jpg", "Laptop image", 1, true);
    }

    @Test
    void addImage_success_returns201() {
        when(productImageService.addImage(eq("prod-1"), any(ProductImageRequest.class))).thenReturn(sampleResponse);

        ProductImageRequest request = new ProductImageRequest("https://example.com/img.jpg", "Laptop image", 1, true);
        ResponseEntity<ProductImageResponse> response = productImageController.addImage("prod-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().id()).isEqualTo("img-1");
        verify(productImageService).addImage("prod-1", request);
    }

    @Test
    void addImage_productNotFound_propagates() {
        when(productImageService.addImage(eq("missing"), any())).thenThrow(new IllegalArgumentException("Product not found"));

        assertThatThrownBy(() -> productImageController.addImage("missing",
                new ProductImageRequest("url", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getImages_success_returns200() {
        when(productImageService.getImagesForProduct("prod-1")).thenReturn(List.of(sampleResponse));

        ResponseEntity<List<ProductImageResponse>> response = productImageController.getImages("prod-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).url()).isEqualTo("https://example.com/img.jpg");
    }

    @Test
    void getImages_empty_returnsEmptyList() {
        when(productImageService.getImagesForProduct("prod-1")).thenReturn(List.of());

        ResponseEntity<List<ProductImageResponse>> response = productImageController.getImages("prod-1");

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getImages_productNotFound_propagates() {
        when(productImageService.getImagesForProduct("missing")).thenThrow(new IllegalArgumentException("Product not found"));

        assertThatThrownBy(() -> productImageController.getImages("missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteImage_success_returns204() {
        doNothing().when(productImageService).deleteImage("img-1");

        ResponseEntity<Void> response = productImageController.deleteImage("prod-1", "img-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(productImageService).deleteImage("img-1");
    }

    @Test
    void deleteImage_notFound_propagates() {
        doThrow(new IllegalArgumentException("Product image not found")).when(productImageService).deleteImage("missing");

        assertThatThrownBy(() -> productImageController.deleteImage("prod-1", "missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
