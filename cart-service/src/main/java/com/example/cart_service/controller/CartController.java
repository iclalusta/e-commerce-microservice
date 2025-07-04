package com.example.cart_service.controller;

import com.example.cart_service.dto.CartMapper;
import com.example.cart_service.dto.CartResponseDTO;
import com.example.cart_service.model.Cart;
import com.example.cart_service.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart") // Standardizing on a single, user-centric base path
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * Adds an item to the authenticated user's cart.
     * The userId is securely passed in a header by the API Gateway.
     */
    @PostMapping("/items")
    public ResponseEntity<Cart> addItem(@RequestHeader("X-User-Id") Long userId,
                                        @RequestBody ItemRequest request) {
        Cart cart = cartService.addItem(String.valueOf(userId), request.productId(), request.quantity());
        return ResponseEntity.ok(cart);
    }

    /**
     * Gets the authenticated user's cart.
     */
    @GetMapping
    public ResponseEntity<CartResponseDTO> viewCart(@RequestHeader("X-User-Id") Long userId) {
        return cartService.getCart(String.valueOf(userId))
                .map(cart -> ResponseEntity.ok(CartMapper.toDto(userId, cart)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates an item's quantity in the authenticated user's cart.
     */
    @PutMapping("/items/{productId}")
    public ResponseEntity<Cart> updateQuantity(@RequestHeader("X-User-Id") Long userId,
                                               @PathVariable Long productId,
                                               @RequestBody QuantityRequest request) {
        // The cartIdentifier is now the same as the userId from the header
        Cart cart = cartService.updateItemQuantity(String.valueOf(userId), productId, request.quantity());
        return ResponseEntity.ok(cart);
    }

    /**
     * Removes an item from the authenticated user's cart.
     */
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Cart> removeItem(@RequestHeader("X-User-Id") Long userId,
                                           @PathVariable Long productId) {
        // The cartIdentifier is now the same as the userId from the header
        Cart cart = cartService.removeItem(String.valueOf(userId), productId);
        return ResponseEntity.ok(cart);
    }

    // Static inner records for request bodies remain unchanged
    static record ItemRequest(Long productId, int quantity) {}
    static record QuantityRequest(int quantity) {}
}