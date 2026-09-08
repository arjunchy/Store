package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.response.ProductImageResponse;
import com.ecommerce.backend.entity.ProductImage;
import org.springframework.stereotype.Component;

@Component
public class ProductImageMapper {

    public ProductImageResponse toResponse(ProductImage image) {
        if (image == null) {
            return null;
        }
        return new ProductImageResponse(
                image.getId(),
                image.getProduct() != null ? image.getProduct().getId() : null,
                image.getUrl(),
                image.getAltText(),
                image.getDisplayOrder(),
                image.getIsPrimary()
        );
    }
}
