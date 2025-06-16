package com.example.cart_service.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CartItemDTO {
    private Long productId;
    private Integer quantity;
    private BigDecimal price;
}
