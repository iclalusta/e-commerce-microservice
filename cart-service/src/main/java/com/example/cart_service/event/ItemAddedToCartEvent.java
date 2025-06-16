package com.example.cart_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemAddedToCartEvent {
    private String cartIdentifier;
    private Long productId;
    private int quantityAdded;
}
