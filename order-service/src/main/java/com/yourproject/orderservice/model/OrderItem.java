package com.yourproject.orderservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private Integer quantity;

    // It's very important to store the price at the time of order
    // in case the product's price changes later.
    private BigDecimal priceAtTimeOfOrder;

    // This sets up the many-to-one relationship. Many OrderItems belong to one Order.
    // The @JoinColumn specifies the foreign key column in the "order_items" table.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
}