package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, String> {

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.product WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderIdWithProduct(@Param("orderId") String orderId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderId(@Param("orderId") String orderId);

    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END FROM OrderItem oi WHERE oi.product.id = :productId AND oi.order.user.userId = :userId")
    boolean existsByProductIdAndUserId(@Param("productId") String productId, @Param("userId") String userId);

    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END FROM OrderItem oi WHERE oi.product.id = :productId AND oi.order.user.userId = :userId AND oi.order.status IN (com.ecommerce.backend.enums.OrderStatus.CONFIRMED, com.ecommerce.backend.enums.OrderStatus.SHIPPED, com.ecommerce.backend.enums.OrderStatus.DELIVERED, com.ecommerce.backend.enums.OrderStatus.COMPLETED)")
    boolean existsVerifiedPurchase(@Param("productId") String productId, @Param("userId") String userId);

    @Query("SELECT oi.product.id as productId, SUM(oi.quantity) as totalSold FROM OrderItem oi WHERE oi.order.status NOT IN (com.ecommerce.backend.enums.OrderStatus.CANCELED, com.ecommerce.backend.enums.OrderStatus.CANCELLED) GROUP BY oi.product.id ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProductIds(org.springframework.data.domain.Pageable pageable);
}
