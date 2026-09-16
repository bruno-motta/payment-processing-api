package com.github.payment.api.application.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
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
    public PaymentCreateResponse findPaymentById(UUID paymentId) {
        return null;
    }

    @Override
    public PaymentCreateResponse refund(UUID paymentId) {
        return null;
    }

    @Override
    public PaymentCreateResponse retry(UUID paymentId) {
        return null;
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
