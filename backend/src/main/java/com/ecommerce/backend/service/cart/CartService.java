package com.ecommerce.backend.service.cart;

import com.ecommerce.backend.dto.request.CartItemRequest;
import com.ecommerce.backend.dto.response.CartResponse;

public interface CartService {

    CartResponse getCart(String userId);

    CartResponse addItem(String userId, CartItemRequest request);

    CartResponse updateItemQuantity(String userId, String cartItemId, Integer quantity);

    CartResponse removeItem(String userId, String cartItemId);

    CartResponse clearCart(String userId);
}
