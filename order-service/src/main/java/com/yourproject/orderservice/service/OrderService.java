// File: src/main/java/com/yourproject/orderservice/service/OrderService.java
package com.yourproject.orderservice.service;

import com.yourproject.orderservice.dto.CartResponseDTO;
import com.yourproject.orderservice.dto.OrderRequestDTO;
import com.yourproject.orderservice.dto.PaymentRequestDTO;
import com.yourproject.orderservice.dto.PaymentResponseDTO;
import com.yourproject.orderservice.dto.OrderCreatedEvent;
import com.yourproject.orderservice.dto.OrderItemDTO;
import com.yourproject.orderservice.model.Order;
import com.yourproject.orderservice.model.OrderItem;
import com.yourproject.orderservice.model.OrderStatus;
import com.yourproject.orderservice.repository.OrderRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service // Tells Spring this is a Service class
public class OrderService {

    @Autowired // Injects the repository we created in Step 3
    private OrderRepository orderRepository;

    @Autowired // Injects the WebClient Builder we configured
    private WebClient.Builder webClientBuilder;

    @Autowired // Injects the RabbitTemplate for sending messages
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    @Value("${CART_SERVICE_URI}")
    private String shoppingCartServiceUrl;

    @Value("${PAYMENT_SERVICE_URI}")
    private String paymentServiceUrl;


    @Transactional
    public Order createOrder(OrderRequestDTO orderRequest, Long userId) {
        CartResponseDTO cart = webClientBuilder.build()
                .get()
                .uri(shoppingCartServiceUrl + "/api/cart")
                .header("X-User-Id", String.valueOf(userId))
                .retrieve()
                .bodyToMono(CartResponseDTO.class)
                .block();

        if (cart == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Shopping cart is empty, cannot create order.");
        }

        Order newOrder = new Order();
        newOrder.setUserId(userId);
        newOrder.setShippingAddress(orderRequest.getShippingAddress());
        newOrder.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (var cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtTimeOfOrder(cartItem.getPrice());
            newOrder.addOrderItem(orderItem);

            totalAmount = totalAmount.add(cartItem.getPrice().multiply(new BigDecimal(cartItem.getQuantity())));
        }
        newOrder.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(newOrder);

        PaymentRequestDTO paymentRequest = new PaymentRequestDTO(savedOrder.getId(), totalAmount);
        PaymentResponseDTO paymentResponse = webClientBuilder.build()
                .post()
                .uri(paymentServiceUrl + "/api/payments")
                .bodyValue(paymentRequest)
                .retrieve()
                .bodyToMono(PaymentResponseDTO.class)
                .block();

        System.out.println("*******************");
        System.out.println(paymentResponse);
        System.out.println("*****************************************");

        if (paymentResponse == null || !paymentResponse.isSuccess()) {
            throw new IllegalStateException("Payment failed, rolling back order creation.");
        }

        savedOrder.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(savedOrder);

        // --- YENİ VE DOĞRU KISIM ---
        // Gerekli servislerin (ProductService, CartService) kullanması için zengin bir event fırlat
        List<OrderItemDTO> orderItemsForEvent = savedOrder.getOrderItems().stream()
                .map(orderItem -> new OrderItemDTO(
                        orderItem.getProductId(),
                        orderItem.getQuantity(),
                        orderItem.getPriceAtTimeOfOrder()))
                .collect(Collectors.toList());

        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getUserId(),
                orderItemsForEvent
        );

        rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
        System.out.println("Published enriched OrderCreatedEvent for order ID: " + savedOrder.getId());
        // --- YENİ VE DOĞRU KISIM SONU ---

        return savedOrder;
    }

    public Optional<Order> findOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    public List<Order> findOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }
}