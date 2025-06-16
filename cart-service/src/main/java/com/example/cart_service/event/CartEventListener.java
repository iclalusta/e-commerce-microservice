package com.example.cart_service.event;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.cart_service.model.ProductDto;
import com.example.cart_service.service.CartService;
import com.example.cart_service.dto.OrderCreatedEvent;
import com.example.cart_service.event.ProductUpdatedEvent;


import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CartEventListener {

    private final CartService cartService;

    @RabbitListener(queues = "order.created.queue")
    public void handleOrderCreated(OrderCreatedEvent event) {
        if (event.getUserId() != null) {
            cartService.clearCart(String.valueOf(event.getUserId()));
        }
    }

    @RabbitListener(queues = "product.updated.queue")
    public void handleProductUpdated(ProductUpdatedEvent event) {
        ProductDto dto = new ProductDto();
        dto.setId(event.getProductId());
        dto.setPrice(event.getNewPrice());
        cartService.updateProduct(event.getProductId(), dto);
    }
}
