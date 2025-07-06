package com.yourproject.orderservice.dto;

import com.yourproject.orderservice.model.OrderStatus;

// Bu DTO, sipariş güncelleme istekleri için kullanılacak.
public class OrderUpdateDTO {

    private String shippingAddress;
    private OrderStatus status;

    // Getters
    public String getShippingAddress() {
        return shippingAddress;
    }

    public OrderStatus getStatus() {
        return status;
    }

    // Setters
    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}