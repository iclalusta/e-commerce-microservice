// File: src/main/java/com/yourproject/orderservice/dto/CartItemDTO.java
package com.yourproject.orderservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data // Lombok annotation for getters, setters, toString, etc.
public class CartItemDTO {
    private Long productId;
    private Integer quantity;
    private BigDecimal price; // Assuming cart service gets price from product service
}