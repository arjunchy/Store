package com.ecommerce.backend.service.productimage;

import com.ecommerce.backend.dto.request.ProductImageReorderRequest;
import com.ecommerce.backend.dto.request.ProductImageRequest;
import com.ecommerce.backend.dto.response.ProductImageResponse;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.ProductImage;
import com.ecommerce.backend.repository.ProductImageRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.mapper.ProductImageMapper;
import com.ecommerce.backend.service.storage.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductImageService {

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageMapper productImageMapper;

    @Autowired(required = false)
    private FileStorageService fileStorageService;

    @Transactional
    public ProductImageResponse addImage(String productId, ProductImageRequest request) {
        log.info("Adding image for productId: {} with url: {}", productId, request.url());
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> {
                        log.warn("Product not found with id: {}", productId);
                        return new IllegalArgumentException("Product not found");
                    });

            if (Boolean.TRUE.equals(request.isPrimary())) {
                log.debug("isPrimary=true, unsetting other primary images for productId: {}", productId);
                productImageRepository.unsetOtherPrimary(productId);
            }

            ProductImage image = ProductImage.builder()
                    .product(product)
                    .url(request.url())
                    .altText(request.altText())
                    .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                    .isPrimary(Boolean.TRUE.equals(request.isPrimary()))
                    .build();

            ProductImage saved = productImageRepository.save(image);
            log.info("Successfully added image with id: {} for productId: {}", saved.getId(), productId);
            return productImageMapper.toResponse(saved);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to add image for productId: {}", productId, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<ProductImageResponse> getImagesForProduct(String productId) {
        log.debug("Fetching images for productId: {}", productId);
        try {
            if (!productRepository.existsById(productId)) {
                log.warn("Product not found with id: {}", productId);
                throw new IllegalArgumentException("Product not found");
            }
            List<ProductImage> images = productImageRepository.findByProductId(productId);
            log.info("Retrieved {} images for productId: {}", images.size(), productId);
            return images.stream()
                    .map(productImageMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch images for productId: {}", productId, e);
            throw e;
        }
    }

    @Transactional
    public void deleteImage(String imageId) {
        log.info("Deleting product image with id: {}", imageId);
        try {
            ProductImage image = productImageRepository.findById(imageId)
                    .orElseThrow(() -> {
                        log.warn("Product image not found with id: {}", imageId);
                        return new IllegalArgumentException("Product image not found");
                    });

            if (fileStorageService != null && image.getUrl() != null) {
                try {
                    String url = image.getUrl();
                    boolean isManaged = url.contains("/uploads/") ||
                            (fileStorageService.getBaseUrl() != null && url.startsWith(fileStorageService.getBaseUrl()));
                    if (isManaged) {
                        log.debug("Deleting physical file for imageId: {} url: {}", imageId, url);
                        fileStorageService.deleteFile(url);
                    } else {
                        log.debug("Skipping physical delete for external URL: {}", url);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to delete physical file for imageId: {} - {}", imageId, ex.getMessage());
                }
            }

            productImageRepository.delete(image);
            log.info("Successfully deleted product image with id: {}", imageId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete product image with id: {}", imageId, e);
            throw e;
        }
    }

    @Transactional
    public ProductImageResponse uploadImage(String productId, MultipartFile file, String altText, Integer displayOrder, boolean isPrimary) {
        log.info("Uploading image for productId: {} altText: {} displayOrder: {} isPrimary: {} file: {}",
                productId, altText, displayOrder, isPrimary, file != null ? file.getOriginalFilename() : "null");
        try {
            if (file == null || file.isEmpty()) {
                log.warn("Upload failed - file is empty for productId: {}", productId);
                throw new IllegalArgumentException("File is required");
            }

            if (fileStorageService == null) {
                log.error("FileStorageService not available for upload");
                throw new IllegalStateException("File storage not configured");
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> {
                        log.warn("Product not found with id: {}", productId);
                        return new IllegalArgumentException("Product not found");
                    });

            String url = fileStorageService.storeFile(file, "products/" + productId);
            log.debug("Stored file for productId: {} -> url: {}", productId, url);

            if (isPrimary) {
                log.debug("isPrimary=true, unsetting other primary images for productId: {}", productId);
                productImageRepository.unsetOtherPrimary(productId);
            }

            ProductImage image = ProductImage.builder()
                    .product(product)
                    .url(url)
                    .altText(altText)
                    .displayOrder(displayOrder != null ? displayOrder : 0)
                    .isPrimary(isPrimary)
                    .build();

            ProductImage saved = productImageRepository.save(image);
            log.info("Successfully uploaded image with id: {} for productId: {} url: {}", saved.getId(), productId, url);
            return productImageMapper.toResponse(saved);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload image for productId: {}", productId, e);
            throw new IllegalStateException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    @Transactional
    public ProductImageResponse setPrimaryImage(String productId, String imageId) {
        log.info("Setting primary image for productId: {} imageId: {}", productId, imageId);
        try {
            if (!productRepository.existsById(productId)) {
                log.warn("Product not found with id: {}", productId);
                throw new IllegalArgumentException("Product not found");
            }

            ProductImage image = productImageRepository.findById(imageId)
                    .orElseThrow(() -> {
                        log.warn("Product image not found with id: {}", imageId);
                        return new IllegalArgumentException("Product image not found");
                    });

            if (!image.getProduct().getId().equals(productId)) {
                log.warn("Image {} does not belong to product {}", imageId, productId);
                throw new IllegalArgumentException("Image does not belong to product");
            }

            productImageRepository.unsetOtherPrimary(productId);
            image.setIsPrimary(true);
            ProductImage saved = productImageRepository.save(image);
            log.info("Successfully set primary image {} for productId: {}", imageId, productId);
            return productImageMapper.toResponse(saved);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to set primary image {} for productId: {}", imageId, productId, e);
            throw e;
        }
    }

    @Transactional
    public List<ProductImageResponse> reorderImages(String productId, List<ProductImageReorderRequest> reorderRequests) {
        log.info("Reordering images for productId: {} with {} entries", productId, reorderRequests != null ? reorderRequests.size() : 0);
        try {
            if (!productRepository.existsById(productId)) {
                log.warn("Product not found with id: {}", productId);
                throw new IllegalArgumentException("Product not found");
            }

            if (reorderRequests == null || reorderRequests.isEmpty()) {
                log.warn("Reorder requests empty for productId: {}", productId);
                throw new IllegalArgumentException("Reorder list cannot be empty");
            }

            java.util.Set<String> reqIds = reorderRequests.stream()
                    .map(ProductImageReorderRequest::imageId)
                    .collect(Collectors.toSet());
            if (reqIds.size() != reorderRequests.size()) {
                throw new IllegalArgumentException("Duplicate imageId in reorder list");
            }

            List<ProductImage> images = productImageRepository.findByProductId(productId);
            if (images.isEmpty()) {
                log.warn("No images found for productId: {}", productId);
                throw new IllegalArgumentException("No images found for product");
            }
            java.util.Set<String> existingIds = images.stream().map(ProductImage::getId).collect(Collectors.toSet());
            for (String rid : reqIds) {
                if (!existingIds.contains(rid)) {
                    throw new IllegalArgumentException("Unknown imageId: " + rid);
                }
            }
            if (reqIds.size() != existingIds.size()) {
                log.warn("Reorder for productId: {} does not include all images: expected {} got {}", productId, existingIds.size(), reqIds.size());
                throw new IllegalArgumentException("Reorder must include all images for product (" + existingIds.size() + " expected, " + reqIds.size() + " provided)");
            }

            Map<String, Integer> orderMap = reorderRequests.stream()
                    .collect(Collectors.toMap(
                            ProductImageReorderRequest::imageId,
                            ProductImageReorderRequest::displayOrder,
                            (a, b) -> b
                    ));

            List<ProductImage> updated = new ArrayList<>();
            for (ProductImage img : images) {
                Integer newOrder = orderMap.get(img.getId());
                if (newOrder != null) {
                    img.setDisplayOrder(newOrder);
                    updated.add(productImageRepository.save(img));
                    log.debug("Updated displayOrder for image {} to {}", img.getId(), newOrder);
                }
            }

            List<ProductImage> sorted = productImageRepository.findByProductId(productId);
            log.info("Successfully reordered {} images for productId: {}", updated.size(), productId);
            return sorted.stream()
                    .map(productImageMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to reorder images for productId: {}", productId, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public ProductImageResponse getPrimaryImage(String productId) {
        log.debug("Fetching primary image for productId: {}", productId);
        try {
            if (!productRepository.existsById(productId)) {
                log.warn("Product not found with id: {}", productId);
                throw new IllegalArgumentException("Product not found");
            }
            List<ProductImage> images = productImageRepository.findByProductId(productId);
            return images.stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .findFirst()
                    .map(productImageMapper::toResponse)
                    .orElseThrow(() -> {
                        log.warn("No primary image found for productId: {}", productId);
                        return new IllegalArgumentException("Primary image not found");
                    });
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch primary image for productId: {}", productId, e);
            throw e;
        }
    }
}
