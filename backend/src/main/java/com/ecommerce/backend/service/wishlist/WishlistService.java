package com.ecommerce.backend.service.wishlist;

import com.ecommerce.backend.dto.request.WishlistRequest;
import com.ecommerce.backend.dto.response.WishlistResponse;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.entity.Wishlist;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.UserRepository;
import com.ecommerce.backend.repository.WishlistRepository;
import com.ecommerce.backend.mapper.WishlistMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WishlistMapper wishlistMapper;

    @Transactional
    public WishlistResponse addToWishlist(String userId, WishlistRequest request) {
        log.info("Adding product {} to wishlist for userId: {}", request.productId(), userId);
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.warn("User not found with id: {}", userId);
                        return new IllegalArgumentException("User not found");
                    });

            Product product = productRepository.findById(request.productId())
                    .orElseThrow(() -> {
                        log.warn("Product not found with id: {}", request.productId());
                        return new IllegalArgumentException("Product not found");
                    });

            if (wishlistRepository.existsByUserIdAndProductId(userId, request.productId())) {
                log.warn("Product {} already in wishlist for userId: {}", request.productId(), userId);
                throw new IllegalArgumentException("Product already in wishlist");
            }

            Wishlist wishlist = Wishlist.builder()
                    .user(user)
                    .product(product)
                    .build();

            Wishlist saved = wishlistRepository.save(wishlist);
            log.info("Successfully added product {} to wishlist with id: {} for userId: {}", request.productId(), saved.getId(), userId);
            return wishlistMapper.toResponse(saved);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to add product {} to wishlist for userId: {}", request.productId(), userId, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<WishlistResponse> getWishlist(String userId) {
        log.debug("Fetching wishlist for userId: {}", userId);
        try {
            if (!userRepository.existsById(userId)) {
                log.warn("User not found with id: {}", userId);
                throw new IllegalArgumentException("User not found");
            }
            List<Wishlist> wishlists = wishlistRepository.findByUserIdWithProduct(userId);
            log.info("Retrieved {} wishlist items for userId: {}", wishlists.size(), userId);
            return wishlists.stream()
                    .map(wishlistMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch wishlist for userId: {}", userId, e);
            throw e;
        }
    }

    @Transactional
    public void removeFromWishlist(String userId, String wishlistId) {
        log.info("Removing wishlist item {} for userId: {}", wishlistId, userId);
        try {
            Wishlist wishlist = wishlistRepository.findById(wishlistId)
                    .orElseThrow(() -> {
                        log.warn("Wishlist item not found with id: {}", wishlistId);
                        return new IllegalArgumentException("Wishlist item not found");
                    });

            if (!wishlist.getUser().getUserId().equals(userId)) {
                log.warn("Access denied - wishlist {} does not belong to user {}", wishlistId, userId);
                throw new IllegalArgumentException("Wishlist item not found");
            }

            wishlistRepository.delete(wishlist);
            log.info("Successfully removed wishlist item {} for userId: {}", wishlistId, userId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to remove wishlist item {} for userId: {}", wishlistId, userId, e);
            throw e;
        }
    }
}
