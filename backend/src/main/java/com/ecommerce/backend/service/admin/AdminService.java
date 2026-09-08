package com.ecommerce.backend.service.admin;

import com.ecommerce.backend.dto.response.AdminStatsResponse;
import com.ecommerce.backend.dto.response.OrderResponse;
import com.ecommerce.backend.dto.response.ProductResponse;
import com.ecommerce.backend.dto.response.UserResponse;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.entity.OrderItem;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.mapper.OrderMapper;
import com.ecommerce.backend.mapper.ProductMapper;
import com.ecommerce.backend.mapper.UserMapper;
import com.ecommerce.backend.repository.OrderItemRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    @Transactional(readOnly = true)
    public AdminStatsResponse getDashboardStats() {
        log.info("Fetching admin dashboard stats");
        try {
            long totalUsers = userRepository.count();
            long totalProducts = productRepository.count();
            long totalOrders = orderRepository.count();
            BigDecimal totalRevenue = orderRepository.sumTotalRevenue();
            if (totalRevenue == null) {
                totalRevenue = BigDecimal.ZERO;
            }
            long pendingOrders = orderRepository.countByStatus(OrderStatus.PROCESSING);
            long outOfStockProducts = productRepository.countByStockQuantity(0);

            List<OrderResponse> recentOrders = getRecentOrders();
            List<UserResponse> recentUsers = getRecentUsers();

            log.info("Admin stats: users={}, products={}, orders={}, revenue={}, pending={}, outOfStock={}",
                    totalUsers, totalProducts, totalOrders, totalRevenue, pendingOrders, outOfStockProducts);

            return new AdminStatsResponse(
                    totalUsers,
                    totalProducts,
                    totalOrders,
                    totalRevenue,
                    pendingOrders,
                    outOfStockProducts,
                    recentOrders,
                    recentUsers
            );
        } catch (Exception e) {
            log.error("Failed to fetch admin dashboard stats", e);
            throw e;
        }
    }

    private static final java.util.Set<OrderStatus> CANONICAL_STATUSES = java.util.EnumSet.of(
            OrderStatus.PROCESSING, OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED, OrderStatus.CANCELED, OrderStatus.COMPLETED);

    @Transactional(readOnly = true)
    public Map<String, Long> getOrderStats() {
        log.debug("Fetching order stats by canonical status");
        try {
            Map<String, Long> stats = new HashMap<>();
            for (OrderStatus status : CANONICAL_STATUSES) {
                long count = orderRepository.countByStatus(status);
                stats.put(status.name(), count);
                log.debug("Order status {}: {}", status, count);
            }
            log.info("Order stats: {}", stats);
            return stats;
        } catch (Exception e) {
            log.error("Failed to fetch order stats", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getRevenueStats() {
        log.debug("Fetching revenue stats by canonical status");
        try {
            Map<String, BigDecimal> stats = new HashMap<>();
            for (OrderStatus status : CANONICAL_STATUSES) {
                BigDecimal revenue = orderRepository.sumTotalRevenueByStatus(status);
                if (revenue == null) {
                    revenue = BigDecimal.ZERO;
                }
                stats.put(status.name(), revenue);
                log.debug("Revenue for status {}: {}", status, revenue);
            }
            BigDecimal total = orderRepository.sumTotalRevenue();
            stats.put("TOTAL", total != null ? total : BigDecimal.ZERO);
            log.info("Revenue stats: {}", stats);
            return stats;
        } catch (Exception e) {
            log.error("Failed to fetch revenue stats", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getTopProducts() {
        log.debug("Fetching top selling products");
        try {
            List<Object[]> topIds = orderItemRepository.findTopSellingProductIds(PageRequest.of(0, 5));
            // N+1 optimization: bulk fetch
            java.util.Set<String> ids = topIds.stream().map(r -> (String)r[0]).collect(java.util.stream.Collectors.toSet());
            java.util.Map<String, Product> productMap = productRepository.findAllById(ids).stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, p->p));
            List<ProductResponse> result = new ArrayList<>();
            for (Object[] row : topIds) {
                String productId = (String) row[0];
                Product product = productMap.get(productId);
                if (product != null) {
                    ProductResponse resp = productMapper.toResponse(product);
                    if (resp != null) {
                        result.add(resp);
                    }
                }
            }
            if (result.isEmpty()) {
                log.debug("No sales data, returning top rated products");
                List<Product> topRated = productRepository.findAll(PageRequest.of(0, 5, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "rating")))
                        .getContent();
                List<ProductResponse> fallback = topRated.stream()
                        .map(productMapper::toResponse)
                        .collect(Collectors.toList());
                result.addAll(fallback);
            }
            log.info("Top products fetched: {}", result.size());
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch top products", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getRecentOrders() {
        log.debug("Fetching recent orders");
        try {
            List<Order> orders = orderRepository.findTop5ByOrderByCreatedAtDesc();
            List<OrderResponse> responses = new ArrayList<>();
            for (Order order : orders) {
                try {
                    List<OrderItem> items = orderItemRepository.findByOrderIdWithProduct(order.getId());
                    OrderResponse resp = orderMapper.toOrderResponse(order, items);
                    if (resp != null) {
                        responses.add(resp);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to map order {} for recent orders", order.getId(), ex);
                }
            }
            log.debug("Recent orders fetched: {}", responses.size());
            return responses;
        } catch (Exception e) {
            log.error("Failed to fetch recent orders", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getRecentUsers() {
        log.debug("Fetching recent users");
        try {
            List<User> users = userRepository.findTop5ByOrderByCreatedAtDesc();
            List<UserResponse> responses = users.stream()
                    .map(userMapper::toResponse)
                    .collect(Collectors.toList());
            log.debug("Recent users fetched: {}", responses.size());
            return responses;
        } catch (Exception e) {
            log.error("Failed to fetch recent users", e);
            throw e;
        }
    }
}
