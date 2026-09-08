package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.response.AdminStatsResponse;
import com.ecommerce.backend.dto.response.OrderResponse;
import com.ecommerce.backend.dto.response.ProductResponse;
import com.ecommerce.backend.dto.response.UserResponse;
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
import com.ecommerce.backend.service.admin.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private AdminService adminService;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private ProductRepository productRepository;
    @Mock private ProductMapper productMapper;

    @InjectMocks
    private AdminController adminController;

    private AdminStatsResponse statsResponse;

    @BeforeEach
    void setUp() {
        statsResponse = new AdminStatsResponse(
                10L, 20L, 30L, new BigDecimal("50000"),
                5L, 3L,
                List.of(new OrderResponse("order-1", "ORD-123", new BigDecimal("1000"), OrderStatus.PENDING, null, DeliveryStatus.PLACED, List.of(), LocalDateTime.now(), null, null, null)),
                List.of(new UserResponse("user-1", "john", "john@example.com", UserRole.USER, LocalDateTime.now()))
        );
    }

    @Test
    void getDashboardStats_success_returns200() {
        when(adminService.getDashboardStats()).thenReturn(statsResponse);
        var resp = adminController.getDashboardStats();
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody().totalUsers()).isEqualTo(10L);
        verify(adminService).getDashboardStats();
    }

    @Test
    void getOrderStats_success_returns200() {
        when(adminService.getOrderStats()).thenReturn(Map.of("PENDING", 5L));
        var resp = adminController.getOrderStats();
        assertThat(resp.getBody().get("PENDING")).isEqualTo(5L);
    }

    @Test
    void getRevenueStats_success_returns200() {
        when(adminService.getRevenueStats()).thenReturn(Map.of("PENDING", new BigDecimal("1000")));
        var resp = adminController.getRevenueStats();
        assertThat(resp.getBody().get("PENDING")).isEqualByComparingTo(new BigDecimal("1000"));
    }

    @Test
    void getAllUsers_success_returns200() {
        var user = new com.ecommerce.backend.entity.User();
        user.setUserId("user-1");
        user.setUsername("john");
        user.setEmail("john@example.com");
        user.setUserRole(UserRole.USER);
        when(userRepository.findAllActive()).thenReturn(List.of(user));
        UserResponse ur = new UserResponse("user-1", "john", "john@example.com", UserRole.USER, LocalDateTime.now());
        when(userMapper.toResponse(user)).thenReturn(ur);

        var resp = adminController.getAllUsers();
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).id()).isEqualTo("user-1");
    }

    @Test
    void getAllProducts_success_returns200() {
        var product = com.ecommerce.backend.entity.Product.builder().id("prod-1").name("Laptop").price(new BigDecimal("1000")).stockQuantity(5).build();
        Page<com.ecommerce.backend.entity.Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findAll(any(Pageable.class))).thenReturn(page);
        ProductResponse pr = new ProductResponse("prod-1", "Laptop", "Desc", new BigDecimal("1000"), 5, false, 0.0f, 0, null, LocalDateTime.now());
        when(productMapper.toResponse(product)).thenReturn(pr);

        var resp = adminController.getAllProducts(Pageable.unpaged());
        assertThat(resp.getBody().getTotalElements()).isEqualTo(1);
    }

    @Test
    void getAllOrders_success_returns200() {
        var order = com.ecommerce.backend.entity.Order.builder().id("order-1").orderNumber("ORD-123").totalAmount(new BigDecimal("1000")).status(OrderStatus.PENDING).build();
        Page<com.ecommerce.backend.entity.Order> page = new PageImpl<>(List.of(order));
        when(orderRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(orderItemRepository.findByOrderIdWithProduct("order-1")).thenReturn(List.of());
        OrderResponse or = new OrderResponse("order-1", "ORD-123", new BigDecimal("1000"), OrderStatus.PENDING, null, DeliveryStatus.PLACED, List.of(), LocalDateTime.now(), null, null, null);
        when(orderMapper.toOrderResponse(eq(order), anyList())).thenReturn(or);

        var resp = adminController.getAllOrders(Pageable.unpaged());
        assertThat(resp.getBody().getTotalElements()).isEqualTo(1);
        assertThat(resp.getBody().getContent().get(0).id()).isEqualTo("order-1");
    }

    @Test
    void getTopProducts_success_returns200() {
        ProductResponse pr = new ProductResponse("prod-1", "Laptop", "Desc", new BigDecimal("1000"), 5, false, 4.5f, 0, null, LocalDateTime.now());
        when(adminService.getTopProducts()).thenReturn(List.of(pr));
        var resp = adminController.getTopProducts();
        assertThat(resp.getBody()).hasSize(1);
    }
}
