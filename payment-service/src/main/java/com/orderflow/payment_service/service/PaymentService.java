package com.orderflow.payment_service.service;

import com.orderflow.payment_service.dto.PaymentRequest;
import com.orderflow.payment_service.model.Payment;
import com.orderflow.payment_service.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    public Payment processPayment(PaymentRequest request, String idempotencyKey) {
        // Step 1: Idempotency Check — if key already processed, return previous record
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
            System.out.println(">>> [IDEMPOTENCY] Duplicate key detected: " + idempotencyKey + ". Returning existing payment.");
            return existingPayment.get();
        }

        // Step 2: Simulate Payment Processing
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setIdempotencyKey(idempotencyKey);
        payment.setStatus("SUCCESS");
        payment.setCreatedAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);
        System.out.println(">>> [PAYMENT PROCESSED] New payment created with ID: " + savedPayment.getId());
        return savedPayment;
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
    }
}