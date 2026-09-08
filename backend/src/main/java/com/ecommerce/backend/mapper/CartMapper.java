package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.response.CartItemResponse;
import com.ecommerce.backend.dto.response.CartResponse;
import com.ecommerce.backend.entity.Cart;
import com.ecommerce.backend.entity.CartItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartMapper {

    public CartItemResponse toCartItemResponse(CartItem item) {
        if (item == null || item.getProduct() == null) {
            return null;
        }
        BigDecimal price = item.getProduct().getPrice();
        int qty = item.getQuantity() != null ? item.getQuantity() : 0;
        BigDecimal subtotal = price.multiply(BigDecimal.valueOf(qty));
        return new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                price,
                qty,
                subtotal
        );
    }

    public CartResponse toCartResponse(Cart cart) {
        if (cart == null) {
            return null;
        }
        List<CartItemResponse> itemResponses = cart.getCartItems().stream()
                .map(this::toCartItemResponse)
                .collect(Collectors.toList());

        BigDecimal total = cart.getCartItems().stream()
                .map(item -> {
                    BigDecimal price = item.getProduct().getPrice();
                    int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                    return price.multiply(BigDecimal.valueOf(qty));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(
                cart.getId(),
                cart.getUser().getUserId(),
                itemResponses,
                total,
                cart.getCreatedAt()
        );
    }
}
