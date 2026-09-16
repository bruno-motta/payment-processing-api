package com.github.payment.api.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.payment.api.application.dtos.request.CreatePaymentRequest;
import com.github.payment.api.application.dtos.response.PaymentCreateResponse;
import com.github.payment.api.application.mapper.PaymentMapper;
import com.github.payment.api.application.ports.in.CreatePaymentUseCase;
import com.github.payment.api.application.ports.in.FindPaymentByIdUseCase;
import com.github.payment.api.application.ports.in.RefundPaymentUseCase;
import com.github.payment.api.application.ports.in.RetryPaymentUseCase;
import com.github.payment.api.application.ports.out.IdempotencyKeyRepository;
import com.github.payment.api.application.ports.out.PaymentGatewayPort;
import com.github.payment.api.application.ports.out.PaymentGatewayResult;
import com.github.payment.api.application.ports.out.PaymentRepository;
import com.github.payment.api.domain.enuns.StatusPayment;
import com.github.payment.api.domain.model.IdempotencyKey;
import com.github.payment.api.domain.model.Payment;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentService implements CreatePaymentUseCase,
                                       FindPaymentByIdUseCase,
                                       RefundPaymentUseCase,
                                       RetryPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final PaymentGatewayPort paymentGatewayPort;
    private final ObjectMapper objectMapper;

    @Transactional
    @Override
    public PaymentCreateResponse createPayment(CreatePaymentRequest request, UUID userId, UUID idempotencyKey) {


        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findByKey(idempotencyKey);
        if (existingKey.isPresent()) {
            log.info("Idempotency key já utilizada: {}", idempotencyKey);
            return deserializer(existingKey.get().getResponseBody());
        }

        // Criação do pagamento no ESTADO PENDING, usando o metodo create, criado dentro do domain.
        Payment payment = Payment.create(
                request.amount(),
                request.currency(),
                request.description(),
                request.paymentMethod(),
                request.paymentMethodId(),
                userId

        );
        // Transaciona para PROCESSING, de acordo com a máquina de estado.
        payment.processing();
        payment = paymentRepository.save(payment);
        log.info("Payment criado e em processamento: {}", payment.getId());


        PaymentGatewayResult result = paymentGatewayPort.charge(payment, idempotencyKey.toString());

        //  // Transaciona para APROVED ou FAILED conforme resultado.
        if (result.success()) {
            payment.approve(result.chargeId());
            log.info("Payment aprovado: {} | chargeId: {}", payment.getId(), result.chargeId());
        } else {
            payment.fail();
            log.warn("Payment falhou: {}", payment.getId());
        }

        Payment savedPayment = paymentRepository.save(payment);

        // 5. Registro de Idempotência e Retorno
        PaymentCreateResponse response = PaymentMapper.toResponse(savedPayment);

        IdempotencyKey key = IdempotencyKey.create(
                idempotencyKey,
                savedPayment.getId(),
                serializer(response)
        );
        idempotencyKeyRepository.save(key);

        return response;
    }

    @Override
    public PaymentCreateResponse findPaymentById(UUID paymentId, UUID userId) {
        return paymentRepository.findByIdAndUserId(paymentId, userId)
                .map(PaymentMapper::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Pagamento não encontrado: " + paymentId));
    }

    @Transactional
    @Override
    public PaymentCreateResponse refund(UUID paymentId, UUID userId) {
        Payment payment = findPayment(paymentId, userId);

        if (payment.getStatus() != StatusPayment.APPROVED) {
            throw new IllegalArgumentException("Somente pagamentos aprovados podem ser reembolsados.");
        }

        if (payment.getGatewayTransactionId() == null || payment.getGatewayTransactionId().isBlank()) {
            throw new IllegalStateException("Pagamento aprovado sem identificador de transação no gateway.");
        }

        PaymentGatewayResult result = paymentGatewayPort.refund(payment.getGatewayTransactionId());
        if (!result.success()) {
            throw new IllegalStateException("Não foi possível realizar o reembolso no gateway.");
        }

        payment.refund();
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Pagamento reembolsado: {} | refundId: {}", savedPayment.getId(), result.chargeId());

        return PaymentMapper.toResponse(savedPayment);
    }

    @Transactional
    @Override
    public PaymentCreateResponse retry(UUID paymentId, UUID userId) {
        Payment payment = findPayment(paymentId, userId);

        if (!payment.isRetryable()) {
            throw new IllegalArgumentException("Pagamento não pode ser retentado. Status: "
                    + payment.getStatus() + ", tentativas: " + payment.getRetry());
        }

        payment.retry();
        PaymentGatewayResult result = paymentGatewayPort.charge(payment, UUID.randomUUID().toString());

        if (result.success()) {
            payment.approve(result.chargeId());
            log.info("Pagamento aprovado na retentativa: {} | chargeId: {}", payment.getId(), result.chargeId());
        } else {
            payment.fail();
            log.warn("Retentativa do pagamento falhou: {}", payment.getId());
        }

        Payment savedPayment = paymentRepository.save(payment);
        return PaymentMapper.toResponse(savedPayment);
    }

    private Payment findPayment(UUID paymentId, UUID userId) {
        return paymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Pagamento não encontrado: " + paymentId));
    }

    // OBJETO JAVA -> JSON TEXT
    private String serializer(PaymentCreateResponse response){
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar resposta de idempotencia0", e);
        }
    }

    //JSON -> OBJETO JAVA
    private PaymentCreateResponse deserializer(String json){
        try {
            return objectMapper.readValue(json, PaymentCreateResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
