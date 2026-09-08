package com.ecommerce.backend.service.productimage;

import com.ecommerce.backend.dto.request.ProductImageReorderRequest;
import com.ecommerce.backend.dto.response.ProductImageResponse;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.ProductImage;
import com.ecommerce.backend.mapper.ProductImageMapper;
import com.ecommerce.backend.repository.ProductImageRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.service.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductImageServiceEnhancedTest {

    @Mock private ProductImageRepository productImageRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductImageMapper productImageMapper;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks
    private ProductImageService productImageService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder().id("prod-1").name("Laptop").build();
        lenient().when(fileStorageService.getBaseUrl()).thenReturn("http://localhost:8080/uploads/");
    }

    @Test
    void uploadImage_success_primary() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(fileStorageService.storeFile(any(), eq("products/prod-1"))).thenReturn("http://localhost:8080/uploads/products/prod-1/uuid.jpg");
        ProductImage saved = ProductImage.builder().id("img-1").product(product).url("http://localhost:8080/uploads/products/prod-1/uuid.jpg").isPrimary(true).build();
        when(productImageRepository.save(any())).thenReturn(saved);
        when(productImageMapper.toResponse(saved)).thenReturn(new ProductImageResponse("img-1", "prod-1", "http://localhost:8080/uploads/products/prod-1/uuid.jpg", "alt", 0, true));

        ProductImageResponse resp = productImageService.uploadImage("prod-1", file, "alt", 0, true);

        assertThat(resp.isPrimary()).isTrue();
        verify(productImageRepository).unsetOtherPrimary("prod-1");
        verify(fileStorageService).storeFile(file, "products/prod-1");
    }

    @Test
    void uploadImage_notPrimary_skipsUnset() {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "content".getBytes());
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(fileStorageService.storeFile(any(), anyString())).thenReturn("http://localhost:8080/uploads/products/prod-1/uuid.png");
        ProductImage saved = ProductImage.builder().id("img-2").product(product).isPrimary(false).build();
        when(productImageRepository.save(any())).thenReturn(saved);
        when(productImageMapper.toResponse(saved)).thenReturn(new ProductImageResponse("img-2", "prod-1", "http://localhost:8080/uploads/products/prod-1/uuid.png", null, 1, false));

        productImageService.uploadImage("prod-1", file, null, 1, false);

        verify(productImageRepository, never()).unsetOtherPrimary(anyString());
    }

    @Test
    void uploadImage_emptyFile_throws() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
        assertThatThrownBy(() -> productImageService.uploadImage("prod-1", empty, null, 0, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File is required");
    }

    @Test
    void uploadImage_productNotFound_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());
        when(productRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productImageService.uploadImage("missing", file, null, 0, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void setPrimaryImage_success() {
        ProductImage img = ProductImage.builder().id("img-1").product(product).isPrimary(false).build();
        when(productRepository.existsById("prod-1")).thenReturn(true);
        when(productImageRepository.findById("img-1")).thenReturn(Optional.of(img));
        when(productImageRepository.save(any())).thenReturn(img);
        when(productImageMapper.toResponse(any())).thenReturn(new ProductImageResponse("img-1", "prod-1", "url", null, 0, true));

        ProductImageResponse resp = productImageService.setPrimaryImage("prod-1", "img-1");

        assertThat(resp.isPrimary()).isTrue();
        verify(productImageRepository).unsetOtherPrimary("prod-1");
        assertThat(img.getIsPrimary()).isTrue();
    }

    @Test
    void setPrimaryImage_productNotFound_throws() {
        when(productRepository.existsById("missing")).thenReturn(false);
        assertThatThrownBy(() -> productImageService.setPrimaryImage("missing", "img-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setPrimaryImage_imageNotBelongToProduct_throws() {
        Product otherProduct = Product.builder().id("prod-2").build();
        ProductImage img = ProductImage.builder().id("img-1").product(otherProduct).build();
        when(productRepository.existsById("prod-1")).thenReturn(true);
        when(productImageRepository.findById("img-1")).thenReturn(Optional.of(img));
        assertThatThrownBy(() -> productImageService.setPrimaryImage("prod-1", "img-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void getPrimaryImage_success() {
        ProductImage primary = ProductImage.builder().id("img-1").product(product).isPrimary(true).url("url").build();
        ProductImage other = ProductImage.builder().id("img-2").product(product).isPrimary(false).build();
        when(productRepository.existsById("prod-1")).thenReturn(true);
        when(productImageRepository.findByProductId("prod-1")).thenReturn(List.of(primary, other));
        when(productImageMapper.toResponse(primary)).thenReturn(new ProductImageResponse("img-1", "prod-1", "url", null, 0, true));

        ProductImageResponse resp = productImageService.getPrimaryImage("prod-1");
        assertThat(resp.id()).isEqualTo("img-1");
    }

    @Test
    void getPrimaryImage_notFound_throws() {
        ProductImage other = ProductImage.builder().id("img-2").product(product).isPrimary(false).build();
        when(productRepository.existsById("prod-1")).thenReturn(true);
        when(productImageRepository.findByProductId("prod-1")).thenReturn(List.of(other));
        assertThatThrownBy(() -> productImageService.getPrimaryImage("prod-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Primary image not found");
    }

    @Test
    void reorderImages_success() {
        ProductImage img1 = ProductImage.builder().id("img-1").product(product).displayOrder(0).build();
        ProductImage img2 = ProductImage.builder().id("img-2").product(product).displayOrder(1).build();
        when(productRepository.existsById("prod-1")).thenReturn(true);
        when(productImageRepository.findByProductId("prod-1"))
                .thenReturn(List.of(img1, img2))
                .thenReturn(List.of(img1, img2)); // second call for return sorted
        when(productImageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productImageMapper.toResponse(any())).thenAnswer(inv -> {
            ProductImage pi = inv.getArgument(0);
            return new ProductImageResponse(pi.getId(), "prod-1", pi.getUrl(), null, pi.getDisplayOrder(), false);
        });

        List<ProductImageReorderRequest> req = List.of(
                new ProductImageReorderRequest("img-1", 5),
                new ProductImageReorderRequest("img-2", 10)
        );

        List<ProductImageResponse> result = productImageService.reorderImages("prod-1", req);

        assertThat(result).hasSize(2);
        assertThat(img1.getDisplayOrder()).isEqualTo(5);
        assertThat(img2.getDisplayOrder()).isEqualTo(10);
    }

    @Test
    void deleteImage_managedUrl_deletesPhysicalFile() {
        ProductImage img = ProductImage.builder().id("img-1").product(product).url("http://localhost:8080/uploads/products/prod-1/file.jpg").build();
        when(productImageRepository.findById("img-1")).thenReturn(Optional.of(img));

        productImageService.deleteImage("img-1");

        verify(fileStorageService).deleteFile("http://localhost:8080/uploads/products/prod-1/file.jpg");
        verify(productImageRepository).delete(img);
    }

    @Test
    void deleteImage_externalUrl_skipsPhysicalDelete() {
        ProductImage img = ProductImage.builder().id("img-1").product(product).url("https://example.com/img.jpg").build();
        when(productImageRepository.findById("img-1")).thenReturn(Optional.of(img));

        productImageService.deleteImage("img-1");

        verify(fileStorageService, never()).deleteFile(anyString());
        verify(productImageRepository).delete(img);
    }

    @Test
    void deleteImage_fileDeleteFails_doesNotFailDbDelete() {
        ProductImage img = ProductImage.builder().id("img-1").product(product).url("http://localhost:8080/uploads/products/prod-1/file.jpg").build();
        when(productImageRepository.findById("img-1")).thenReturn(Optional.of(img));
        doThrow(new IllegalStateException("disk fail")).when(fileStorageService).deleteFile(anyString());

        // Should still delete DB record and not throw
        productImageService.deleteImage("img-1");

        verify(productImageRepository).delete(img);
    }
}
