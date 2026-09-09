package com.ecommerce.backend.service.cart;

import com.ecommerce.backend.dto.request.CartItemRequest;
import com.ecommerce.backend.dto.response.CartResponse;
import com.ecommerce.backend.entity.Cart;
import com.ecommerce.backend.entity.CartItem;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.repository.CartItemRepository;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.mapper.CartMapper;
import com.ecommerce.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartMapper cartMapper;

    private Cart getOrCreateCart(String userId) {
        log.debug("Getting or creating cart for userId: {}", userId);
        try {
            return cartRepository.findByUserIdWithItems(userId)
                    .orElseGet(() -> {
                        log.info("No cart found for userId: {}, creating new cart", userId);
                        User user = userRepository.findById(userId)
                                .orElseThrow(() -> {
                                    log.warn("User not found with id: {}", userId);
                                    return new IllegalArgumentException("User not found");
                                });
                        Cart newCart = Cart.builder()
                                .user(user)
                                .build();
                        try {
                            Cart saved = cartRepository.save(newCart);
                            log.info("Created new cart with id: {} for userId: {}", saved.getId(), userId);
                            return saved;
                        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                            log.warn("Concurrent cart creation for userId: {}, fetching existing", userId);
                            return cartRepository.findByUserIdWithItems(userId).orElseThrow(() -> ex);
                        }
                    });
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get or create cart for userId: {}", userId, e);
            throw e;
        }
    }

    private CartResponse toCartResponse(Cart cart) {
        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse getCart(String userId) {
        log.info("Fetching cart for userId: {}", userId);
        try {
            Cart cart = getOrCreateCart(userId);
            log.debug("Found cart with id: {} containing {} items for userId: {}", cart.getId(), cart.getCartItems().size(), userId);
            return toCartResponse(cart);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch cart for userId: {}", userId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public CartResponse addItem(String userId, CartItemRequest request) {
        log.info("Adding item to cart for userId: {} productId: {} quantity: {}", userId, request.productId(), request.quantity());
        try {
            Cart cart = getOrCreateCart(userId);

            Product product = productRepository.findByIdForUpdate(request.productId())
                    .orElseGet(() -> productRepository.findById(request.productId())
                            .orElseThrow(() -> {
                                log.warn("Product not found with id: {}", request.productId());
                                return new IllegalArgumentException("Product not found");
                            }));
            if (product.getDeletedAt() != null) throw new IllegalArgumentException("Product not available");
            if (request.quantity() == null || request.quantity() <= 0) {
                log.warn("Invalid quantity {} for addItem", request.quantity());
                throw new IllegalArgumentException("Quantity must be > 0");
            }
            if (product.getStockQuantity() != null && request.quantity() > product.getStockQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for product " + product.getName() + ": available " + product.getStockQuantity());
            }

            CartItem existing = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                    .orElse(null);

            if (existing != null) {
                int newQty = existing.getQuantity() + request.quantity();
                if (newQty > 99) throw new IllegalArgumentException("Cart quantity limit 99 exceeded");
                if (product.getStockQuantity() != null && newQty > product.getStockQuantity()) {
                    throw new IllegalArgumentException("Insufficient stock: requested " + newQty + " available " + product.getStockQuantity());
                }
                log.debug("Product already in cart, increasing quantity from {} to {} for cartItemId: {}", existing.getQuantity(), newQty, existing.getId());
                existing.setQuantity(newQty);
                cartItemRepository.save(existing);
                log.info("Updated quantity for cartItemId: {} to {}", existing.getId(), newQty);
            } else {
                try {
                    CartItem newItem = CartItem.builder()
                            .cart(cart)
                            .product(product)
                            .quantity(request.quantity())
                            .build();
                    CartItem saved = cartItemRepository.save(newItem);
                    cart.getCartItems().add(saved);
                    log.info("Added new cartItem with id: {} for product {} to cart {}", saved.getId(), product.getId(), cart.getId());
                } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                    throw new IllegalArgumentException("Product already in cart (concurrent)", ex);
                }
            }

            Cart refreshed = cartRepository.findByUserIdWithItems(userId).orElse(cart);
            return toCartResponse(refreshed);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to add item to cart for userId: {} productId: {}", userId, request.productId(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(String userId, String cartItemId, Integer quantity) {
        log.info("Updating quantity for cartItemId: {} to {} for userId: {}", cartItemId, quantity, userId);
        try {
            Cart cart = getOrCreateCart(userId);

            CartItem cartItem = cartItemRepository.findById(cartItemId)
                    .orElseThrow(() -> {
                        log.warn("Cart item not found with id: {}", cartItemId);
                        return new IllegalArgumentException("Cart item not found");
                    });

            if (!cartItem.getCart().getId().equals(cart.getId())) {
                log.warn("Access denied - cartItem {} does not belong to user {}", cartItemId, userId);
                throw new IllegalArgumentException("Cart item not found");
            }

            if (quantity == null || quantity <= 0) {
                log.info("Quantity <= 0, removing cartItemId: {}", cartItemId);
                cart.getCartItems().removeIf(item -> item.getId().equals(cartItemId));
                cartItemRepository.delete(cartItem);
            } else {
                if (quantity > 99) throw new IllegalArgumentException("Quantity must be <=99");
                // stock check for update — fetch fresh stock to prevent oversell via stale cartItem.product
                Product p = cartItem.getProduct();
                if (p != null) {
                    Product freshStock = productRepository.findByIdForUpdate(p.getId())
                            .orElseGet(() -> productRepository.findById(p.getId()).orElse(p));
                    if (freshStock.getStockQuantity() != null && quantity > freshStock.getStockQuantity()) {
                        throw new IllegalArgumentException("Insufficient stock: available " + freshStock.getStockQuantity());
                    }
                }
                log.debug("Setting quantity {} for cartItemId: {}", quantity, cartItemId);
                cartItem.setQuantity(quantity);
                cartItemRepository.save(cartItem);
                log.info("Updated quantity for cartItemId: {} to {}", cartItemId, quantity);
            }

            Cart refreshed = cartRepository.findByUserIdWithItems(userId).orElse(cart);
            return toCartResponse(refreshed);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update quantity for cartItemId: {} for userId: {}", cartItemId, userId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public CartResponse removeItem(String userId, String cartItemId) {
        log.info("Removing cartItemId: {} for userId: {}", cartItemId, userId);
        try {
            Cart cart = getOrCreateCart(userId);

            CartItem cartItem = cartItemRepository.findById(cartItemId)
                    .orElseThrow(() -> {
                        log.warn("Cart item not found with id: {}", cartItemId);
                        return new IllegalArgumentException("Cart item not found");
                    });

            if (!cartItem.getCart().getId().equals(cart.getId())) {
                log.warn("Access denied - cartItem {} does not belong to user {}", cartItemId, userId);
                throw new IllegalArgumentException("Cart item not found");
            }

            cart.getCartItems().removeIf(item -> item.getId().equals(cartItemId));
            cartItemRepository.delete(cartItem);
            log.info("Successfully removed cartItemId: {} for userId: {}", cartItemId, userId);

            Cart refreshed = cartRepository.findByUserIdWithItems(userId).orElse(cart);
            return toCartResponse(refreshed);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to remove cartItemId: {} for userId: {}", cartItemId, userId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public CartResponse clearCart(String userId) {
        log.info("Clearing cart for userId: {}", userId);
        try {
            Cart cart = getOrCreateCart(userId);
            int size = cart.getCartItems().size();
            cart.getCartItems().clear();
            cartRepository.save(cart);
            log.info("Cleared {} items from cartId: {} for userId: {}", size, cart.getId(), userId);
            return toCartResponse(cart);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to clear cart for userId: {}", userId, e);
            throw e;
        }
    }
}
