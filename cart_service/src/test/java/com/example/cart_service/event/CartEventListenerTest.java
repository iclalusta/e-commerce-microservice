package com.example.cart_service.event;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.argThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.cart_service.event.ProductUpdatedEvent;
import com.example.cart_service.dto.OrderCreatedEvent;

import com.example.cart_service.service.CartService;

class CartEventListenerTest {

    private CartEventListener listener;

    @Mock
    private CartService cartService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        listener = new CartEventListener(cartService);
    }

    @Test
    void handleOrderCreated_clearsCart() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setUserId(1L);

        listener.handleOrderCreated(event);

        verify(cartService).clearCart("1");
    }

    @Test
    void handleProductUpdated_updatesProduct() {
        ProductUpdatedEvent event = new ProductUpdatedEvent();
        event.setProductId(1L);
        event.setNewPrice(new BigDecimal("9.99"));

        listener.handleProductUpdated(event);

        verify(cartService).updateProduct(eq(1L),
                argThat(dto -> new BigDecimal("9.99").compareTo(dto.getPrice()) == 0
                        && Long.valueOf(1L).equals(dto.getId())));
    }
}
