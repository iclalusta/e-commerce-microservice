package com.example.cart_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field; // Import Field
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.client.RestTemplate;

import com.example.cart_service.event.ItemAddedToCartEvent; // Added import
import com.example.cart_service.model.Cart;
import com.example.cart_service.model.CartItem;
import com.example.cart_service.model.ProductDto;
import com.example.cart_service.repository.CartRepository;

class CartServiceTest {

    private CartService cartService;

    @Mock
    private CartRepository cartRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setup() throws NoSuchFieldException, IllegalAccessException { // Added exceptions
        MockitoAnnotations.openMocks(this);
        cartService = new CartService(cartRepository, restTemplate, rabbitTemplate);

        // Manually set the @Value fields using reflection for the test
        Field itemAddedExchangeField = CartService.class.getDeclaredField("itemAddedExchange");
        itemAddedExchangeField.setAccessible(true);
        itemAddedExchangeField.set(cartService, "cartExchange");

        Field itemAddedRoutingKeyField = CartService.class.getDeclaredField("itemAddedRoutingKey");
        itemAddedRoutingKeyField.setAccessible(true);
        itemAddedRoutingKeyField.set(cartService, "cart.item.added");
    }

    @Test
    void addItem_createsCartWhenMissing() {
        ProductDto product = new ProductDto();
        product.setId(1L);
        product.setPrice(new BigDecimal("10"));
        product.setStock(5);
        when(restTemplate.getForObject(any(String.class), eq(ProductDto.class))).thenReturn(product);
        when(cartRepository.findByCartIdentifier("user1")).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        Cart result = cartService.addItem("user1", 1L, 2);

        assertThat(result.getItems()).hasSize(1);
        CartItem item = result.getItems().getFirst();
        assertThat(item.getProductId()).isEqualTo(1L);
        assertThat(item.getQuantity()).isEqualTo(2);
        // Corrected verify statement to expect the hardcoded values
        verify(rabbitTemplate).convertAndSend(eq("cartExchange"), eq("cart.item.added"), any(com.example.cart_service.event.ItemAddedToCartEvent.class));
    }

    @Test
    void updateItemQuantity_zeroRemovesCart() {
        Cart cart = new Cart();
        cart.setCartIdentifier("user2");
        cart.getItems().add(new CartItem(2L, 1, new BigDecimal("3")));
        cart.setLastModifiedDate(LocalDateTime.now());
        when(cartRepository.findByCartIdentifier("user2")).thenReturn(Optional.of(cart));

        Cart returned = cartService.updateItemQuantity("user2", 2L, 0);

        assertThat(returned.getItems()).isEmpty();
        verify(cartRepository).delete(cart);
    }

    @Test
    void addItem_existingItemIncreasesQuantity() {
        Cart existing = new Cart();
        existing.setCartIdentifier("user1");
        existing.getItems().add(new CartItem(1L, 1, new BigDecimal("2")));
        when(cartRepository.findByCartIdentifier("user1")).thenReturn(Optional.of(existing));

        ProductDto product = new ProductDto();
        product.setId(1L);
        product.setPrice(new BigDecimal("2"));
        product.setStock(10);
        when(restTemplate.getForObject(any(String.class), eq(ProductDto.class))).thenReturn(product);
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        Cart result = cartService.addItem("user1", 1L, 2);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getQuantity()).isEqualTo(3);
    }

    @Test
    void removeItem_deletesCartWhenEmpty() {
        Cart cart = new Cart();
        cart.setCartIdentifier("user2");
        cart.getItems().add(new CartItem(2L, 1, new BigDecimal("3")));
        when(cartRepository.findByCartIdentifier("user2")).thenReturn(Optional.of(cart));

        Cart returned = cartService.removeItem("user2", 2L);

        assertThat(returned.getItems()).isEmpty();
        verify(cartRepository).delete(cart);
    }

    @Test
    void updateProduct_updatesCartPrices() {
        Cart cart = new Cart();
        cart.setCartIdentifier("c1");
        cart.getItems().add(new CartItem(1L, 1, new BigDecimal("1")));
        when(cartRepository.findByItemsProductId(1L)).thenReturn(List.of(cart));

        ProductDto update = new ProductDto();
        update.setId(1L);
        update.setPrice(new BigDecimal("5"));

        cartService.updateProduct(1L, update);

        assertThat(cart.getItems().getFirst().getPrice()).isEqualTo(new BigDecimal("5"));
        verify(cartRepository).save(cart);
    }

}
