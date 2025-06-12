package com.yourproject.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
// Serializable is important for the object to be converted and sent over the network
public class OrderCreatedEvent implements Serializable {

    private Long orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private List<OrderItemDTO> items; // Let's create this small DTO too

    // You can add any other details the consumer services might need
}