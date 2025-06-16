package com.example.cart_service.dto;

import com.example.cart_service.model.Cart;

public final class CartMapper {
    private CartMapper() {}

    public static CartResponseDTO toDto(Long userId, Cart cart) {
        CartResponseDTO dto = new CartResponseDTO();
        dto.setUserId(userId);
        dto.setItems(cart.getItems().stream().map(item -> {
            CartItemDTO ci = new CartItemDTO();
            ci.setProductId(item.getProductId());
            ci.setQuantity(item.getQuantity());
            ci.setPrice(item.getPrice());
            return ci;
        }).toList());
        return dto;
    }
}
