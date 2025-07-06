// Paket adı kendi servisinize göre olmalı (örn: com.example.cart_service.dto)
package com.example.productservice.dto;

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
    private Long userId;
    private List<OrderItemDTO> items;
}