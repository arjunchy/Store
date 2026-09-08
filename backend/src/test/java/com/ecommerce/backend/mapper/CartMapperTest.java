package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.response.CartItemResponse;
import com.ecommerce.backend.dto.response.CartResponse;
import com.ecommerce.backend.entity.Cart;
import com.ecommerce.backend.entity.CartItem;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CartMapperTest {

    private final CartMapper mapper = new CartMapper();
    @Test
    void toCartItemResponse_mapsAllFields() {
        Product product = Product.builder()
                .id("prod-1")
                .name("Laptop")
                .price(new BigDecimal("999.99"))
                .build();
        CartItem item = CartItem.builder()
                .id("item-1")
                .product(product)
                .quantity(2)
                .build();

        CartItemResponse response = mapper.toCartItemResponse(item);

        assertThat(response.id()).isEqualTo("item-1");
        assertThat(response.productId()).isEqualTo("prod-1");
        assertThat(response.productName()).isEqualTo("Laptop");
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("999.99"));
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.subtotal()).isEqualByComparingTo(new BigDecimal("1999.98"));
    }

    @Test
    void toCartItemResponse_handlesNull() {
        assertThat(mapper.toCartItemResponse(null)).isNull();
    }

    @Test
    void toCartItemResponse_handlesNullProduct() {
        CartItem item = CartItem.builder()
                .id("item-2")
                .product(null)
                .quantity(1)
                .build();

        assertThat(mapper.toCartItemResponse(item)).isNull();
    }

    @Test
    void toCartItemResponse_handlesNullQuantity() {
        Product product = Product.builder()
                .id("prod-2")
                .name("Phone")
                .price(BigDecimal.TEN)
                .build();
        CartItem item = CartItem.builder()
                .id("item-3")
                .product(product)
                .quantity(null)
                .build();

        CartItemResponse response = mapper.toCartItemResponse(item);

        assertThat(response.quantity()).isEqualTo(0);
        assertThat(response.subtotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void toCartResponse_mapsAllFields() {
        User user = User.builder().userId("user-1").build();
        Product product1 = Product.builder().id("prod-1").name("Laptop").price(new BigDecimal("1000")).build();
        Product product2 = Product.builder().id("prod-2").name("Mouse").price(new BigDecimal("25")).build();
        CartItem item1 = CartItem.builder().id("item-1").product(product1).quantity(1).build();
        CartItem item2 = CartItem.builder().id("item-2").product(product2).quantity(4).build();
        LocalDateTime now = LocalDateTime.now();
        Cart cart = Cart.builder()
                .id("cart-1")
                .user(user)
                .cartItems(new ArrayList<>(List.of(item1, item2)))
                .createdAt(now)
                .build();

        CartResponse response = mapper.toCartResponse(cart);

        assertThat(response.id()).isEqualTo("cart-1");
        assertThat(response.userId()).isEqualTo("user-1");
        assertThat(response.items()).hasSize(2);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("1100.00"));
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void toCartResponse_handlesNull() {
        assertThat(mapper.toCartResponse(null)).isNull();
    }

    @Test
    void toCartResponse_emptyCart() {
        User user = User.builder().userId("user-1").build();
        Cart cart = Cart.builder()
                .id("cart-2")
                .user(user)
                .cartItems(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        CartResponse response = mapper.toCartResponse(cart);

        assertThat(response.items()).isEmpty();
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
