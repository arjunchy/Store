package com.ecommerce.backend.service.product;

import com.ecommerce.backend.dto.request.ProductRequest;
import com.ecommerce.backend.dto.response.ProductDetailResponse;
import com.ecommerce.backend.dto.response.ProductImageResponse;
import com.ecommerce.backend.dto.response.ProductResponse;
import com.ecommerce.backend.entity.Category;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.ProductImage;
import com.ecommerce.backend.mapper.ProductImageMapper;
import com.ecommerce.backend.mapper.ProductMapper;
import com.ecommerce.backend.repository.CategoryRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceEnhancedTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductMapper productMapper;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private ProductImageMapper productImageMapper;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category category;
    private Product product;
    private ProductRequest request;

    @BeforeEach
    void setUp() {
        category = Category.builder().id("cat-1").name("Electronics").build();
        product = Product.builder()
                .id("prod-1")
                .name("Gaming Laptop")
                .description("High-performance")
                .price(new BigDecimal("1500.00"))
                .stockQuantity(10)
                .isNewArrival(true)
                .rating(4.5f)
                .reviewCount(12)
                .category(category)
                .createdAt(LocalDateTime.now())
                .build();

        request = new ProductRequest(
                "Gaming Laptop",
                "High-performance",
                new BigDecimal("1500.00"),
                10,
                true,
                "cat-1"
        );

        lenient().when(productMapper.toResponse(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            return new ProductResponse(
                    p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                    p.getStockQuantity(), p.getIsNewArrival(), p.getRating(),
                    p.getReviewCount(), p.getCategory() != null ? p.getCategory().getId() : null,
                    p.getCreatedAt()
            );
        });

        lenient().when(productImageMapper.toResponse(any(ProductImage.class))).thenAnswer(inv -> {
            ProductImage pi = inv.getArgument(0);
            return new ProductImageResponse(
                    pi.getId(), pi.getProduct() != null ? pi.getProduct().getId() : "prod-1",
                    pi.getUrl(), pi.getAltText(), pi.getDisplayOrder(), pi.getIsPrimary()
            );
        });
    }

    // --- createProductWithImages ---

    @Test
    void createProductWithImages_noFiles_delegatesToCreate() {
        Product saved = Product.builder().id("prod-1").name("Gaming Laptop").price(new BigDecimal("1500.00")).stockQuantity(10).isNewArrival(true).category(category).description("High-performance").build();
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(saved);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(saved));

        ProductResponse result = productService.createProductWithImages(request, null);

        assertThat(result.name()).isEqualTo("Gaming Laptop");
        verify(productRepository, atLeastOnce()).save(any());
        verify(fileStorageService, never()).storeFile(any(), anyString());
    }

    @Test
    void createProductWithImages_withFiles_storesAndCreatesImages() {
        Product saved = Product.builder().id("prod-1").name("Gaming Laptop").price(new BigDecimal("1500.00")).stockQuantity(10).isNewArrival(true).category(category).description("High-performance").build();
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(saved);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(saved));
        when(fileStorageService.storeFile(any(), eq("products/prod-1"))).thenReturn("http://localhost:8080/uploads/products/prod-1/file1.jpg")
                .thenReturn("http://localhost:8080/uploads/products/prod-1/file2.jpg");
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(inv -> {
            ProductImage pi = inv.getArgument(0);
            pi.setId("img-" + pi.getDisplayOrder());
            return pi;
        });

        MockMultipartFile f1 = new MockMultipartFile("files", "f1.jpg", "image/jpeg", "content1".getBytes());
        MockMultipartFile f2 = new MockMultipartFile("files", "f2.jpg", "image/jpeg", "content2".getBytes());

        ProductResponse result = productService.createProductWithImages(request, List.of(f1, f2));

        assertThat(result).isNotNull();
        verify(fileStorageService, times(2)).storeFile(any(), eq("products/prod-1"));
        verify(productImageRepository, times(2)).save(any(ProductImage.class));
        verify(productImageRepository).unsetOtherPrimary("prod-1");
    }

    @Test
    void createProductWithImages_emptyFile_skipped() {
        Product saved = Product.builder().id("prod-1").name("Gaming Laptop").price(new BigDecimal("1500.00")).stockQuantity(10).isNewArrival(true).category(category).description("High-performance").build();
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(saved);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(saved));
        MockMultipartFile empty = new MockMultipartFile("files", "empty.jpg", "image/jpeg", new byte[0]);
        MockMultipartFile valid = new MockMultipartFile("files", "valid.jpg", "image/jpeg", "content".getBytes());
        when(fileStorageService.storeFile(any(), anyString())).thenReturn("http://localhost:8080/uploads/products/prod-1/valid.jpg");
        when(productImageRepository.save(any())).thenAnswer(inv -> {
            ProductImage pi = inv.getArgument(0);
            pi.setId("img-1");
            return pi;
        });

        productService.createProductWithImages(request, List.of(empty, valid));

        verify(fileStorageService, times(1)).storeFile(any(), anyString());
        verify(productImageRepository, times(1)).save(any());
    }

    // --- getProductDetail ---

    @Test
    void getProductDetail_success_withImages() {
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        ProductImage img1 = ProductImage.builder().id("img-1").product(product).url("http://localhost:8080/uploads/products/prod-1/1.jpg").displayOrder(0).isPrimary(true).build();
        ProductImage img2 = ProductImage.builder().id("img-2").product(product).url("http://localhost:8080/uploads/products/prod-1/2.jpg").displayOrder(1).isPrimary(false).build();
        when(productImageRepository.findByProductId("prod-1")).thenReturn(List.of(img1, img2));

        ProductDetailResponse detail = productService.getProductDetail("prod-1");

        assertThat(detail.id()).isEqualTo("prod-1");
        assertThat(detail.images()).hasSize(2);
        assertThat(detail.images().get(0).isPrimary()).isTrue();
        assertThat(detail.name()).isEqualTo("Gaming Laptop");
    }

    @Test
    void getProductDetail_noImages_returnsEmptyList() {
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductId("prod-1")).thenReturn(List.of());

        ProductDetailResponse detail = productService.getProductDetail("prod-1");

        assertThat(detail.images()).isEmpty();
    }

    @Test
    void getProductDetail_productNotFound_throws() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.getProductDetail("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    // --- stats ---

    @Test
    void getTotalProducts_returnsCount() {
        when(productRepository.count()).thenReturn(42L);
        assertThat(productService.getTotalProducts()).isEqualTo(42L);
        verify(productRepository).count();
    }

    @Test
    void getOutOfStockProducts_returnsCount() {
        when(productRepository.countByStockQuantity(0)).thenReturn(5L);
        assertThat(productService.getOutOfStockProducts()).isEqualTo(5L);
        verify(productRepository).countByStockQuantity(0);
    }

    // --- updateProductWithImages ---

    @Test
    void updateProductWithImages_withFiles_addsImages() {
        Product existing = Product.builder().id("prod-1").name("Old").description("Old").price(new BigDecimal("100")).stockQuantity(1).isNewArrival(false).category(category).build();
        Product saved = Product.builder().id("prod-1").name("Updated").description("New").price(new BigDecimal("200")).stockQuantity(5).isNewArrival(false).category(category).build();
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(existing)).thenReturn(Optional.of(saved));
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(saved);
        when(productImageRepository.findByProductId("prod-1")).thenReturn(List.of()); // no existing images
        when(fileStorageService.storeFile(any(), eq("products/prod-1"))).thenReturn("http://localhost:8080/uploads/products/prod-1/new.jpg");
        when(productImageRepository.save(any())).thenAnswer(inv -> {
            ProductImage pi = inv.getArgument(0);
            pi.setId("img-new");
            return pi;
        });

        ProductRequest updateReq = new ProductRequest("Updated", "New", new BigDecimal("200"), 5, false, "cat-1");
        MockMultipartFile f = new MockMultipartFile("files", "new.jpg", "image/jpeg", "content".getBytes());

        ProductResponse result = productService.updateProductWithImages("prod-1", updateReq, List.of(f));

        assertThat(result).isNotNull();
        verify(fileStorageService).storeFile(any(), eq("products/prod-1"));
        verify(productImageRepository).save(any(ProductImage.class));
    }

    @Test
    void updateProductWithImages_noFiles_delegatesToUpdateOnly() {
        Product existing = Product.builder().id("prod-1").name("Old").description("Old").price(new BigDecimal("100")).stockQuantity(1).isNewArrival(false).build();
        Product saved = Product.builder().id("prod-1").name("Updated").description("New").price(new BigDecimal("200")).stockQuantity(5).isNewArrival(false).build();
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(anyString())).thenReturn(Optional.of(category));
        when(productRepository.save(any())).thenReturn(saved);

        ProductRequest updateReq = new ProductRequest("Updated", "New", new BigDecimal("200"), 5, false, "cat-1");
        ProductResponse result = productService.updateProductWithImages("prod-1", updateReq, null);

        assertThat(result.name()).isEqualTo("Updated");
        verify(fileStorageService, never()).storeFile(any(), anyString());
    }
}
