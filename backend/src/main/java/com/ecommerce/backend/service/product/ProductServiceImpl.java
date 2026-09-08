package com.ecommerce.backend.service.product;

import com.ecommerce.backend.dto.request.ProductRequest;
import com.ecommerce.backend.dto.response.ProductDetailResponse;
import com.ecommerce.backend.dto.response.ProductImageResponse;
import com.ecommerce.backend.dto.response.ProductResponse;
import com.ecommerce.backend.dto.ProductSearchCriteria;
import com.ecommerce.backend.entity.Category;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.ProductImage;
import com.ecommerce.backend.repository.CategoryRepository;
import com.ecommerce.backend.repository.ProductImageRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.mapper.ProductImageMapper;
import com.ecommerce.backend.mapper.ProductMapper;
import com.ecommerce.backend.service.storage.FileStorageService;
import com.ecommerce.backend.specification.ProductSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductSpecification productSpecification;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductImageMapper productImageMapper;

    @Autowired(required = false)
    private FileStorageService fileStorageService;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        log.info("Creating product with name: {} and categoryId: {}", request.name(), request.categoryId());
        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity() != null ? request.stockQuantity() : 0);
        product.setIsNewArrival(request.isNewArrival() != null ? request.isNewArrival() : false);

        if (request.categoryId() != null && !request.categoryId().isBlank()) {
            log.debug("Looking up category with id: {}", request.categoryId());
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> {
                        log.warn("Category not found with id: {}", request.categoryId());
                        return new IllegalArgumentException("Category not found");
                    });
            product.setCategory(category);
            log.debug("Set category {} for product {}", category.getId(), request.name());
        } else {
            product.setCategory(null);
            log.debug("Creating product without category (top-level)");
        }

        Product saved = productRepository.save(product);
        log.info("Successfully created product with id: {} and name: {}", saved.getId(), saved.getName());
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(String id) {
        log.debug("Fetching product by id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found with id: {}", id);
                    return new IllegalArgumentException("Product not found");
                });
        log.debug("Found product with id: {} and name: {}", product.getId(), product.getName());
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(Pageable pageable) {
        log.debug("Fetching all products with pageable: {}", pageable);
        if (pageable.getPageSize() > 100) throw new IllegalArgumentException("Page size must be <=100");
        if (pageable.getSort().isSorted()) {
            java.util.Set<String> allowedSortFields = java.util.Set.of("name", "price", "createdAt", "rating", "stockQuantity", "reviewCount");
            for (Sort.Order order : pageable.getSort()) {
                if (!allowedSortFields.contains(order.getProperty())) {
                    throw new IllegalArgumentException("Invalid sort field: " + order.getProperty());
                }
            }
        }
        Page<Product> page = productRepository.findAll(pageable);
        log.info("Retrieved {} products (total {} across {} pages)", page.getNumberOfElements(), page.getTotalElements(), page.getTotalPages());
        return page.map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getByCategory(String categoryId, Pageable pageable) {
        log.debug("Fetching products for categoryId: {} with pageable: {}", categoryId, pageable);
        Page<Product> page = productRepository.findByCategoryId(categoryId, pageable);
        log.info("Retrieved {} products for category {}", page.getNumberOfElements(), categoryId);
        return page.map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> search(ProductSearchCriteria criteria, Pageable pageable) {
        log.info("Searching products with criteria: name={}, categoryId={}, minPrice={}, maxPrice={}, isNewArrival={}, minRating={}",
                criteria.name(), criteria.categoryId(), criteria.minPrice(), criteria.maxPrice(), criteria.isNewArrival(), criteria.minRating());
        try {
            var spec = productSpecification.buildSearchSpecification(
                    criteria.name(),
                    criteria.categoryId(),
                    criteria.minPrice(),
                    criteria.maxPrice(),
                    criteria.isNewArrival(),
                    criteria.minRating()
            );

            if (pageable.getPageSize() > 100) throw new IllegalArgumentException("Page size must be <=100");
            Pageable sortedPageable = pageable;
            if (pageable.getSort().isSorted()) {
                java.util.Set<String> allowedSortFields = java.util.Set.of("name", "price", "createdAt", "rating", "stockQuantity", "reviewCount");
                for (Sort.Order order : pageable.getSort()) {
                    if (!allowedSortFields.contains(order.getProperty())) {
                        throw new IllegalArgumentException("Invalid sort field: " + order.getProperty());
                    }
                }
            }
            if (criteria.sortBy() != null && !criteria.sortBy().isBlank()) {
                java.util.Set<String> allowedSortFields = java.util.Set.of("name", "price", "createdAt", "rating", "stockQuantity", "reviewCount");
                String sortField = criteria.sortBy();
                if (!allowedSortFields.contains(sortField)) {
                    throw new IllegalArgumentException("Invalid sort field: " + criteria.sortBy());
                }
                Sort.Direction direction = "desc".equalsIgnoreCase(criteria.sortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC;
                sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(direction, sortField));
            } else if (pageable.getSort().isSorted()) {
                sortedPageable = pageable;
            }

            Page<Product> page = productRepository.findAll(spec, sortedPageable);
            log.info("Search returned {} products (total {} across {} pages)", page.getNumberOfElements(), page.getTotalElements(), page.getTotalPages());
            return page.map(productMapper::toResponse);
        } catch (Exception e) {
            log.error("Failed to search products with criteria", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ProductResponse update(String id, ProductRequest request) {
        log.info("Updating product with id: {} and name: {}", id, request.name());
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found for update with id: {}", id);
                    return new IllegalArgumentException("Product not found");
                });

        existing.setName(request.name());
        existing.setDescription(request.description());
        existing.setPrice(request.price());
        existing.setStockQuantity(request.stockQuantity() != null ? request.stockQuantity() : 0);
        existing.setIsNewArrival(request.isNewArrival() != null ? request.isNewArrival() : false);
        log.debug("Updated fields for product id: {}", id);

        if (request.categoryId() != null && !request.categoryId().isBlank()) {
            log.debug("Looking up category with id: {} for product update", request.categoryId());
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> {
                        log.warn("Category not found with id: {}", request.categoryId());
                        return new IllegalArgumentException("Category not found");
                    });
            existing.setCategory(category);
            log.debug("Set new category {} for product {}", category.getId(), id);
        } else {
            existing.setCategory(null);
            log.debug("Removing category for product id: {}", id);
        }

        Product saved = productRepository.save(existing);
        log.info("Successfully updated product with id: {} and name: {}", saved.getId(), saved.getName());
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(String id) {
        log.info("Deleting product with id: {}", id);
        if (!productRepository.existsById(id)) {
            log.warn("Delete failed - product not found with id: {}", id);
            throw new IllegalArgumentException("Product not found");
        }
        productRepository.deleteById(id);
        log.info("Successfully soft-deleted product with id: {} (via @SQLDelete)", id);
    }

    @Override
    @Transactional
    public ProductResponse createProductWithImages(ProductRequest request, List<MultipartFile> files) {
        log.info("Creating product with images: name={} files={}", request.name(), files != null ? files.size() : 0);
        if (files != null && files.size() > 10) throw new IllegalArgumentException("Max 10 images allowed");
        ProductResponse created = create(request);
        if (files == null || files.isEmpty()) {
            log.debug("No files provided for productId: {}", created.id());
            return created;
        }

        if (fileStorageService == null) {
            log.warn("FileStorageService not available, skipping image upload for productId: {}", created.id());
            return created;
        }

        Product product = productRepository.findById(created.id())
                .orElseThrow(() -> new IllegalArgumentException("Product not found after creation"));

        int savedCount = 0;
        int index = 0;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                log.warn("Skipping empty file at index {} for productId: {}", index, created.id());
                index++;
                continue;
            }
            try {
                String url = fileStorageService.storeFile(file, "products/" + product.getId());
                boolean isPrimary = savedCount == 0;
                if (isPrimary) {
                    productImageRepository.unsetOtherPrimary(product.getId());
                }
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .url(url)
                        .altText(product.getName())
                        .displayOrder(savedCount)
                        .isPrimary(isPrimary)
                        .build();
                productImageRepository.save(image);
                log.info("Saved image {} (savedCount {}) for productId: {} url: {} isPrimary: {}", index, savedCount, product.getId(), url, isPrimary);
                savedCount++;
            } catch (Exception e) {
                log.error("Failed to store file at index {} for productId: {}", index, product.getId(), e);
                throw new IllegalStateException("Failed to store image: " + e.getMessage(), e);
            }
            index++;
        }
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProductWithImages(String id, ProductRequest request, List<MultipartFile> files) {
        log.info("Updating product with images: id={} files={}", id, files != null ? files.size() : 0);
        if (files != null && files.size() > 10) throw new IllegalArgumentException("Max 10 images allowed");
        ProductResponse updated = update(id, request);
        if (files == null || files.isEmpty()) {
            log.debug("No files provided for update of productId: {}", id);
            return updated;
        }

        if (fileStorageService == null) {
            log.warn("FileStorageService not available, skipping image upload for productId: {}", id);
            return updated;
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found after update"));

        List<ProductImage> existingImages = productImageRepository.findByProductId(id);
        boolean hasPrimary = existingImages.stream().anyMatch(img -> Boolean.TRUE.equals(img.getIsPrimary()));
        int startOrder = existingImages.size();

        int savedCount = 0;
        int index = 0;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                log.warn("Skipping empty file at index {} for productId: {}", index, id);
                index++;
                continue;
            }
            try {
                String url = fileStorageService.storeFile(file, "products/" + id);
                boolean isPrimary = !hasPrimary && savedCount == 0;
                if (isPrimary) {
                    productImageRepository.unsetOtherPrimary(id);
                }
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .url(url)
                        .altText(product.getName())
                        .displayOrder(startOrder + savedCount)
                        .isPrimary(isPrimary)
                        .build();
                productImageRepository.save(image);
                log.info("Saved additional image {} (savedCount {}) for productId: {} url: {}", index, savedCount, id, url);
                if (isPrimary) {
                    hasPrimary = true;
                }
                savedCount++;
            } catch (Exception e) {
                log.error("Failed to store file at index {} for productId: {}", index, id, e);
                throw new IllegalStateException("Failed to store image: " + e.getMessage(), e);
            }
            index++;
        }
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(String id) {
        log.debug("Fetching product detail for id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found with id: {}", id);
                    return new IllegalArgumentException("Product not found");
                });
        List<ProductImage> images = productImageRepository.findByProductId(id);
        List<ProductImageResponse> imageResponses = images.stream()
                .map(productImageMapper::toResponse)
                .collect(Collectors.toList());
        log.info("Retrieved product detail for id: {} with {} images", id, imageResponses.size());
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getIsNewArrival(),
                product.getRating(),
                product.getReviewCount(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                imageResponses,
                product.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalProducts() {
        long count = productRepository.count();
        log.debug("Total products: {}", count);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public long getOutOfStockProducts() {
        long count = productRepository.countByStockQuantity(0);
        log.debug("Out of stock products: {}", count);
        return count;
    }
}
