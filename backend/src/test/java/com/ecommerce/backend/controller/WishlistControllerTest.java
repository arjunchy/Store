package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.request.WishlistRequest;
import com.ecommerce.backend.dto.response.WishlistResponse;
import com.ecommerce.backend.service.wishlist.WishlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistControllerTest {

    @Mock
    private WishlistService wishlistService;

    @InjectMocks
    private WishlistController wishlistController;

    @Mock
    private CustomUserDetails userDetails;

    private WishlistResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new WishlistResponse("wish-1", "prod-1", "Laptop", LocalDateTime.now());
    }

    @Test
    void addToWishlist_success_returns201() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(wishlistService.addToWishlist(eq("user-1"), any(WishlistRequest.class))).thenReturn(sampleResponse);

        WishlistRequest request = new WishlistRequest("prod-1");
        ResponseEntity<WishlistResponse> response = wishlistController.addToWishlist(request, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().id()).isEqualTo("wish-1");
        verify(wishlistService).addToWishlist("user-1", request);
    }

    @Test
    void addToWishlist_alreadyInWishlist_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(wishlistService.addToWishlist(eq("user-1"), any())).thenThrow(new IllegalArgumentException("Product already in wishlist"));

        assertThatThrownBy(() -> wishlistController.addToWishlist(new WishlistRequest("prod-1"), userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already in wishlist");
    }

    @Test
    void addToWishlist_productNotFound_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(wishlistService.addToWishlist(eq("user-1"), any())).thenThrow(new IllegalArgumentException("Product not found"));

        assertThatThrownBy(() -> wishlistController.addToWishlist(new WishlistRequest("missing"), userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getWishlist_success_returns200() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(wishlistService.getWishlist("user-1")).thenReturn(List.of(sampleResponse));

        ResponseEntity<List<WishlistResponse>> response = wishlistController.getWishlist(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).productName()).isEqualTo("Laptop");
    }

    @Test
    void getWishlist_empty_returnsEmptyList() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(wishlistService.getWishlist("user-1")).thenReturn(List.of());

        ResponseEntity<List<WishlistResponse>> response = wishlistController.getWishlist(userDetails);

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void removeFromWishlist_success_returns204() {
        when(userDetails.getUserId()).thenReturn("user-1");
        doNothing().when(wishlistService).removeFromWishlist("user-1", "wish-1");

        ResponseEntity<Void> response = wishlistController.removeFromWishlist("wish-1", userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(wishlistService).removeFromWishlist("user-1", "wish-1");
    }

    @Test
    void removeFromWishlist_notFound_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        doThrow(new IllegalArgumentException("Wishlist item not found")).when(wishlistService).removeFromWishlist("user-1", "missing");

        assertThatThrownBy(() -> wishlistController.removeFromWishlist("missing", userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeFromWishlist_notOwner_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        doThrow(new IllegalArgumentException("Wishlist item not found")).when(wishlistService).removeFromWishlist("user-1", "wish-1");

        assertThatThrownBy(() -> wishlistController.removeFromWishlist("wish-1", userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
