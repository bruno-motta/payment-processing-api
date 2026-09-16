package com.github.payment.api.infrastructure.persistence.mapper;

import com.github.payment.api.domain.model.IdempotencyKey;
import com.github.payment.api.infrastructure.persistence.entity.IdempotencyKeyEntity;
import com.github.payment.api.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyKeyEntityMapper {

    public IdempotencyKeyEntity toEntity(IdempotencyKey domain){
        if(domain == null){
            return null;
        }

        PaymentEntity paymentReference = new PaymentEntity();
        paymentReference.setId(domain.getPaymentId());

        return new IdempotencyKeyEntity(
                domain.getId(),
                domain.getKey(),
                paymentReference,
                domain.getResponseBody(),
                domain.getCreatedAt()
        );

    }


    public IdempotencyKey toDomain(IdempotencyKeyEntity entity){
        return IdempotencyKey.reconstitute(
            entity.getId(),
            entity.getKey(),
            entity.getPaymentId() != null ? entity.getPaymentId().getId() : null,
            entity.getResponseBody(),
            entity.getCreatedAt()
        );
    }
}
