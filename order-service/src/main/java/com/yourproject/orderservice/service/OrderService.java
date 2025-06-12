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

    @Value("${service.url.shopping-cart}")
    private String shoppingCartServiceUrl;

    @Value("${service.url.payment}")
    private String paymentServiceUrl;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    private String routingKey = "order.created";

    @Transactional // This annotation makes the whole method a single database transaction.
                   // If any part fails, all database changes will be rolled back.
    public Order createOrder(OrderRequestDTO orderRequest) {
        CartResponseDTO cart = webClientBuilder.build()
                .get()
                .uri(shoppingCartServiceUrl + "/{userId}", orderRequest.getUserId()) // Assuming this endpoint exists
                .retrieve()
                .bodyToMono(CartResponseDTO.class)
                .block(); // .block() makes it synchronous. In a fully reactive app, you wouldn't do this.

        if (cart == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Shopping cart is empty, cannot create order.");
        }

        // Create the order object from the cart details
        Order newOrder = new Order();
        newOrder.setUserId(orderRequest.getUserId());
        newOrder.setShippingAddress(orderRequest.getShippingAddress());
        newOrder.setStatus(OrderStatus.PENDING); // Initial status

        // Calculate total amount and add order items
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (var cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtTimeOfOrder(cartItem.getPrice());
            newOrder.addOrderItem(orderItem); // Using our helper method

            totalAmount = totalAmount.add(cartItem.getPrice().multiply(new BigDecimal(cartItem.getQuantity())));
        }
        newOrder.setTotalAmount(totalAmount);

        // For now, let's save the order first to get an ID for the payment request
        Order savedOrder = orderRepository.save(newOrder);

        // Step 2: Call the (mock) Payment Service
        PaymentRequestDTO paymentRequest = new PaymentRequestDTO(savedOrder.getId(), totalAmount);
        PaymentResponseDTO paymentResponse = webClientBuilder.build()
                .post()
                .uri(paymentServiceUrl + "/process") 
                .bodyValue(paymentRequest)
                .retrieve()
                .bodyToMono(PaymentResponseDTO.class)
                .block();

        if (paymentResponse == null || !paymentResponse.isSuccess()) {
            // If payment fails, we throw an exception, and because of @Transactional,
            // the saved order will be rolled back from the database.
            throw new IllegalStateException("Payment failed, rolling back order creation.");
        }

        // If payment succeeded, update order status and save again
        savedOrder.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(savedOrder);
        

        // Step 3: Publish the OrderCreatedEvent to RabbitMQ
        // The routing key "order.created" can be used by RabbitMQ to send the message
        // to the correct queues (e.g., for notification and product services).
        OrderCreatedEvent event = new OrderCreatedEvent(
            savedOrder.getId(),
            savedOrder.getUserId(),
            savedOrder.getTotalAmount(),
            // Convert your OrderItem entities to OrderItemDTOs for the event
            savedOrder.getOrderItems().stream()
                    .map(item -> new OrderItemDTO(item.getProductId(), item.getQuantity(), item.getPriceAtTimeOfOrder()))
                    .collect(Collectors.toList())
        );
        
        rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
        System.out.println("Published OrderCreatedEvent for order ID: " + savedOrder.getId());


        return savedOrder;
    }

    public Optional<Order> findOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    public List<Order> findOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }
}