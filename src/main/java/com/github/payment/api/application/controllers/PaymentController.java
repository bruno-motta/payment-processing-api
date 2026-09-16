package com.github.payment.api.application.controllers;

import com.github.payment.api.application.dtos.request.CreatePaymentRequest;
import com.github.payment.api.application.dtos.response.PaymentCreateResponse;
import com.github.payment.api.application.ports.in.CreatePaymentUseCase;
import com.github.payment.api.application.ports.in.FindPaymentByIdUseCase;
import com.github.payment.api.application.ports.in.RefundPaymentUseCase;
import com.github.payment.api.application.ports.in.RetryPaymentUseCase;
import com.github.payment.api.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final CreatePaymentUseCase paymentUseCase;
    private final FindPaymentByIdUseCase findPaymentByIdUseCase;
    private final RefundPaymentUseCase refundPaymentUseCase;
    private final RetryPaymentUseCase retryPaymentUseCase;
    private final JwtService jwtService;

    @PostMapping
    public ResponseEntity<PaymentCreateResponse> createPayment(
            @RequestBody @Valid CreatePaymentRequest request,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestHeader("Authorization") String authHeader){

        String token = authHeader.replace("Bearer ", "");

        UUID userId = UUID.fromString(jwtService.extractUserId(token));

        log.info("Criando pagamento para usuário: {}", userId);

        PaymentCreateResponse response = paymentUseCase.createPayment(request, userId, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentCreateResponse> findPaymentById(
            @PathVariable UUID paymentId,
            @RequestHeader("Authorization") String authHeader) {
        PaymentCreateResponse response = findPaymentByIdUseCase.findPaymentById(paymentId, extractUserId(authHeader));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentCreateResponse> refund(
            @PathVariable UUID paymentId,
            @RequestHeader("Authorization") String authHeader) {
        PaymentCreateResponse response = refundPaymentUseCase.refund(paymentId, extractUserId(authHeader));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/retry")
    public ResponseEntity<PaymentCreateResponse> retry(
            @PathVariable UUID paymentId,
            @RequestHeader("Authorization") String authHeader) {
        PaymentCreateResponse response = retryPaymentUseCase.retry(paymentId, extractUserId(authHeader));
        return ResponseEntity.ok(response);
    }

    // Garante que apenas o dono do pagamento consiga consultá-lo ou alterá-lo.
    private UUID extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return UUID.fromString(jwtService.extractUserId(token));
    }
}
