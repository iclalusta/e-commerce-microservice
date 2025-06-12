package com.yourproject.orderservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders") // Good practice to name the table explicitly
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments the ID
    private Long id;

    private Long userId; // The ID of the user who placed the order

    @CreationTimestamp // Automatically set the time when the order is created
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING) // Stores the enum value as a String (e.g., "PENDING")
    private OrderStatus status;

    private String shippingAddress;

    private BigDecimal totalAmount;

    // This sets up the one-to-many relationship. One Order has many OrderItems.
    // CascadeType.ALL means if we save an Order, its items are also saved.
    // orphanRemoval=true means if an item is removed from this list, it's deleted from the DB.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    // A helper method to easily add items to the order and keep both sides of the relationship in sync
    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
    }
}