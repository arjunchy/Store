package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.response.ProductResponse;
import com.ecommerce.backend.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getIsNewArrival(),
                product.getRating(),
                product.getReviewCount(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCreatedAt()
        );
    }
}
