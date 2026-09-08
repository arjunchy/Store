package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.enums.DeliveryStatus;
import com.ecommerce.backend.enums.OrderPaymentStatus;
import com.ecommerce.backend.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") String id);

    @Query("SELECT o FROM Order o WHERE o.user.userId = :userId")
    Page<Order> findByUserId(@Param("userId") String userId, Pageable pageable);

    long countByStatus(OrderStatus status);

    // Revenue = only PAID orders. Unpaid/expired/canceled or merely-placed orders
    // must not count as realized revenue.
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = com.ecommerce.backend.enums.OrderPaymentStatus.PAID")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status")
    BigDecimal sumTotalRevenueByStatus(@Param("status") OrderStatus status);

    List<Order> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT o FROM Order o WHERE (:status IS NULL OR o.status = :status) AND (:search IS NULL OR :search = '' OR o.orderNumber LIKE CONCAT('%', :search, '%') ESCAPE '\\' OR o.user.email LIKE CONCAT('%', :search, '%') ESCAPE '\\' OR o.user.username LIKE CONCAT('%', :search, '%') ESCAPE '\\')")
    Page<Order> findByAdminFilter(@Param("status") OrderStatus status, @Param("search") String search, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:deliveryStatus IS NULL OR o.deliveryStatus = :deliveryStatus) AND " +
           "(:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\' " +
           "OR LOWER(o.user.email) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\' " +
           "OR LOWER(o.user.username) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\')")
    Page<Order> findAdminFiltered(
        @Param("status") OrderStatus status,
        @Param("deliveryStatus") DeliveryStatus deliveryStatus,
        @Param("paymentStatus") OrderPaymentStatus paymentStatus,
        @Param("search") String search,
        Pageable pageable
    );

    boolean existsByUserUserId(String userId);

    @Query("SELECT o FROM Order o WHERE o.user.userId = :userId AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:deliveryStatus IS NULL OR o.deliveryStatus = :deliveryStatus) AND " +
           "(:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\')")
    Page<Order> findByUserIdFiltered(
        @Param("userId") String userId,
        @Param("status") OrderStatus status,
        @Param("deliveryStatus") DeliveryStatus deliveryStatus,
        @Param("paymentStatus") OrderPaymentStatus paymentStatus,
        @Param("search") String search,
        Pageable pageable
    );
}
