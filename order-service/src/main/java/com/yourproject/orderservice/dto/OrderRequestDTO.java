package com.yourproject.orderservice.dto;

import lombok.Getter;
import lombok.Setter;

// This is a simple class to carry data for creating an order.
// We'll get this from the client in the request body.
@Getter
@Setter
public class OrderRequestDTO {
    private String shippingAddress;
    // In a real app, this would probably be a list of cart items,
    // but we'll keep it simple for now and assume the OrderService will fetch the cart.
}