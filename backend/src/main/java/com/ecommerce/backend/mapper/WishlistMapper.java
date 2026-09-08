package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.response.WishlistResponse;
import com.ecommerce.backend.entity.Wishlist;
import org.springframework.stereotype.Component;

@Component
public class WishlistMapper {

    public WishlistResponse toResponse(Wishlist wishlist) {
        if (wishlist == null) {
            return null;
        }
        return new WishlistResponse(
                wishlist.getId(),
                wishlist.getProduct() != null ? wishlist.getProduct().getId() : null,
                wishlist.getProduct() != null ? wishlist.getProduct().getName() : null,
                wishlist.getCreatedAt()
        );
    }
}
