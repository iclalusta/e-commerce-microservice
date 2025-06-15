package com.example.cart_service.event;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductUpdatedEvent {
    private Long productId;
    private String newName;
    private BigDecimal newPrice;
    private int newStock;
}
