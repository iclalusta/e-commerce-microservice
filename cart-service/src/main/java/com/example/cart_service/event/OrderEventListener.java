package com.example.cart_service.event;

import com.example.cart_service.dto.OrderCreatedEvent;
import com.example.cart_service.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @Autowired
    private CartService cartService;

    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void onOrderCreated(OrderCreatedEvent event) {
        if (event == null || event.getUserId() == null) {
            log.warn("Received an incomplete order created event. Ignoring.");
            return;
        }

        log.info("Order created event received for user: {}", event.getUserId());
        try {
            // --- DÜZELTME BURADA ---
            // Long tipindeki userId, String.valueOf() ile String'e çevrildi.
            cartService.clearCart(String.valueOf(event.getUserId()));

            log.info("Cart cleared successfully for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Error clearing cart for user: " + event.getUserId(), e);
        }
    }
}