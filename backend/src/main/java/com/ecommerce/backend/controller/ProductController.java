package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.request.ProductRequest;
import com.ecommerce.backend.dto.response.ProductDetailResponse;
import com.ecommerce.backend.dto.response.ProductResponse;
import com.ecommerce.backend.dto.ProductSearchCriteria;
import com.ecommerce.backend.service.product.ProductService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@Slf4j
public class ProductController {

    @Autowired
    private ProductService productService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        log.info("POST /api/products - Creating product with name: {}", request.name());
        try {
            ProductResponse created = productService.create(request);
            log.info("POST /api/products - Successfully created product with id: {}", created.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("POST /api/products - Create failed - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("POST /api/products - Unexpected error during creation", e);
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAll(Pageable pageable) {
        log.info("GET /api/products - Fetching products with pageable: {}", pageable);
        try {
            Page<ProductResponse> page = productService.getAll(pageable);
            log.debug("GET /api/products - Retrieved {} products", page.getNumberOfElements());
            return ResponseEntity.ok(page);
        } catch (Exception e) {
            log.error("GET /api/products - Failed to fetch products", e);
            throw e;
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) Boolean isNewArrival,
            @RequestParam(required = false) Float minRating,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDirection,
            Pageable pageable) {
        log.info("GET /api/products/search - Searching with name={}, categoryId={}, minPrice={}, maxPrice={}, isNewArrival={}, minRating={}",
                name, categoryId, minPrice, maxPrice, isNewArrival, minRating);
        try {
            ProductSearchCriteria criteria = new ProductSearchCriteria(
                    name, categoryId, minPrice, maxPrice, isNewArrival, minRating, sortBy, sortDirection
            );
            Page<ProductResponse> page = productService.search(criteria, pageable);
            log.debug("GET /api/products/search - Found {} products", page.getNumberOfElements());
            return ResponseEntity.ok(page);
        } catch (Exception e) {
            log.error("GET /api/products/search - Failed to search products", e);
            throw e;
        }
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProductResponse>> getByCategory(
            @PathVariable String categoryId, Pageable pageable) {
        log.info("GET /api/products/category/{} - Fetching products for category", categoryId);
        try {
            Page<ProductResponse> page = productService.getByCategory(categoryId, pageable);
            log.debug("GET /api/products/category/{} - Retrieved {} products", categoryId, page.getNumberOfElements());
            return ResponseEntity.ok(page);
        } catch (Exception e) {
            log.error("GET /api/products/category/{} - Failed to fetch products", categoryId, e);
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        log.info("GET /api/products/stats - Fetching product stats (ADMIN)");
        try {
            Map<String, Long> stats = new HashMap<>();
            stats.put("totalProducts", productService.getTotalProducts());
            stats.put("outOfStockProducts", productService.getOutOfStockProducts());
            log.debug("GET /api/products/stats - total={}, outOfStock={}", stats.get("totalProducts"), stats.get("outOfStockProducts"));
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("GET /api/products/stats - Failed", e);
            throw e;
        }
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<ProductDetailResponse> getProductDetail(@PathVariable String id) {
        log.info("GET /api/products/{}/details - Fetching product detail", id);
        try {
            ProductDetailResponse detail = productService.getProductDetail(id);
            log.debug("GET /api/products/{}/details - Found with {} images", id, detail.images() != null ? detail.images().size() : 0);
            return ResponseEntity.ok(detail);
        } catch (IllegalArgumentException e) {
            log.warn("GET /api/products/{}/details - Product not found", id);
            throw e;
        } catch (Exception e) {
            log.error("GET /api/products/{}/details - Unexpected error", id, e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable String id) {
        log.info("GET /api/products/{} - Fetching product by id", id);
        try {
            ProductResponse product = productService.getById(id);
            log.debug("GET /api/products/{} - Found product with name: {}", id, product.name());
            return ResponseEntity.ok(product);
        } catch (IllegalArgumentException e) {
            log.warn("GET /api/products/{} - Product not found", id);
            throw e;
        } catch (Exception e) {
            log.error("GET /api/products/{} - Unexpected error", id, e);
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request) {
        log.info("PUT /api/products/{} - Updating product with name: {}", id, request.name());
        try {
            ProductResponse updated = productService.update(id, request);
            log.info("PUT /api/products/{} - Successfully updated", id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("PUT /api/products/{} - Update failed - {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("PUT /api/products/{} - Unexpected error during update", id, e);
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("DELETE /api/products/{} - Deleting product", id);
        try {
            productService.delete(id);
            log.info("DELETE /api/products/{} - Successfully soft-deleted", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("DELETE /api/products/{} - Delete failed - {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("DELETE /api/products/{} - Unexpected error during delete", id, e);
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/with-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> createWithImages(
            @RequestPart("product") @Valid ProductRequest request,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        log.info("POST /api/products/with-images - Creating product with images: {} files={}", request.name(), files != null ? files.size() : 0);
        try {
            ProductResponse created = productService.createProductWithImages(request, files);
            log.info("POST /api/products/with-images - Successfully created with id: {}", created.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("POST /api/products/with-images - Failed - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("POST /api/products/with-images - Unexpected error", e);
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/{id}/with-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> updateWithImages(
            @PathVariable String id,
            @RequestPart("product") @Valid ProductRequest request,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        log.info("PUT /api/products/{}/with-images - Updating with images: {} files={}", id, request.name(), files != null ? files.size() : 0);
        try {
            ProductResponse updated = productService.updateProductWithImages(id, request, files);
            log.info("PUT /api/products/{}/with-images - Successfully updated", id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("PUT /api/products/{}/with-images - Failed - {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("PUT /api/products/{}/with-images - Unexpected error", id, e);
            throw e;
        }
    }
}
