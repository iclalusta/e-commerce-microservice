package com.huseyinkaplan.payment_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class PaymentRequest {

    @NotNull
    private Long orderId;

    @NotNull
    @Min(1)
    private BigDecimal amount;

    public @NotNull Long getOrderId() {
        return orderId;
    }

    public void setOrderId(@NotNull Long orderId) {
        this.orderId = orderId;
    }

    public @NotNull @Min(1) BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(@NotNull @Min(1) BigDecimal amount) {
        this.amount = amount;
    }
}