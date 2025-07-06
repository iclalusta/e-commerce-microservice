package com.yourproject.orderservice.controller;

import com.yourproject.orderservice.dto.OrderRequestDTO;
import com.yourproject.orderservice.model.Order;
import com.yourproject.orderservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.yourproject.orderservice.dto.OrderUpdateDTO;


import java.util.List;

@RestController // Tells Spring this class will handle REST API requests
@RequestMapping("/api/orders") // All endpoints in this class will start with /api/orders
public class OrderController {

    // @Autowired tells Spring to inject the OrderService bean here.
    // We will create the OrderService class in the next step.
    @Autowired
    private OrderService orderService;

    /**
     * Endpoint to create a new order.
     * It expects order data in the request's body.
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestHeader("X-User-Id") Long userId,
                                             @RequestBody OrderRequestDTO orderRequest) {
        // We will pass the request data to the service layer to handle the business logic.
        // The service will return the newly created and saved order.
        Order newOrder = orderService.createOrder(orderRequest,userId);
        // We return the new order and an HTTP status of 201 CREATED.
        return new ResponseEntity<>(newOrder, HttpStatus.CREATED);
    }

    /**
     * Endpoint to get a specific order by its ID.
     * The {orderId} part is a path variable.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long orderId) {
        // We ask the service to find the order. The service might return null if not found.
        return orderService.findOrderById(orderId)
                .map(order -> ResponseEntity.ok(order)) // If found, return it with 200 OK
                .orElse(ResponseEntity.notFound().build()); // If not found, return 404 NOT FOUND
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long orderId, @RequestBody OrderUpdateDTO orderDetails) {
        try {
            Order updatedOrder = orderService.updateOrder(orderId, orderDetails);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            // Sipariş bulunamazsa 404 Not Found döndürür.
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Endpoint to get all orders for a specific user.
     * Example URL: /api/orders?userId=123
     */
    @GetMapping
    public ResponseEntity<List<Order>> getOrdersByUserId(@RequestHeader("X-User-Id") Long userId) {
        // We use the custom method we defined in our OrderRepository (via the service).
        List<Order> orders = orderService.findOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> allOrders = orderService.findAllOrders();
        return ResponseEntity.ok(allOrders);
    }
}