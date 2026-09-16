package com.github.payment.api.infrastructure.gateway;

import com.github.payment.api.application.ports.out.PaymentGatewayResult;
import com.github.payment.api.application.ports.out.PaymentGatewayPort;
import com.github.payment.api.domain.model.Payment;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class StripeGatewayAdapter implements PaymentGatewayPort {

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    @PostConstruct
    private void init(){
        Stripe.apiKey = stripeApiKey;
    }


    @Override
    public PaymentGatewayResult charge(Payment payment, String idempotencyKey) {
        try {
            //Conversão de BigDecimal para centavos como pede a doc do Stripe (ex: 100.50 -> 10050)
            long amountInCents = payment.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(payment.getCurrency().toLowerCase())
                    .setDescription(payment.getDescription())
                    .setConfirm(true) //confirma e tenta cobrar novamente.
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    );
            // Se o pagamento já tiver o ID do metodo (ex: pm_card_visa), vincula
            if(payment.getPaymentMethodId() != null && !payment.getPaymentMethodId().isBlank()){
                paramsBuilder.setPaymentMethod(payment.getPaymentMethodId());
            }

            PaymentIntentCreateParams params = paramsBuilder.build();

            //Configuração de Idempotência do lado da Stripe -> Proteção de Ponta a Ponta.
            RequestOptions requestOptions = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            log.info("Enviando cobrança para a stripe com chave de Idempotência: {} ",  idempotencyKey);
            PaymentIntent intent = PaymentIntent.create(params, requestOptions);

            // verifica o status retornado pela Stripe
            boolean isSucess = "succeeded".equalsIgnoreCase(intent.getStatus());
            log.info("Stripe respondeu para intent {}: status={}", intent.getId(), intent.getStatus() );

            return new PaymentGatewayResult(intent.getId(), isSucess);

        } catch (StripeException e) {
            log.error("Erro ao processar pagamento na Stripe: {}", e.getMessage(), e);
            // Retorna falha mantendo o chargeId como null ou código do erro
            return new PaymentGatewayResult(null , false);
        }
    }

    @Override
    public PaymentGatewayResult refund(String gatewayTransactionId) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(gatewayTransactionId)
                    .build();

            Refund refund = Refund.create(params);
            boolean success = "succeeded".equalsIgnoreCase(refund.getStatus());

            log.info("Stripe respondeu ao reembolso {}: status={}", refund.getId(), refund.getStatus());
            return new PaymentGatewayResult(refund.getId(), success);
        } catch (StripeException e) {
            log.error("Erro ao reembolsar transação {} na Stripe: {}", gatewayTransactionId, e.getMessage(), e);
            return new PaymentGatewayResult(null, false);
        }
    }


}
