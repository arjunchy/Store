package com.ecommerce.backend.service.cart;

import com.ecommerce.backend.dto.request.CartItemRequest;
import com.ecommerce.backend.dto.response.CartResponse;
import com.ecommerce.backend.entity.Cart;
import com.ecommerce.backend.entity.CartItem;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.mapper.CartMapper;
import com.ecommerce.backend.repository.CartItemRepository;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    private User user;
    private Product product;
    private Cart cart;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        user = User.builder().userId("user-1").build();
        product = Product.builder().id("prod-1").name("Laptop").price(new BigDecimal("999.99")).stockQuantity(10).build();
        cart = Cart.builder().id("cart-1").user(user).cartItems(new ArrayList<>()).build();
        cartItem = CartItem.builder().id("item-1").cart(cart).product(product).quantity(2).build();
        cart.getCartItems().add(cartItem);
    }

    @Test
    void getCart_existingCart_success() {
        when(cartRepository.findByUserIdWithItems("user-1")).thenReturn(Optional.of(cart));
        when(cartMapper.toCartResponse(cart)).thenReturn(mock(CartResponse.class));

        CartResponse response = cartService.getCart("user-1");

        assertThat(response).isNotNull();
        verify(cartRepository).findByUserIdWithItems("user-1");
    }

    @Test
    void getCart_noCart_createsNew() {
        Cart newCart = Cart.builder().id("cart-new").user(user).cartItems(new ArrayList<>()).build();
        when(cartRepository.findByUserIdWithItems("user-1")).thenReturn(Optional.empty());
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);
        when(cartMapper.toCartResponse(newCart)).thenReturn(mock(CartResponse.class));

        CartResponse response = cartService.getCart("user-1");

        assertThat(response).isNotNull();
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void getCart_userNotFound_throws() {
        when(cartRepository.findByUserIdWithItems("missing")).thenReturn(Optional.empty());
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getCart("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void addItem_newItem_success() {
        when(cartRepository.findByUserIdWithItems("user-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("prod-2")).thenReturn(Optional.of(
                Product.builder().id("prod-2").name("Mouse").price(new BigDecimal("25.00")).stockQuantity(50).build()));
        when(cartItemRepository.findByCartIdAndProductId("cart-1", "prod-2")).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(
                CartItem.builder().id("item-2").product(product).quantity(3).build());
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(cart));
        when(cartMapper.toCartResponse(any())).thenReturn(mock(CartResponse.class));

        CartResponse response = cartService.addItem("user-1", new CartItemRequest("prod-2", 3));

        assertThat(response).isNotNull();
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addItem_existingItem_increasesQuantity() {
        when(cartRepository.findByUserIdWithItems("user-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId("cart-1", "prod-1")).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(any())).thenReturn(cartItem);
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(cart));
        when(cartMapper.toCartResponse(any())).thenReturn(mock(CartResponse.class));

        CartResponse response = cartService.addItem("user-1", new CartItemRequest("prod-1", 1));

        assertThat(response).isNotNull();
        assertThat(cartItem.getQuantity()).isEqualTo(3);
    }

    @Test
    void addItem_productNotFound_throws() {
        when(cartRepository.findByUserIdWithItems("user-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem("user-1", new CartItemRequest("missing", 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void addItem_invalidQuantity_throws() {
        when(cartRepository.findByUserIdWithItems("user-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem("user-1", new CartItemRequest("prod-1", 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be > 0");
    }

    @Test
    void updateItemQuantity_success() {
        when(cartRepository.findByUserIdWithItems("user-1")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById("item-1")).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(any())).thenReturn(cartItem);
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(cart));
        when(cartMapper.toCartResponse(any())).thenReturn(mock(CartResponse.class));

        CartResponse response = cartService.updateItemQuantity("user-1", "item-1", 5);

        assertThat(response).isNotNull();
        assertThat(cartItem.getQuantity()).isEqualTo(5);
    }

    @Test
    void updateItemQuantity_zeroQuantity_deletes() {
        when(cartRepository.findByUserIdWithItems("user-1")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById("item-1")).thenReturn(Optional.of(cartItem));
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(cart));
        when(cartMapper.toCartResponse(any())).thenReturn(mock(CartResponse.class));

        cartService.updateItemQuantity("user-1", "item-1", 0);

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void updateItemQuantity_notFound_throws() {
        when(cartRepository.findByUserIdWithItems("user-1")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItemQuantity("user-1", "missing", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cart item not found");
    }

    @Test
    void updateItemQuantity_wrongCart_throws() {
        Cart otherCart = Cart.builder().id("cart-other").user(user).cartItems(new ArrayList<>()).build();
        CartItem otherItem = CartItem.builder().id("item-other").cart(otherCart).product(product).quantity(1).build();
        when(cartRepository.findByUserIdWithItems("user-1")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById("item-other")).thenReturn(Optional.of(otherItem));

        assertThatThrownBy(() -> cartService.updateItemQuantity("user-1", "item-other", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cart item not found");
    }

    @Test
    void removeItem_success() {
        when(cartRepository.findByUserIdWithItems("user-1")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById("item-1")).thenReturn(Optional.of(cartItem));
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(cart));
        when(cartMapper.toCartResponse(any())).thenReturn(mock(CartResponse.class));

        cartService.removeItem("user-1", "item-1");

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void removeItem_notFound_throws() {
        when(cartRepository.findByUserIdWithItems("user-1")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem("user-1", "missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cart item not found");
    }

    @Test
    void clearCart_success() {
        when(cartRepository.findByUserIdWithItems("user-1")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenReturn(cart);
        when(cartMapper.toCartResponse(any())).thenReturn(mock(CartResponse.class));

        CartResponse response = cartService.clearCart("user-1");

        assertThat(response).isNotNull();
        verify(cartRepository).save(cart);
    }

    @Test
    void clearCart_emptyCart() {
        Cart emptyCart = Cart.builder().id("cart-2").user(user).cartItems(new ArrayList<>()).build();
        when(cartRepository.findByUserIdWithItems("user-1")).thenReturn(Optional.of(emptyCart));
        when(cartRepository.save(any())).thenReturn(emptyCart);
        when(cartMapper.toCartResponse(any())).thenReturn(mock(CartResponse.class));

        CartResponse response = cartService.clearCart("user-1");

        assertThat(response).isNotNull();
    }
}
