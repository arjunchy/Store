package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.request.ProductRequest;
import com.ecommerce.backend.dto.response.ProductResponse;
import com.ecommerce.backend.entity.Category;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.mapper.ProductMapper;
import com.ecommerce.backend.repository.CategoryRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.service.product.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category category;
    private Product product;
    private ProductRequest createRequest;
    private ProductRequest updateRequest;
    private Pageable pageable;

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

        createRequest = new ProductRequest(
                "Gaming Laptop",
                "High-performance",
                new BigDecimal("1500.00"),
                10,
                true,
                "cat-1"
        );

        updateRequest = new ProductRequest(
                "Updated Laptop",
                "Updated desc",
                new BigDecimal("1400.00"),
                5,
                false,
                "cat-1"
        );

        pageable = PageRequest.of(0, 10);

        when(productMapper.toResponse(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            return new ProductResponse(
                    p.getId(),
                    p.getName(),
                    p.getDescription(),
                    p.getPrice(),
                    p.getStockQuantity(),
                    p.getIsNewArrival(),
                    p.getRating(),
                    p.getReviewCount(),
                    p.getCategory() != null ? p.getCategory().getId() : null,
                    p.getCreatedAt()
            );
        });
    }

    // --- create ---

    @Test
    void create_withCategory_success() {
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(category));
        Product saved = Product.builder().id("prod-1").name("Gaming Laptop").description("High-performance").price(new BigDecimal("1500.00")).stockQuantity(10).isNewArrival(true).category(category).build();
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse result = productService.create(createRequest);

        assertThat(result.name()).isEqualTo("Gaming Laptop");
        assertThat(result.categoryId()).isEqualTo("cat-1");
        verify(categoryRepository).findById("cat-1");
        verify(productRepository).save(argThat(p -> p.getCategory() != null && p.getCategory().getId().equals("cat-1") && p.getPrice().equals(new BigDecimal("1500.00"))));
    }

    @Test
    void create_withoutCategory_success() {
        ProductRequest req = new ProductRequest("No Cat", "Desc", new BigDecimal("100"), 1, false, null);
        Product saved = Product.builder().id("prod-2").name("No Cat").description("Desc").price(new BigDecimal("100")).stockQuantity(1).isNewArrival(false).build();
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse result = productService.create(req);

        assertThat(result.name()).isEqualTo("No Cat");
        assertThat(result.categoryId()).isNull();
        verify(categoryRepository, never()).findById(anyString());
        verify(productRepository).save(argThat(p -> p.getCategory() == null));
    }

    @Test
    void create_withBlankCategoryId_treatedAsNoCategory() {
        ProductRequest req = new ProductRequest("No Cat", "Desc", new BigDecimal("100"), 1, false, "   ");
        Product saved = Product.builder().id("prod-2").name("No Cat").build();
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse result = productService.create(req);

        assertThat(result.categoryId()).isNull();
        verify(categoryRepository, never()).findById(anyString());
    }

    @Test
    void create_categoryNotFound_throws() {
        when(categoryRepository.findById("bad-cat")).thenReturn(Optional.empty());
        ProductRequest req = new ProductRequest("Laptop", "Desc", new BigDecimal("100"), 1, false, "bad-cat");

        assertThatThrownBy(() -> productService.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");

        verify(productRepository, never()).save(any());
    }

    @Test
    void create_withNullStockAndIsNewArrival_defaults() {
        ProductRequest req = new ProductRequest("Laptop", "Desc", new BigDecimal("100"), null, null, null);
        Product saved = Product.builder().id("prod-3").name("Laptop").stockQuantity(0).isNewArrival(false).price(new BigDecimal("100")).description("Desc").build();
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse result = productService.create(req);

        assertThat(result.stockQuantity()).isEqualTo(0);
        assertThat(result.isNewArrival()).isFalse();
        verify(productRepository).save(argThat(p -> p.getStockQuantity() == 0 && p.getIsNewArrival() == false));
    }

    // --- getById ---

    @Test
    void getById_success() {
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        ProductResponse result = productService.getById("prod-1");

        assertThat(result.id()).isEqualTo("prod-1");
        assertThat(result.name()).isEqualTo("Gaming Laptop");
        verify(productRepository).findById("prod-1");
    }

    @Test
    void getById_notFound_throws() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void getById_softDeleted_filteredByWhere_throws() {
        when(productRepository.findById("deleted")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById("deleted"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- getAll ---

    @Test
    void getAll_success_mapsPage() {
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
        when(productRepository.findAll(pageable)).thenReturn(page);

        Page<ProductResponse> result = productService.getAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Gaming Laptop");
        verify(productRepository).findAll(pageable);
    }

    @Test
    void getAll_empty_returnsEmptyPage() {
        Page<Product> empty = new PageImpl<>(List.of(), pageable, 0);
        when(productRepository.findAll(pageable)).thenReturn(empty);

        Page<ProductResponse> result = productService.getAll(pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    // --- getByCategory ---

    @Test
    void getByCategory_success() {
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
        when(productRepository.findByCategoryId("cat-1", pageable)).thenReturn(page);

        Page<ProductResponse> result = productService.getByCategory("cat-1", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).categoryId()).isEqualTo("cat-1");
        verify(productRepository).findByCategoryId("cat-1", pageable);
    }

    @Test
    void getByCategory_empty_returnsEmpty() {
        Page<Product> empty = new PageImpl<>(List.of(), pageable, 0);
        when(productRepository.findByCategoryId("cat-1", pageable)).thenReturn(empty);

        Page<ProductResponse> result = productService.getByCategory("cat-1", pageable);

        assertThat(result.getContent()).isEmpty();
    }

    // --- update ---

    @Test
    void update_success_withoutCategoryChange() {
        Product existing = Product.builder().id("prod-1").name("Old").description("Old").price(new BigDecimal("100")).stockQuantity(1).isNewArrival(false).category(category).build();
        ProductRequest req = new ProductRequest("Updated", "New Desc", new BigDecimal("200"), 5, true, null);
        Product saved = Product.builder().id("prod-1").name("Updated").description("New Desc").price(new BigDecimal("200")).stockQuantity(5).isNewArrival(true).category(null).build();
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse result = productService.update("prod-1", req);

        assertThat(result.name()).isEqualTo("Updated");
        assertThat(result.categoryId()).isNull();
        verify(productRepository).save(argThat(p -> p.getCategory() == null && p.getName().equals("Updated") && p.getPrice().equals(new BigDecimal("200"))));
    }

    @Test
    void update_success_withNewCategory() {
        Product existing = Product.builder().id("prod-1").name("Old").description("Old").price(BigDecimal.ONE).stockQuantity(1).isNewArrival(false).build();
        Category newCategory = Category.builder().id("cat-2").name("Books").build();
        ProductRequest req = new ProductRequest("Updated", "Desc", new BigDecimal("200"), 5, false, "cat-2");
        Product saved = Product.builder().id("prod-1").name("Updated").category(newCategory).price(new BigDecimal("200")).stockQuantity(5).isNewArrival(false).description("Desc").build();
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(existing));
        when(categoryRepository.findById("cat-2")).thenReturn(Optional.of(newCategory));
        when(productRepository.save(any())).thenReturn(saved);

        ProductResponse result = productService.update("prod-1", req);

        assertThat(result.categoryId()).isEqualTo("cat-2");
        verify(categoryRepository).findById("cat-2");
    }

    @Test
    void update_withBlankCategoryId_removesCategory() {
        Product existing = Product.builder().id("prod-1").name("Old").category(category).price(BigDecimal.ONE).stockQuantity(1).isNewArrival(false).description("Old").build();
        ProductRequest req = new ProductRequest("Updated", "Desc", new BigDecimal("100"), 1, false, "  ");
        Product saved = Product.builder().id("prod-1").name("Updated").category(null).price(new BigDecimal("100")).stockQuantity(1).isNewArrival(false).description("Desc").build();
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenReturn(saved);

        ProductResponse result = productService.update("prod-1", req);

        assertThat(result.categoryId()).isNull();
        verify(categoryRepository, never()).findById(anyString());
    }

    @Test
    void update_notFound_throws() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());
        ProductRequest req = new ProductRequest("New", "Desc", BigDecimal.ONE, 1, false, null);

        assertThatThrownBy(() -> productService.update("missing", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void update_categoryNotFound_throws() {
        Product existing = Product.builder().id("prod-1").name("Old").price(BigDecimal.ONE).stockQuantity(1).description("Old").build();
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(existing));
        ProductRequest req = new ProductRequest("Updated", "Desc", BigDecimal.ONE, 1, false, "bad-cat");
        when(categoryRepository.findById("bad-cat")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update("prod-1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");

        verify(productRepository, never()).save(any());
    }

    @Test
    void update_withNullStockAndIsNewArrival_defaults() {
        Product existing = Product.builder().id("prod-1").name("Old").price(BigDecimal.ONE).stockQuantity(5).isNewArrival(true).description("Old").build();
        ProductRequest req = new ProductRequest("Updated", "Desc", BigDecimal.TEN, null, null, null);
        Product saved = Product.builder().id("prod-1").name("Updated").price(BigDecimal.TEN).stockQuantity(0).isNewArrival(false).description("Desc").build();
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenReturn(saved);

        ProductResponse result = productService.update("prod-1", req);

        assertThat(result.stockQuantity()).isEqualTo(0);
        assertThat(result.isNewArrival()).isFalse();
    }

    // --- delete ---

    @Test
    void delete_success() {
        when(productRepository.existsById("prod-1")).thenReturn(true);
        doNothing().when(productRepository).deleteById("prod-1");

        productService.delete("prod-1");

        verify(productRepository).existsById("prod-1");
        verify(productRepository).deleteById("prod-1");
    }

    @Test
    void delete_notFound_throws() {
        when(productRepository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> productService.delete("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");

        verify(productRepository, never()).deleteById(anyString());
    }
}
