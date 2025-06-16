package com.example.cart_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.cart_service.event.ItemAddedToCartEvent;
import com.example.cart_service.model.Cart;
import com.example.cart_service.model.CartItem;
import com.example.cart_service.model.ProductDto;
import com.example.cart_service.repository.CartRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${product.service.url:http://localhost:8080}")
    private String productServiceUrl;

    @Value("${cart.item.added.exchange:cartExchange}")
    private String itemAddedExchange;

    @Value("${cart.item.added.routingKey:cart.item.added}")
    private String itemAddedRoutingKey;

    public Cart addItem(String cartIdentifier, Long productId, int quantity) {
        ProductDto product = restTemplate.getForObject(productServiceUrl + "/api/products/" + productId, ProductDto.class);
        if (product == null) {
            throw new IllegalArgumentException("Product not found");
        }
        if (product.getStock() < quantity) {
            throw new IllegalArgumentException("Insufficient stock");
        }

        Cart cart = cartRepository.findByCartIdentifier(cartIdentifier)
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setCartIdentifier(cartIdentifier);
                    return c;
                });

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        if (existing.isPresent()) {
            int newQty = existing.get().getQuantity() + quantity;
            if (newQty > product.getStock()) {
                throw new IllegalArgumentException("Insufficient stock");
            }
            existing.get().setQuantity(newQty);
            existing.get().setPrice(product.getPrice());
        } else {
            cart.getItems().add(new CartItem(productId, quantity, product.getPrice()));
        }
        cart.setLastModifiedDate(LocalDateTime.now());
        Cart saved = cartRepository.save(cart);

        ItemAddedToCartEvent event = new ItemAddedToCartEvent(cartIdentifier, productId, quantity);
        rabbitTemplate.convertAndSend(itemAddedExchange, itemAddedRoutingKey, event);

        return saved;
    }

    public Optional<Cart> getCart(String cartIdentifier) {
        return cartRepository.findByCartIdentifier(cartIdentifier);
    }

    public Cart updateItemQuantity(String cartIdentifier, Long productId, int quantity) {
        Cart cart = cartRepository.findByCartIdentifier(cartIdentifier)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

        Optional<CartItem> itemOpt = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();
        if (itemOpt.isEmpty()) {
            throw new IllegalArgumentException("Item not found in cart");
        }
        CartItem item = itemOpt.get();
        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(quantity);
        }
        cart.setLastModifiedDate(LocalDateTime.now());
        if (cart.getItems().isEmpty()) {
            cartRepository.delete(cart);
            return cart;
        }
        return cartRepository.save(cart);
    }

    public Cart removeItem(String cartIdentifier, Long productId) {
        Cart cart = cartRepository.findByCartIdentifier(cartIdentifier)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));
        cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        cart.setLastModifiedDate(LocalDateTime.now());
        if (cart.getItems().isEmpty()) {
            cartRepository.delete(cart);
            return cart;
        }
        return cartRepository.save(cart);
    }

    public void clearCart(String cartIdentifier) {
        cartRepository.deleteByCartIdentifier(cartIdentifier);
    }

    public void updateProduct(Long productId, ProductDto update) {
        List<Cart> carts = cartRepository.findByItemsProductId(productId);
        for (Cart cart : carts) {
            cart.getItems().forEach(item -> {
                if (item.getProductId().equals(productId)) {
                    item.setPrice(update.getPrice());
                }
            });
            cart.setLastModifiedDate(LocalDateTime.now());
            cartRepository.save(cart);
        }
    }
}
