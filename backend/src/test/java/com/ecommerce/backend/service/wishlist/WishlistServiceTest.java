package com.ecommerce.backend.service.wishlist;

import com.ecommerce.backend.dto.request.WishlistRequest;
import com.ecommerce.backend.dto.response.WishlistResponse;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.entity.Wishlist;
import com.ecommerce.backend.mapper.WishlistMapper;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.UserRepository;
import com.ecommerce.backend.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WishlistServiceTest {

    @Mock private WishlistRepository wishlistRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private WishlistMapper wishlistMapper;

    @InjectMocks
    private WishlistService wishlistService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = User.builder().userId("user-1").build();
        product = Product.builder().id("prod-1").name("Laptop").build();
    }

    @Test
    void addToWishlist_success() {
        WishlistRequest request = new WishlistRequest("prod-1");
        Wishlist saved = Wishlist.builder().id("wish-1").user(user).product(product).createdAt(LocalDateTime.now()).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(wishlistRepository.existsByUserIdAndProductId("user-1", "prod-1")).thenReturn(false);
        when(wishlistRepository.save(any())).thenReturn(saved);
        when(wishlistMapper.toResponse(saved)).thenReturn(
                new WishlistResponse("wish-1", "prod-1", "Laptop", LocalDateTime.now())
        );

        WishlistResponse response = wishlistService.addToWishlist("user-1", request);

        assertThat(response.productId()).isEqualTo("prod-1");
    }

    @Test
    void addToWishlist_alreadyExists_throws() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(wishlistRepository.existsByUserIdAndProductId("user-1", "prod-1")).thenReturn(true);

        assertThatThrownBy(() -> wishlistService.addToWishlist("user-1", new WishlistRequest("prod-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already in wishlist");
    }

    @Test
    void addToWishlist_userNotFound_throws() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.addToWishlist("missing", new WishlistRequest("prod-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void addToWishlist_productNotFound_throws() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.addToWishlist("user-1", new WishlistRequest("missing")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void getWishlist_success() {
        Wishlist item = Wishlist.builder().id("wish-1").user(user).product(product).build();
        when(userRepository.existsById("user-1")).thenReturn(true);
        when(wishlistRepository.findByUserIdWithProduct("user-1")).thenReturn(List.of(item));
        when(wishlistMapper.toResponse(item)).thenReturn(
                new WishlistResponse("wish-1", "prod-1", "Laptop", LocalDateTime.now())
        );

        List<WishlistResponse> result = wishlistService.getWishlist("user-1");

        assertThat(result).hasSize(1);
    }

    @Test
    void getWishlist_empty() {
        when(userRepository.existsById("user-1")).thenReturn(true);
        when(wishlistRepository.findByUserIdWithProduct("user-1")).thenReturn(List.of());

        List<WishlistResponse> result = wishlistService.getWishlist("user-1");

        assertThat(result).isEmpty();
    }

    @Test
    void getWishlist_userNotFound_throws() {
        when(userRepository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> wishlistService.getWishlist("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void removeFromWishlist_success() {
        Wishlist item = Wishlist.builder().id("wish-1").user(user).product(product).build();
        when(wishlistRepository.findById("wish-1")).thenReturn(Optional.of(item));

        wishlistService.removeFromWishlist("user-1", "wish-1");

        verify(wishlistRepository).delete(item);
    }

    @Test
    void removeFromWishlist_notFound_throws() {
        when(wishlistRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.removeFromWishlist("user-1", "missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wishlist item not found");
    }

    @Test
    void removeFromWishlist_wrongOwner_throws() {
        User otherUser = User.builder().userId("user-2").build();
        Wishlist otherItem = Wishlist.builder().id("wish-2").user(otherUser).product(product).build();
        when(wishlistRepository.findById("wish-2")).thenReturn(Optional.of(otherItem));

        assertThatThrownBy(() -> wishlistService.removeFromWishlist("user-1", "wish-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wishlist item not found");
    }
}
