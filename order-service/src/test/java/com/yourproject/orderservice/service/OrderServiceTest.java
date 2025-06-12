package com.yourproject.orderservice.service;

import com.yourproject.orderservice.dto.CartResponseDTO;
import com.yourproject.orderservice.dto.OrderRequestDTO;
import com.yourproject.orderservice.dto.PaymentResponseDTO;
import com.yourproject.orderservice.model.Order;
import com.yourproject.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private WebClient webClient;

    // --- Mocks for the WebClient Fluent API chain ---
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private OrderService orderService;

    // This setup method now correctly mocks each step for both GET and POST
    @BeforeEach
    void setUp() {
        // Shared setup
        Mockito.when(webClientBuilder.build()).thenReturn(webClient);

        // --- Mocking for GET requests ---
        Mockito.when(webClient.get()).thenReturn(requestHeadersUriSpec);
        Mockito.when(requestHeadersUriSpec.uri(Mockito.anyString(), Mockito.anyLong())).thenReturn(requestHeadersSpec);
        Mockito.when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // --- Mocking for POST requests ---
        Mockito.when(webClient.post()).thenReturn(requestBodyUriSpec);
        Mockito.when(requestBodyUriSpec.uri(Mockito.anyString())).thenReturn(requestBodySpec);
        Mockito.when(requestBodySpec.bodyValue(Mockito.any())).thenReturn(requestHeadersSpec);
        // The retrieve() call for POST also returns a responseSpec, so we can reuse the line from the GET setup
        // Mockito.when(requestHeadersSpec.retrieve()).thenReturn(responseSpec); // This is already covered above

    }


    @Test
    void whenCreateOrder_andPaymentSucceeds_thenOrderShouldBeSavedAndEventPublished() {
        // --- 1. ARRANGE (Set up the test scenario) ---

        OrderRequestDTO orderRequest = new OrderRequestDTO();
        orderRequest.setUserId(1L);

        // Mock the response from the Shopping Cart Service (which uses GET)
        CartResponseDTO cartResponse = new CartResponseDTO();
        cartResponse.setItems(Collections.emptyList()); // Keeping it simple
        Mockito.when(responseSpec.bodyToMono(CartResponseDTO.class)).thenReturn(Mono.just(cartResponse));

        // Mock the response from the Payment Service (which uses POST)
        PaymentResponseDTO paymentResponse = new PaymentResponseDTO();
        paymentResponse.setSuccess(true);
        // We need to tell the mock what to return when bodyToMono is called for PaymentResponseDTO
        Mockito.when(responseSpec.bodyToMono(PaymentResponseDTO.class)).thenReturn(Mono.just(paymentResponse));

        Mockito.when(orderRepository.save(Mockito.any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // --- 2. ACT (Call the method we are testing) ---
        Order result = orderService.createOrder(orderRequest);

        // --- 3. ASSERT (Check if the result is correct) ---
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1L, result.getUserId());

        Mockito.verify(orderRepository, Mockito.times(2)).save(Mockito.any(Order.class));
        Mockito.verify(rabbitTemplate, Mockito.times(1)).convertAndSend(Mockito.anyString(), Mockito.anyString(), Mockito.any(Object.class));
    }
}