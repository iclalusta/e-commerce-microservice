package com.huseyinkaplan.payment_service.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class PaymentRequest {
    @NotNull
    private Long orderId;

    @NotNull
    @Min(1)
    private Double amount;

    @NotBlank
    private String cardHolder;

    @Pattern(regexp = "\\d{16}")
    private String cardNumber;

    @Pattern(regexp = "\\d{3}")
    private String cvv;

    public @NotNull Long getOrderId() {
        return orderId;
    }

    public void setOrderId(@NotNull Long orderId) {
        this.orderId = orderId;
    }

    public @NotNull @Min(1) Double getAmount() {
        return amount;
    }

    public void setAmount(@NotNull @Min(1) Double amount) {
        this.amount = amount;
    }

    public @NotBlank String getCardHolder() {
        return cardHolder;
    }

    public void setCardHolder(@NotBlank String cardHolder) {
        this.cardHolder = cardHolder;
    }

    public @Pattern(regexp = "\\d{16}") String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(@Pattern(regexp = "\\d{16}") String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public @Pattern(regexp = "\\d{3}") String getCvv() {
        return cvv;
    }

    public void setCvv(@Pattern(regexp = "\\d{3}") String cvv) {
        this.cvv = cvv;
    }
}
