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

import org.slf4j.Logger; // YENİ
import org.slf4j.LoggerFactory; // YENİ


@Service
@RequiredArgsConstructor
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class); // YENİ
    private final CartRepository cartRepository;
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;



    @Value("${product.service.url}")
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

        //ItemAddedToCartEvent event = new ItemAddedToCartEvent(cartIdentifier, productId, quantity);
        //rabbitTemplate.convertAndSend(itemAddedExchange, itemAddedRoutingKey, event);

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

    /*public void updateProduct(Long productId, ProductDto update) {
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
    }*/

    public void updateProductInCarts(Long productId, ProductDto productUpdate) {
        // Bu productId'yi içeren tüm sepetleri bul
        List<Cart> cartsToUpdate = cartRepository.findByItemsProductId(productId);
 
        if (cartsToUpdate.isEmpty()) {
            log.info("No carts found containing product ID: {}. No update needed.", productId);
            return;
        }
 
        // --- YENİ EKLENEN STOK KONTROL MANTIĞI ---
        if (productUpdate.getStock() <= 0) {
            // **Durum 1: Stok SIFIR veya daha az ise, ürünü tüm sepetlerden sil.**
            log.warn("Stock for product ID: {} is zero or less. Removing this item from all carts.", productId);
 
            for (Cart cart : cartsToUpdate) {
                // Ürünü sepetin item listesinden predicate kullanarak kaldır
                boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(productId));
 
                if (removed) {
                    log.info("Removed product ID: {} from cart with identifier: {}", productId, cart.getCartIdentifier());
 
                    // Ürün kaldırıldıktan sonra sepetin boş olup olmadığını kontrol et
                    if (cart.getItems().isEmpty()) {
                        // Eğer sepet artık boşsa, bu sepeti veritabanından tamamen sil
                        cartRepository.delete(cart);
                        log.info("Cart with identifier: {} is now empty and has been deleted.", cart.getCartIdentifier());
                    } else {
                        // Eğer sepette başka ürünler varsa, sepetin son halini kaydet
                        cart.setLastModifiedDate(LocalDateTime.now());
                        cartRepository.save(cart);
                    }
                }
            }
        } else {
            // **Durum 2: Stok VARSA, mevcut mantığı uygula (sadece fiyatı güncelle).**
            log.info("Stock for product ID: {} is available. Updating price in relevant carts.", productId);
 
            for (Cart cart : cartsToUpdate) {
                boolean priceUpdated = false;
                for (CartItem item : cart.getItems()) {
                    if (item.getProductId().equals(productId)) {
                        item.setPrice(productUpdate.getPrice()); // Ürünün yeni fiyatını ayarla
                        priceUpdated = true;
                    }
                }
                if (priceUpdated) {
                    cart.setLastModifiedDate(LocalDateTime.now());
                    cartRepository.save(cart);
                    log.info("Updated price for product ID: {} in cart with identifier: {}", productId, cart.getCartIdentifier());
                }
            }
        }
    }
}
