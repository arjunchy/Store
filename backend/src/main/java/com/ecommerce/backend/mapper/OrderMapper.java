package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.response.OrderItemResponse;
import com.ecommerce.backend.dto.response.OrderResponse;
import com.ecommerce.backend.dto.response.OrderStatusHistoryResponse;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.entity.OrderItem;
import com.ecommerce.backend.entity.OrderStatusHistory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderItemResponse toOrderItemResponse(OrderItem item) {
        if (item == null || item.getProduct() == null) {
            return null;
        }
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getPrice(),
                item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        );
    }

    public OrderResponse toOrderResponse(Order order, List<OrderItem> items) {
        if (order == null) {
            return null;
        }
        List<OrderItemResponse> itemResponses = items.stream()
                .map(this::toOrderItemResponse)
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getDeliveryStatus(),
                itemResponses,
                order.getCreatedAt(),
                order.getUser() != null ? order.getUser().getUserId() : null,
                order.getUser() != null ? order.getUser().getEmail() : null,
                order.getUser() != null ? order.getUser().getUsername() : null
        ).withAmounts(
                order.getSubtotal(),
                order.getShippingCost(),
                order.getTax(),
                order.getPaymentMethod()
        );
    }

    public OrderStatusHistoryResponse toHistoryResponse(OrderStatusHistory history) {
        if (history == null) {
            return null;
        }
        return new OrderStatusHistoryResponse(
                history.getId(),
                history.getOrder() != null ? history.getOrder().getId() : null,
                history.getStatus(),
                history.getStatusType() != null ? history.getStatusType().name() : "ORDER",
                history.getNote(),
                history.getChangedBy(),
                history.getCreatedAt()
        );
    }
}
