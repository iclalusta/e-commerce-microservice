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
import com.huseyinkaplan.payment_service.dto.PaymentResponseDTO;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> makePayment(@RequestBody PaymentRequest request) {
        Payment payment = paymentService.processPayment(request);

        System.out.println("**************************");
        System.out.println("Payment: success");
        System.out.println("**************************");
        PaymentResponseDTO paymentdto = new PaymentResponseDTO(true);

        return ResponseEntity.ok(paymentdto);
    }
}
