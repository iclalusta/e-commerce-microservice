// File: src/main/java/com/yourproject/orderservice/dto/CartResponseDTO.java
package com.yourproject.orderservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class CartResponseDTO {
    private Long userId;
    private List<CartItemDTO> items;
}