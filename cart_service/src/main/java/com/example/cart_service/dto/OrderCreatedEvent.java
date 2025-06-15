package com.example.cart_service.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class OrderCreatedEvent implements Serializable {
    private Long orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private List<OrderItemDTO> items;
}
