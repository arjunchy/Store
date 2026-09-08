package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.response.AdminStatsResponse;
import com.ecommerce.backend.dto.response.OrderResponse;
import com.ecommerce.backend.dto.response.ProductResponse;
import com.ecommerce.backend.dto.response.UserResponse;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.entity.OrderItem;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.mapper.OrderMapper;
import com.ecommerce.backend.mapper.ProductMapper;
import com.ecommerce.backend.mapper.UserMapper;
import com.ecommerce.backend.repository.OrderItemRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.UserRepository;
import com.ecommerce.backend.service.admin.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMapper productMapper;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getDashboardStats() {
        log.info("GET /api/admin/stats - Fetching dashboard stats (ADMIN)");
        try {
            AdminStatsResponse stats = adminService.getDashboardStats();
            log.debug("GET /api/admin/stats - totalUsers={}, totalProducts={}, totalOrders={}",
                    stats.totalUsers(), stats.totalProducts(), stats.totalOrders());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("GET /api/admin/stats - Failed", e);
            throw e;
        }
    }

    @GetMapping("/stats/orders")
    public ResponseEntity<Map<String, Long>> getOrderStats() {
        log.info("GET /api/admin/stats/orders - Fetching order stats (ADMIN)");
        try {
            Map<String, Long> stats = adminService.getOrderStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("GET /api/admin/stats/orders - Failed", e);
            throw e;
        }
    }

    @GetMapping("/stats/revenue")
    public ResponseEntity<Map<String, BigDecimal>> getRevenueStats() {
        log.info("GET /api/admin/stats/revenue - Fetching revenue stats (ADMIN)");
        try {
            Map<String, BigDecimal> stats = adminService.getRevenueStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("GET /api/admin/stats/revenue - Failed", e);
            throw e;
        }
    }

    @GetMapping("/orders")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(Pageable pageable) {
        log.info("GET /api/admin/orders - Fetching all orders (ADMIN) pageable: {}", pageable);
        try {
            if (pageable.getPageSize() > 100) throw new IllegalArgumentException("Page size must be ≤100");
            Page<Order> page = orderRepository.findAll(pageable);
            Page<OrderResponse> mapped = page.map(order -> {
                List<OrderItem> items = orderItemRepository.findByOrderIdWithProduct(order.getId());
                return orderMapper.toOrderResponse(order, items);
            });
            log.debug("GET /api/admin/orders - Retrieved {} orders", mapped.getNumberOfElements());
            return ResponseEntity.ok(mapped);
        } catch (Exception e) {
            log.error("GET /api/admin/orders - Failed", e);
            throw e;
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("GET /api/admin/users - Fetching all users (ADMIN)");
        try {
            List<User> users = userRepository.findAllActive();
            List<UserResponse> responses = users.stream()
                    .map(userMapper::toResponse)
                    .collect(Collectors.toList());
            log.debug("GET /api/admin/users - Retrieved {} users", responses.size());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("GET /api/admin/users - Failed", e);
            throw e;
        }
    }

    @GetMapping("/products")
    public ResponseEntity<Page<ProductResponse>> getAllProducts(Pageable pageable) {
        log.info("GET /api/admin/products - Fetching all products (ADMIN) pageable: {}", pageable);
        try {
            Page<Product> page = productRepository.findAll(pageable);
            Page<ProductResponse> mapped = page.map(productMapper::toResponse);
            log.debug("GET /api/admin/products - Retrieved {} products", mapped.getNumberOfElements());
            return ResponseEntity.ok(mapped);
        } catch (Exception e) {
            log.error("GET /api/admin/products - Failed", e);
            throw e;
        }
    }

    @GetMapping("/products/top")
    public ResponseEntity<List<ProductResponse>> getTopProducts() {
        log.info("GET /api/admin/products/top - Fetching top products (ADMIN)");
        try {
            List<ProductResponse> top = adminService.getTopProducts();
            return ResponseEntity.ok(top);
        } catch (Exception e) {
            log.error("GET /api/admin/products/top - Failed", e);
            throw e;
        }
    }
}
