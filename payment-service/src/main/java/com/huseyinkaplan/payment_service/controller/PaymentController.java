package com.huseyinkaplan.payment_service.controller;

import com.huseyinkaplan.payment_service.dto.PaymentRequest;
import com.huseyinkaplan.payment_service.entity.Payment;
import com.huseyinkaplan.payment_service.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Payment> makePayment(@RequestBody PaymentRequest request) {
        Payment payment = paymentService.processPayment(request);
        return ResponseEntity.ok(payment);
    }
}
