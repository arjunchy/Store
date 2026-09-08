package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Payment;
import com.ecommerce.backend.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId AND p.status = :status")
    Optional<Payment> findByOrderIdAndStatus(@Param("orderId") String orderId, @Param("status") PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId")
    List<Payment> findByOrderId(@Param("orderId") String orderId);

    Optional<Payment> findByPidx(String pidx);

    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId AND p.status = :status ORDER BY p.createdAt DESC")
    Optional<Payment> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(@Param("orderId") String orderId, @Param("status") PaymentStatus status);

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(String orderId);
}
