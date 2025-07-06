package com.yourproject.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreatedEvent implements Serializable {
    private Long orderId;
    private Long userId; // Sepet servisi için
    private List<OrderItemDTO> items; // Ürün servisi için
}