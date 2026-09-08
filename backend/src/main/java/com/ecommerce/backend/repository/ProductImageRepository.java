package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, String> {

    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id = :productId ORDER BY pi.displayOrder ASC")
    List<ProductImage> findByProductId(@Param("productId") String productId);

    @Modifying
    @Query("UPDATE ProductImage pi SET pi.isPrimary = false WHERE pi.product.id = :productId AND pi.isPrimary = true")
    int unsetOtherPrimary(@Param("productId") String productId);
}
