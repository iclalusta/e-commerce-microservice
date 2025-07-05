package com.huseyinkaplan.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor // Add this annotation
@NoArgsConstructor  // Good practice to also have a no-args constructor
public class PaymentResponseDTO {
    private boolean success;
}