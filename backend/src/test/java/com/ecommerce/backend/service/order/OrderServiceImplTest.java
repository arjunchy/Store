package com.ecommerce.backend.service.order;

import com.ecommerce.backend.dto.request.OrderRequest;
import com.ecommerce.backend.dto.response.OrderItemResponse;
import com.ecommerce.backend.dto.response.OrderResponse;
import com.ecommerce.backend.dto.response.OrderStatusHistoryResponse;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.enums.OrderPaymentStatus;
import com.ecommerce.backend.enums.DeliveryStatus;
import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.mapper.OrderMapper;
import com.ecommerce.backend.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Product product;
    private Address address;
    private Cart cart;
    private CartItem cartItem;
    private Order order;
    private Pageable pageable;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        user = User.builder().userId("user-1").build();
        product = Product.builder().id("prod-1").name("Laptop").price(new BigDecimal("100.00")).stockQuantity(10).build();
        address = Address.builder().id("addr-1").user(user).label("Home").street("123 Main St").city("Springfield").state("IL").postalCode("62701").country("US").build();
        cartItem = CartItem.builder().id("item-1").product(product).quantity(2).build();
        cart = Cart.builder().id("cart-1").user(user).cartItems(new ArrayList<>(List.of(cartItem))).build();
        order = Order.builder().id("ord-1").orderNumber("ORD-1234").user(user).totalAmount(new BigDecimal("200.00")).status(OrderStatus.PENDING).paymentStatus(OrderPaymentStatus.PENDING).build();
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void placeOrder_success() throws Exception {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        when(addressRepository.findById("addr-1")).thenReturn(Optional.of(address));
        doAnswer(inv -> "{\"street\":\"123 Main St\"}").when(objectMapper).writeValueAsString(any());
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(productRepository.findByIdForUpdate("prod-1")).thenReturn(Optional.of(product));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(mock(OrderItem.class));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(orderItemRepository.findByOrderIdWithProduct("ord-1")).thenReturn(List.of());
        when(orderMapper.toOrderResponse(any(), anyList())).thenReturn(
                new OrderResponse("ord-1", "ORD-1234", new BigDecimal("200.00"), OrderStatus.PENDING, OrderPaymentStatus.PENDING, DeliveryStatus.PLACED, List.of(), LocalDateTime.now(), null, null, null)
        );

        OrderRequest request = new OrderRequest("addr-1");
        OrderResponse response = orderService.placeOrder("user-1", request);

        assertThat(response).isNotNull();
        assertThat(response.orderNumber()).isEqualTo("ORD-1234");
        verify(orderRepository, times(2)).save(any());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void placeOrder_userNotFound_throws() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder("missing", new OrderRequest("addr-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void placeOrder_emptyCart_throws() {
        Cart emptyCart = Cart.builder().id("cart-2").user(user).cartItems(new ArrayList<>()).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(emptyCart));

        assertThatThrownBy(() -> orderService.placeOrder("user-1", new OrderRequest("addr-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cart is empty");
    }

    @Test
    void placeOrder_noCart_throws() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder("user-1", new OrderRequest("addr-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cart is empty");
    }

    @Test
    void placeOrder_addressNotFound_throws() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        when(addressRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder("user-1", new OrderRequest("missing")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Address not found");
    }

    @Test
    void placeOrder_wrongAddressOwner_throws() {
        User otherUser = User.builder().userId("user-2").build();
        Address otherAddress = Address.builder().id("addr-2").user(otherUser).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        when(addressRepository.findById("addr-2")).thenReturn(Optional.of(otherAddress));

        assertThatThrownBy(() -> orderService.placeOrder("user-1", new OrderRequest("addr-2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Address not found");
    }

    @Test
    void placeOrder_insufficientStock_throws() throws Exception {
        Product lowStockProduct = Product.builder().id("prod-2").name("Phone").price(BigDecimal.TEN).stockQuantity(1).build();
        CartItem lowStockItem = CartItem.builder().id("item-2").product(lowStockProduct).quantity(5).build();
        Cart lowStockCart = Cart.builder().id("cart-3").user(user).cartItems(new ArrayList<>(List.of(lowStockItem))).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(lowStockCart));
        when(addressRepository.findById("addr-1")).thenReturn(Optional.of(address));
        doAnswer(inv -> "{}").when(objectMapper).writeValueAsString(any());
        when(orderRepository.save(any())).thenReturn(order);
        when(productRepository.findByIdForUpdate("prod-2")).thenReturn(Optional.of(lowStockProduct));
        when(productRepository.findById("prod-2")).thenReturn(Optional.of(lowStockProduct));

        assertThatThrownBy(() -> orderService.placeOrder("user-1", new OrderRequest("addr-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void getOrderForUser_success() {
        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdWithProduct("ord-1")).thenReturn(List.of());
        when(orderMapper.toOrderResponse(any(), anyList())).thenReturn(
                new OrderResponse("ord-1", "ORD-1234", BigDecimal.TEN, OrderStatus.PENDING, OrderPaymentStatus.PENDING, DeliveryStatus.PLACED, List.of(), LocalDateTime.now(), null, null, null)
        );

        OrderResponse response = orderService.getOrderForUser("ord-1", "user-1");

        assertThat(response.id()).isEqualTo("ord-1");
    }

    @Test
    void getOrderForUser_notFound_throws() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderForUser("missing", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void getOrderForUser_wrongOwner_throws() {
        User otherUser = User.builder().userId("user-2").build();
        Order otherOrder = Order.builder().id("ord-2").user(otherUser).build();
        when(orderRepository.findById("ord-2")).thenReturn(Optional.of(otherOrder));

        assertThatThrownBy(() -> orderService.getOrderForUser("ord-2", "user-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAllOrders_success() {
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1);
        when(orderRepository.findByUserId("user-1", pageable)).thenReturn(page);
        when(orderItemRepository.findByOrderIdWithProduct("ord-1")).thenReturn(List.of());
        when(orderMapper.toOrderResponse(any(), anyList())).thenReturn(
                new OrderResponse("ord-1", "ORD-1234", BigDecimal.TEN, OrderStatus.PENDING, OrderPaymentStatus.PENDING, DeliveryStatus.PLACED, List.of(), LocalDateTime.now(), null, null, null)
        );

        Page<OrderResponse> result = orderService.getAllOrders("user-1", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void updateOrderStatus_pendingToProcessing_success() {
        order.setStatus(OrderStatus.PROCESSING);
        when(orderRepository.findByIdForUpdate("ord-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        when(orderItemRepository.findByOrderIdWithProduct("ord-1")).thenReturn(List.of());
        when(orderMapper.toOrderResponse(any(), anyList())).thenReturn(
                new OrderResponse("ord-1", "ORD-1234", BigDecimal.TEN, OrderStatus.CONFIRMED, OrderPaymentStatus.PENDING, DeliveryStatus.PLACED, List.of(), LocalDateTime.now(), null, null, null)
        );

        OrderResponse response = orderService.updateOrderStatus("ord-1", OrderStatus.CONFIRMED);

        assertThat(response).isNotNull();
        verify(orderStatusHistoryRepository).save(any());
    }

    @Test
    void updateOrderStatus_pendingToCancelled_success() {
        order.setStatus(OrderStatus.PROCESSING);
        when(orderRepository.findByIdForUpdate("ord-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        when(orderItemRepository.findByOrderIdWithProduct("ord-1")).thenReturn(List.of());
        when(orderMapper.toOrderResponse(any(), anyList())).thenReturn(
                new OrderResponse("ord-1", "ORD-1234", BigDecimal.TEN, OrderStatus.CANCELED, OrderPaymentStatus.PENDING, DeliveryStatus.PLACED, List.of(), LocalDateTime.now(), null, null, null)
        );

        OrderResponse response = orderService.updateOrderStatus("ord-1", OrderStatus.CANCELED, "Changed mind", "user-1");

        assertThat(response).isNotNull();
    }

    @Test
    void updateOrderStatus_invalidTransition_throws() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findByIdForUpdate("ord-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus("ord-1", OrderStatus.PROCESSING))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void updateOrderStatus_deliveredIsTerminal_throws() {
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findByIdForUpdate("ord-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus("ord-1", OrderStatus.SHIPPED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateOrderStatus_cancelledIsTerminal_throws() {
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findByIdForUpdate("ord-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus("ord-1", OrderStatus.PENDING))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateOrderStatus_notFound_throws() {
        when(orderRepository.findByIdForUpdate("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus("missing", OrderStatus.PROCESSING))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void getOrderHistory_success() {
        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(order));
        OrderStatusHistory history = OrderStatusHistory.builder().id("hist-1").order(order).status(OrderStatus.PENDING).note("Placed").changedBy("user-1").createdAt(LocalDateTime.now()).build();
        when(orderStatusHistoryRepository.findByOrderId("ord-1")).thenReturn(List.of(history));
        when(orderMapper.toHistoryResponse(history)).thenReturn(
                new OrderStatusHistoryResponse("hist-1", "ord-1", OrderStatus.PENDING, "ORDER", "Placed", "user-1", LocalDateTime.now())
        );

        List<OrderStatusHistoryResponse> result = orderService.getOrderHistory("ord-1", "user-1");

        assertThat(result).hasSize(1);
    }

    @Test
    void getOrderHistory_wrongOwner_throws() {
        User otherUser = User.builder().userId("user-2").build();
        Order otherOrder = Order.builder().id("ord-2").user(otherUser).build();
        when(orderRepository.findById("ord-2")).thenReturn(Optional.of(otherOrder));

        assertThatThrownBy(() -> orderService.getOrderHistory("ord-2", "user-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
