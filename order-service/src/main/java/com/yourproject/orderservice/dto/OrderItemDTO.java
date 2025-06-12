// File: src/main/java/com/yourproject/orderservice/dto/OrderItemDTO.java
package com.yourproject.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDTO implements Serializable {
    private Long productId;
    private Integer quantity;
    private BigDecimal priceAtTimeOfOrder;
}