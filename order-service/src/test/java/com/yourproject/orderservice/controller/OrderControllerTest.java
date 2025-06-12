package com.yourproject.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourproject.orderservice.dto.OrderRequestDTO;
import com.yourproject.orderservice.model.Order;
import com.yourproject.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;

// Loads the full Spring context for a test
@SpringBootTest
// Automatically configures MockMvc
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc; // A tool to perform fake HTTP requests

    @Autowired
    private ObjectMapper objectMapper; // A tool to convert Java objects to JSON strings

    @MockBean // Creates a mock version of the OrderService in the Spring context
    private OrderService orderService;

    @Test
    void whenPostOrder_thenCreateOrderAndReturn201() throws Exception {
        // --- 1. ARRANGE ---
        // The data we will send in our request
        OrderRequestDTO orderRequest = new OrderRequestDTO();
        orderRequest.setUserId(1L);
        orderRequest.setShippingAddress("123 Test St");

        // The Order object that we expect our mocked service to return
        Order mockOrderResponse = new Order();
        mockOrderResponse.setId(1L);
        mockOrderResponse.setUserId(1L);
        mockOrderResponse.setTotalAmount(new BigDecimal("100.00"));

        // Tell our mocked OrderService what to do when its createOrder method is called
        Mockito.when(orderService.createOrder(Mockito.any(OrderRequestDTO.class))).thenReturn(mockOrderResponse);

        // --- 2. ACT & 3. ASSERT ---
        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                // Check if the HTTP status is 201 CREATED
                .andExpect(MockMvcResultMatchers.status().isCreated())
                // Check if the returned JSON has the correct order ID
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L))
                // Check if the returned JSON has the correct user ID
                .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(1L));
    }
}