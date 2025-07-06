package com.example.productservice.event;

import com.example.productservice.dto.OrderCreatedEvent;
import com.example.productservice.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @Autowired
    private ProductService productService;

    // @RabbitListener anotasyonunun application.properties'teki doğru kuyruk adını kullandığından emin olun.
    @RabbitListener(queues = "${rabbitmq.queue.stock-decrease}")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Order created event received for stock update. Order ID: {}", event.getOrderId());
        try {
            event.getItems().forEach(item -> {
                log.info("Decreasing stock for product ID: {} by quantity: {}", item.getProductId(), item.getQuantity());
                productService.decreaseStock(item.getProductId(), item.getQuantity());
            });
            log.info("Stock update successful for order ID: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error processing stock update for order ID: " + event.getOrderId(), e);
            // Burada hatayı telafi etme mekanizmaları (örneğin, mesajı başka bir kuyruğa gönderme) eklenebilir.
        }
    }
}