package com.ecommerce.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record AdminStatsResponse(
        long totalUsers,
        long totalProducts,
        long totalOrders,
        BigDecimal totalRevenue,
        long pendingOrders,
        long outOfStockProducts,
        List<OrderResponse> recentOrders,
        List<UserResponse> recentUsers
) {
}
