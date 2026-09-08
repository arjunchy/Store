package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, String> {

    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.product.id = :productId")
    List<Review> findByProductIdWithUser(@Param("productId") String productId);

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId")
    List<Review> findByProductId(@Param("productId") String productId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Review r WHERE r.product.id = :productId AND r.user.userId = :userId")
    boolean existsByProductIdAndUserId(@Param("productId") String productId, @Param("userId") String userId);

    boolean existsByUserUserId(String userId);

    org.springframework.data.domain.Page<Review> findByRatingGreaterThanEqual(Integer rating, org.springframework.data.domain.Pageable pageable);
}
