package com.example.cart_service.dto;

import java.util.List;
import lombok.Data;

@Data
public class CartResponseDTO {
    private Long userId;
    private List<CartItemDTO> items;
}
