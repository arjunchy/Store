package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.request.ProductImageReorderRequest;
import com.ecommerce.backend.dto.request.ProductImageRequest;
import com.ecommerce.backend.dto.response.ProductImageResponse;
import com.ecommerce.backend.service.productimage.ProductImageService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/images")
@Slf4j
public class ProductImageController {

    @Autowired
    private ProductImageService productImageService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProductImageResponse> addImage(
            @PathVariable String productId,
            @Valid @RequestBody ProductImageRequest request) {
        log.info("POST /api/products/{}/images - Adding image with url: {}", productId, request.url());
        try {
            ProductImageResponse response = productImageService.addImage(productId, request);
            log.info("POST /api/products/{}/images - Successfully added image with id: {}", productId, response.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("POST /api/products/{}/images - Failed - {}", productId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("POST /api/products/{}/images - Unexpected error", productId, e);
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<List<ProductImageResponse>> getImages(@PathVariable String productId) {
        log.info("GET /api/products/{}/images - Fetching images", productId);
        try {
            List<ProductImageResponse> images = productImageService.getImagesForProduct(productId);
            log.debug("GET /api/products/{}/images - Retrieved {} images", productId, images.size());
            return ResponseEntity.ok(images);
        } catch (IllegalArgumentException e) {
            log.warn("GET /api/products/{}/images - Product not found", productId);
            throw e;
        } catch (Exception e) {
            log.error("GET /api/products/{}/images - Failed", productId, e);
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable String productId,
            @PathVariable String imageId) {
        log.info("DELETE /api/products/{}/images/{} - Deleting image", productId, imageId);
        try {
            productImageService.deleteImage(imageId);
            log.info("DELETE /api/products/{}/images/{} - Successfully deleted", productId, imageId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("DELETE /api/products/{}/images/{} - Failed - {}", productId, imageId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("DELETE /api/products/{}/images/{} - Unexpected error", productId, imageId, e);
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductImageResponse> uploadImage(
            @PathVariable String productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "altText", required = false) String altText,
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder,
            @RequestParam(value = "isPrimary", required = false, defaultValue = "false") boolean isPrimary) {
        log.info("POST /api/products/{}/images/upload - Uploading file: {} altText: {} isPrimary: {}",
                productId, file != null ? file.getOriginalFilename() : "null", altText, isPrimary);
        try {
            ProductImageResponse response = productImageService.uploadImage(productId, file, altText, displayOrder, isPrimary);
            log.info("POST /api/products/{}/images/upload - Successfully uploaded image with id: {}", productId, response.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("POST /api/products/{}/images/upload - Failed - {}", productId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("POST /api/products/{}/images/upload - Unexpected error", productId, e);
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{imageId}/primary")
    public ResponseEntity<ProductImageResponse> setPrimaryImage(
            @PathVariable String productId,
            @PathVariable String imageId) {
        log.info("PUT /api/products/{}/images/{}/primary - Setting primary", productId, imageId);
        try {
            ProductImageResponse response = productImageService.setPrimaryImage(productId, imageId);
            log.info("PUT /api/products/{}/images/{}/primary - Successfully set primary", productId, imageId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("PUT /api/products/{}/images/{}/primary - Failed - {}", productId, imageId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("PUT /api/products/{}/images/{}/primary - Unexpected error", productId, imageId, e);
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/reorder")
    public ResponseEntity<List<ProductImageResponse>> reorderImages(
            @PathVariable String productId,
            @Valid @RequestBody List<ProductImageReorderRequest> reorderRequests) {
        log.info("PUT /api/products/{}/images/reorder - Reordering {} images", productId, reorderRequests != null ? reorderRequests.size() : 0);
        try {
            List<ProductImageResponse> responses = productImageService.reorderImages(productId, reorderRequests);
            log.info("PUT /api/products/{}/images/reorder - Successfully reordered", productId);
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            log.warn("PUT /api/products/{}/images/reorder - Failed - {}", productId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("PUT /api/products/{}/images/reorder - Unexpected error", productId, e);
            throw e;
        }
    }

    @GetMapping("/primary")
    public ResponseEntity<ProductImageResponse> getPrimaryImage(@PathVariable String productId) {
        log.info("GET /api/products/{}/images/primary - Fetching primary", productId);
        try {
            ProductImageResponse response = productImageService.getPrimaryImage(productId);
            log.debug("GET /api/products/{}/images/primary - Found image with id: {}", productId, response.id());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("GET /api/products/{}/images/primary - Not found - {}", productId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("GET /api/products/{}/images/primary - Failed", productId, e);
            throw e;
        }
    }
}
