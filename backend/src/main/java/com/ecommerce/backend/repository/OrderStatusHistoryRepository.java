package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, String> {

    @Query("SELECT h FROM OrderStatusHistory h WHERE h.order.id = :orderId ORDER BY h.createdAt ASC")
    List<OrderStatusHistory> findByOrderId(@Param("orderId") String orderId);
}
