package com.ecommerce.backend.service.admin;

import com.ecommerce.backend.dto.response.AdminStatsResponse;
import com.ecommerce.backend.dto.response.OrderResponse;
import com.ecommerce.backend.dto.response.ProductResponse;
import com.ecommerce.backend.dto.response.UserResponse;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.DeliveryStatus;
import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.enums.UserRole;
import com.ecommerce.backend.mapper.OrderMapper;
import com.ecommerce.backend.mapper.ProductMapper;
import com.ecommerce.backend.mapper.UserMapper;
import com.ecommerce.backend.repository.OrderItemRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private UserMapper userMapper;
    @Mock private ProductMapper productMapper;

    @InjectMocks
    private AdminService adminService;

    private User user;
    private Product product;
    private Order order;

    @BeforeEach
    void setUp() {
        user = User.builder().userId("user-1").username("john").email("john@example.com").userRole(UserRole.USER).createdAt(LocalDateTime.now()).build();
        product = Product.builder().id("prod-1").name("Laptop").price(new BigDecimal("1000")).stockQuantity(5).rating(4.5f).build();
        order = Order.builder().id("order-1").orderNumber("ORD-123").totalAmount(new BigDecimal("1000")).status(OrderStatus.PENDING).createdAt(LocalDateTime.now()).build();
    }

    @Test
    void getDashboardStats_success() {
        when(userRepository.count()).thenReturn(10L);
        when(productRepository.count()).thenReturn(20L);
        when(orderRepository.count()).thenReturn(30L);
        when(orderRepository.sumTotalRevenue()).thenReturn(new BigDecimal("50000.00"));
        when(orderRepository.countByStatus(OrderStatus.PENDING)).thenReturn(5L);
        when(productRepository.countByStockQuantity(0)).thenReturn(3L);

        OrderResponse orderResp = new OrderResponse("order-1", "ORD-123", new BigDecimal("1000"), OrderStatus.PENDING, null, DeliveryStatus.PLACED, List.of(), LocalDateTime.now(), null, null, null);
        UserResponse userResp = new UserResponse("user-1", "john", "john@example.com", UserRole.USER, LocalDateTime.now());

        // Mock recent orders and users via repository calls
        when(orderRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of(order));
        when(orderItemRepository.findByOrderIdWithProduct("order-1")).thenReturn(List.of());
        when(orderMapper.toOrderResponse(eq(order), anyList())).thenReturn(orderResp);
        when(userRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResp);

        AdminStatsResponse stats = adminService.getDashboardStats();

        assertThat(stats.totalUsers()).isEqualTo(10L);
        assertThat(stats.totalProducts()).isEqualTo(20L);
        assertThat(stats.totalOrders()).isEqualTo(30L);
        assertThat(stats.totalRevenue()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(stats.pendingOrders()).isEqualTo(5L);
        assertThat(stats.outOfStockProducts()).isEqualTo(3L);
        assertThat(stats.recentOrders()).hasSize(1);
        assertThat(stats.recentUsers()).hasSize(1);
    }

    @Test
    void getDashboardStats_nullRevenue_defaultsToZero() {
        when(userRepository.count()).thenReturn(0L);
        when(productRepository.count()).thenReturn(0L);
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.sumTotalRevenue()).thenReturn(null);
        when(orderRepository.countByStatus(any())).thenReturn(0L);
        when(productRepository.countByStockQuantity(0)).thenReturn(0L);
        when(orderRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(userRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of());

        AdminStatsResponse stats = adminService.getDashboardStats();
        assertThat(stats.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getOrderStats_success() {
        for (OrderStatus status : OrderStatus.values()) {
            when(orderRepository.countByStatus(status)).thenReturn(2L);
        }
        Map<String, Long> stats = adminService.getOrderStats();
        assertThat(stats).hasSize(OrderStatus.values().length);
        assertThat(stats.get("PENDING")).isEqualTo(2L);
    }

    @Test
    void getRevenueStats_success() {
        for (OrderStatus status : OrderStatus.values()) {
            when(orderRepository.sumTotalRevenueByStatus(status)).thenReturn(new BigDecimal("1000"));
        }
        when(orderRepository.sumTotalRevenue()).thenReturn(new BigDecimal("5000"));
        Map<String, BigDecimal> stats = adminService.getRevenueStats();
        assertThat(stats).containsKey("PENDING");
        assertThat(stats).containsKey("TOTAL");
        assertThat(stats.get("TOTAL")).isEqualByComparingTo(new BigDecimal("5000"));
    }

    @Test
    void getRecentOrders_success() {
        when(orderRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of(order));
        when(orderItemRepository.findByOrderIdWithProduct("order-1")).thenReturn(List.of());
        OrderResponse resp = new OrderResponse("order-1", "ORD-123", new BigDecimal("1000"), OrderStatus.PENDING, null, DeliveryStatus.PLACED, List.of(), LocalDateTime.now(), null, null, null);
        when(orderMapper.toOrderResponse(eq(order), anyList())).thenReturn(resp);

        List<OrderResponse> recent = adminService.getRecentOrders();
        assertThat(recent).hasSize(1);
        assertThat(recent.get(0).id()).isEqualTo("order-1");
    }

    @Test
    void getTopProducts_withSales_returnsProducts() {
        Object[] row = new Object[]{"prod-1", 10L};
        when(orderItemRepository.findTopSellingProductIds(any(PageRequest.class))).thenReturn(java.util.Collections.singletonList(row));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        ProductResponse pr = new ProductResponse("prod-1", "Laptop", "Desc", new BigDecimal("1000"), 5, false, 4.5f, 0, null, LocalDateTime.now());
        when(productMapper.toResponse(product)).thenReturn(pr);

        List<ProductResponse> top = adminService.getTopProducts();
        assertThat(top).hasSize(1);
        assertThat(top.get(0).id()).isEqualTo("prod-1");
    }

    @Test
    void getTopProducts_noSales_fallsBackToTopRated() {
        when(orderItemRepository.findTopSellingProductIds(any(PageRequest.class))).thenReturn(java.util.Collections.emptyList());
        Product topRated = Product.builder().id("prod-2").name("Phone").price(new BigDecimal("500")).rating(5.0f).stockQuantity(10).build();
        when(productRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(topRated)));
        ProductResponse pr = new ProductResponse("prod-2", "Phone", "Desc", new BigDecimal("500"), 10, false, 5.0f, 0, null, LocalDateTime.now());
        when(productMapper.toResponse(topRated)).thenReturn(pr);

        List<ProductResponse> top = adminService.getTopProducts();
        assertThat(top).hasSize(1);
        assertThat(top.get(0).id()).isEqualTo("prod-2");
    }

    @Test
    void getRecentUsers_success() {
        when(userRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of(user));
        UserResponse ur = new UserResponse("user-1", "john", "john@example.com", UserRole.USER, LocalDateTime.now());
        when(userMapper.toResponse(user)).thenReturn(ur);

        List<UserResponse> recent = adminService.getRecentUsers();
        assertThat(recent).hasSize(1);
    }
}
