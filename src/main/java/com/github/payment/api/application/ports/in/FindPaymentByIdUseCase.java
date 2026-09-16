package com.github.payment.api.application.ports.in;

import com.github.payment.api.application.dtos.response.PaymentCreateResponse;

import java.util.UUID;

public interface FindPaymentByIdUseCase {

    PaymentCreateResponse findPaymentById(UUID paymentId, UUID userId);
}
