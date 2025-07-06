// Paket adı kendi servisinize göre olmalı (örn: com.example.cart_service.dto)
package com.example.cart_service.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDTO implements Serializable {
    private Long productId;
    private Integer quantity;
    private BigDecimal priceAtTimeOfOrder;
}