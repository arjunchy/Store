package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.response.WishlistResponse;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.entity.Wishlist;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WishlistMapperTest {

    private final WishlistMapper mapper = new WishlistMapper();

    @Test
    void toResponse_mapsAllFields() {
        Product product = Product.builder().id("prod-1").name("Laptop").build();
        User user = User.builder().userId("user-1").build();
        LocalDateTime now = LocalDateTime.now();
        Wishlist wishlist = Wishlist.builder()
                .id("wish-1")
                .user(user)
                .product(product)
                .createdAt(now)
                .build();

        WishlistResponse response = mapper.toResponse(wishlist);

        assertThat(response.id()).isEqualTo("wish-1");
        assertThat(response.productId()).isEqualTo("prod-1");
        assertThat(response.productName()).isEqualTo("Laptop");
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void toResponse_handlesNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void toResponse_handlesNullProduct() {
        Wishlist wishlist = Wishlist.builder()
                .id("wish-2")
                .user(User.builder().userId("user-1").build())
                .product(null)
                .createdAt(LocalDateTime.now())
                .build();

        WishlistResponse response = mapper.toResponse(wishlist);

        assertThat(response.productId()).isNull();
        assertThat(response.productName()).isNull();
    }
}
