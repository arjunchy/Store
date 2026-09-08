package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WishlistRepository extends JpaRepository<Wishlist, String> {

    @Query("SELECT w FROM Wishlist w JOIN FETCH w.product WHERE w.user.userId = :userId")
    List<Wishlist> findByUserIdWithProduct(@Param("userId") String userId);

    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM Wishlist w WHERE w.user.userId = :userId AND w.product.id = :productId")
    boolean existsByUserIdAndProductId(@Param("userId") String userId, @Param("productId") String productId);
}
