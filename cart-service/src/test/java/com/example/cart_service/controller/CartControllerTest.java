package com.example.cart_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.cart_service.model.Cart;
import com.example.cart_service.model.CartItem;
import com.example.cart_service.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;

class CartControllerTest {

    @Mock
    private CartService cartService;

    private MockMvc mockMvc;

    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        CartController controller = new CartController(cartService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void addItem_returnsCart() throws Exception {
        Cart cart = new Cart();
        cart.setCartIdentifier("1");
        cart.getItems().add(new CartItem(1L, 1, new BigDecimal("2")));
        when(cartService.addItem(any(), any(), anyInt())).thenReturn(cart);

        String body = mapper.writeValueAsString(new CartController.ItemRequest(1L, 1));
        mockMvc.perform(post("/api/carts/1/items").contentType(MediaType.APPLICATION_JSON).content(body))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.cartIdentifier").value("1"));
    }

    @Test
    void viewCart_notFound() throws Exception {
        when(cartService.getCart("99")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/carts/99"))
               .andExpect(status().isNotFound());
    }
}
