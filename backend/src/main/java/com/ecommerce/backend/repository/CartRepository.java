package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, String> {

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.cartItems ci LEFT JOIN FETCH ci.product WHERE c.user.userId = :userId")
    Optional<Cart> findByUserIdWithItems(@Param("userId") String userId);

    @Query("SELECT c FROM Cart c WHERE c.user.userId = :userId")
    Optional<Cart> findByUserId(@Param("userId") String userId);
}
