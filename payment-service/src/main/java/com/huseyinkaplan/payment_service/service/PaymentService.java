package com.huseyinkaplan.payment_service.service;

import com.huseyinkaplan.payment_service.dto.PaymentRequest;
import com.huseyinkaplan.payment_service.entity.Payment;
import com.huseyinkaplan.payment_service.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    public Payment processPayment(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setCreatedAt(LocalDateTime.now());

        payment.setStatus("COMPLETED");

        return paymentRepository.save(payment);
    }
}
