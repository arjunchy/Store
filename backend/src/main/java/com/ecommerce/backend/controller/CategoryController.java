package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.request.CategoryRequest;
import com.ecommerce.backend.dto.response.CategoryResponse;
import com.ecommerce.backend.service.category.CategoryService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        log.info("POST /api/categories - Creating category with name: {}", request.name());
        try {
            CategoryResponse created = categoryService.create(request);
            log.info("POST /api/categories - Successfully created category with id: {}", created.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("POST /api/categories - Create failed - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("POST /api/categories - Unexpected error during creation", e);
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll(
            @RequestParam(defaultValue = "false") boolean topLevelOnly) {
        log.info("GET /api/categories - Fetching categories, topLevelOnly={}", topLevelOnly);
        try {
            List<CategoryResponse> categories = categoryService.getAll(topLevelOnly);
            log.debug("GET /api/categories - Retrieved {} categories", categories.size());
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("GET /api/categories - Failed to fetch categories", e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getById(@PathVariable String id) {
        log.info("GET /api/categories/{} - Fetching category by id", id);
        try {
            CategoryResponse category = categoryService.getById(id);
            log.debug("GET /api/categories/{} - Found category with name: {}", id, category.name());
            return ResponseEntity.ok(category);
        } catch (IllegalArgumentException e) {
            log.warn("GET /api/categories/{} - Category not found", id);
            throw e;
        } catch (Exception e) {
            log.error("GET /api/categories/{} - Unexpected error", id, e);
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable String id,
            @Valid @RequestBody CategoryRequest request) {
        log.info("PUT /api/categories/{} - Updating category with name: {}", id, request.name());
        try {
            CategoryResponse updated = categoryService.update(id, request);
            log.info("PUT /api/categories/{} - Successfully updated", id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("PUT /api/categories/{} - Update failed - {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("PUT /api/categories/{} - Unexpected error during update", id, e);
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("DELETE /api/categories/{} - Deleting category", id);
        try {
            categoryService.delete(id);
            log.info("DELETE /api/categories/{} - Successfully deleted", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("DELETE /api/categories/{} - Delete failed - {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("DELETE /api/categories/{} - Unexpected error during delete", id, e);
            throw e;
        }
    }
}
