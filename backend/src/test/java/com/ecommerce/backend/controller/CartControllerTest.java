package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.request.CartItemRequest;
import com.ecommerce.backend.dto.response.CartItemResponse;
import com.ecommerce.backend.dto.response.CartResponse;
import com.ecommerce.backend.service.cart.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    @Mock
    private CustomUserDetails userDetails;

    private CartResponse sampleCartResponse;

    @BeforeEach
    void setUp() {
        CartItemResponse item = new CartItemResponse("item-1", "prod-1", "Laptop", new BigDecimal("999.99"), 2, new BigDecimal("1999.98"));
        sampleCartResponse = new CartResponse("cart-1", "user-1", List.of(item), new BigDecimal("1999.98"), LocalDateTime.now());
    }

    @Test
    void addItem_success_returns200() {
        when(userDetails.getUserId()).thenReturn("user-1");
        CartItemRequest request = new CartItemRequest("prod-1", 2);
        when(cartService.addItem(eq("user-1"), any(CartItemRequest.class))).thenReturn(sampleCartResponse);

        ResponseEntity<CartResponse> response = cartController.addItem(request, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().id()).isEqualTo("cart-1");
        verify(cartService).addItem("user-1", request);
    }

    @Test
    void addItem_productNotFound_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        CartItemRequest request = new CartItemRequest("missing", 1);
        when(cartService.addItem(eq("user-1"), any())).thenThrow(new IllegalArgumentException("Product not found"));

        assertThatThrownBy(() -> cartController.addItem(request, userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateItem_success_returns200() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(cartService.updateItemQuantity("user-1", "item-1", 5)).thenReturn(sampleCartResponse);

        ResponseEntity<CartResponse> response = cartController.updateItem("item-1", Map.of("quantity", 5), userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(cartService).updateItemQuantity("user-1", "item-1", 5);
    }

    @Test
    void updateItem_nullQuantity_throws() {
        when(userDetails.getUserId()).thenReturn("user-1");

        assertThatThrownBy(() -> cartController.updateItem("item-1", Map.of(), userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity is required");
    }

    @Test
    void updateItem_notFound_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(cartService.updateItemQuantity("user-1", "missing", 1)).thenThrow(new IllegalArgumentException("Cart item not found"));

        assertThatThrownBy(() -> cartController.updateItem("missing", Map.of("quantity", 1), userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeItem_success_returns200() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(cartService.removeItem("user-1", "item-1")).thenReturn(sampleCartResponse);

        ResponseEntity<CartResponse> response = cartController.removeItem("item-1", userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(cartService).removeItem("user-1", "item-1");
    }

    @Test
    void removeItem_notFound_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(cartService.removeItem("user-1", "missing")).thenThrow(new IllegalArgumentException("Cart item not found"));

        assertThatThrownBy(() -> cartController.removeItem("missing", userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getCart_success_returns200() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(cartService.getCart("user-1")).thenReturn(sampleCartResponse);

        ResponseEntity<CartResponse> response = cartController.getCart(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().items()).hasSize(1);
    }

    @Test
    void clearCart_success_returns200() {
        CartResponse emptyCart = new CartResponse("cart-1", "user-1", List.of(), BigDecimal.ZERO, LocalDateTime.now());
        when(userDetails.getUserId()).thenReturn("user-1");
        when(cartService.clearCart("user-1")).thenReturn(emptyCart);

        ResponseEntity<CartResponse> response = cartController.clearCart(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().items()).isEmpty();
    }
}
