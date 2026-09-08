package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.request.CartItemRequest;
import com.ecommerce.backend.dto.response.CartResponse;
import com.ecommerce.backend.service.cart.CartService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@Slf4j
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @Valid @RequestBody CartItemRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "POST /api/cart/items - userId: {} productId: {} quantity: {}",
                userId,
                request.productId(),
                request.quantity()
        );

        try {
            CartResponse response =
                    cartService.addItem(userId, request);

            log.info(
                    "POST /api/cart/items - Added item for userId: {} cartId: {}",
                    userId,
                    response.id()
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            log.warn(
                    "POST /api/cart/items - Failed for userId: {} - {}",
                    userId,
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "POST /api/cart/items - Unexpected error for userId: {}",
                    userId,
                    e
            );

            throw e;
        }
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateItem(
            @PathVariable String cartItemId,
            @RequestBody Map<String, Integer> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();
        Integer quantity = body.get("quantity");

        log.info(
                "PUT /api/cart/items/{} - userId: {} quantity: {}",
                cartItemId,
                userId,
                quantity
        );

        try {
            if (quantity == null) {
                log.warn(
                        "PUT /api/cart/items/{} - quantity missing",
                        cartItemId
                );

                throw new IllegalArgumentException(
                        "Quantity is required"
                );
            }

            CartResponse response =
                    cartService.updateItemQuantity(
                            userId,
                            cartItemId,
                            quantity
                    );

            log.info(
                    "PUT /api/cart/items/{} - Updated for userId: {}",
                    cartItemId,
                    userId
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            log.warn(
                    "PUT /api/cart/items/{} - Failed for userId: {} - {}",
                    cartItemId,
                    userId,
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "PUT /api/cart/items/{} - Unexpected error for userId: {}",
                    cartItemId,
                    userId,
                    e
            );

            throw e;
        }
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable String cartItemId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "DELETE /api/cart/items/{} - userId: {}",
                cartItemId,
                userId
        );

        try {
            CartResponse response =
                    cartService.removeItem(
                            userId,
                            cartItemId
                    );

            log.info(
                    "DELETE /api/cart/items/{} - Removed for userId: {}",
                    cartItemId,
                    userId
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            log.warn(
                    "DELETE /api/cart/items/{} - Failed for userId: {} - {}",
                    cartItemId,
                    userId,
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "DELETE /api/cart/items/{} - Unexpected error for userId: {}",
                    cartItemId,
                    userId,
                    e
            );

            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "GET /api/cart - Fetching cart for userId: {}",
                userId
        );

        try {
            CartResponse response =
                    cartService.getCart(userId);

            log.debug(
                    "GET /api/cart - Retrieved cartId: {} with {} items for userId: {}",
                    response.id(),
                    response.items().size(),
                    userId
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            log.error(
                    "GET /api/cart - Failed for userId: {}",
                    userId,
                    e
            );

            throw e;
        }
    }

    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "DELETE /api/cart - Clearing cart for userId: {}",
                userId
        );

        try {
            CartResponse response =
                    cartService.clearCart(userId);

            log.info(
                    "DELETE /api/cart - Cleared cartId: {} for userId: {}",
                    response.id(),
                    userId
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            log.error(
                    "DELETE /api/cart - Failed for userId: {}",
                    userId,
                    e
            );

            throw e;
        }
    }
}