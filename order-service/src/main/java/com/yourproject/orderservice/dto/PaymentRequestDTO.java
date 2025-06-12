// File: src/main/java/com/yourproject/orderservice/dto/PaymentRequestDTO.java
package com.yourproject.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor // Lombok annotation for a constructor with all fields
public class PaymentRequestDTO {
    private Long orderId; // Or some other reference
    private BigDecimal amount;
    // In a real app, you'd have card details, etc.
}