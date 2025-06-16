package com.example.cart_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.cart_service.model.Cart;
import com.example.cart_service.service.CartService;
import com.example.cart_service.dto.CartMapper;
import com.example.cart_service.dto.CartResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/api/carts", "/api/cart"})
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/{userId}/items")
    public ResponseEntity<Cart> addItem(@PathVariable Long userId,
                                        @RequestBody ItemRequest request) {
        Cart cart = cartService.addItem(String.valueOf(userId), request.productId(), request.quantity());
        return ResponseEntity.ok(cart);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CartResponseDTO> viewCart(@PathVariable Long userId) {
        return cartService.getCart(String.valueOf(userId))
                .map(cart -> ResponseEntity.ok(CartMapper.toDto(userId, cart)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{cartIdentifier}/items/{productId}")
    public ResponseEntity<Cart> updateQuantity(@PathVariable String cartIdentifier,
                                               @PathVariable Long productId,
                                               @RequestBody QuantityRequest request) {
        Cart cart = cartService.updateItemQuantity(cartIdentifier, productId, request.quantity());
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/{cartIdentifier}/items/{productId}")
    public ResponseEntity<Cart> removeItem(@PathVariable String cartIdentifier,
                                           @PathVariable Long productId) {
        Cart cart = cartService.removeItem(cartIdentifier, productId);
        return ResponseEntity.ok(cart);
    }

    static record ItemRequest(Long productId, int quantity) {}
    static record QuantityRequest(int quantity) {}
}
