package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.id = :id")
    java.util.Optional<Product> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") String id);

    Page<Product> findByCategoryId(String categoryId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.category.id = :categoryId")
    List<Product> findAllByCategoryId(@org.springframework.data.repository.query.Param("categoryId") String categoryId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Product p SET p.category = null WHERE p.category.id = :categoryId")
    void clearCategoryForProducts(@org.springframework.data.repository.query.Param("categoryId") String categoryId);

    long countByStockQuantity(int stockQuantity);

    boolean existsByName(String name);

    java.util.Optional<Product> findByName(String name);
}
