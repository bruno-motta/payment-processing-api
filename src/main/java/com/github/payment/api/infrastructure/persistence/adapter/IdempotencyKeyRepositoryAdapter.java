package com.github.payment.api.infrastructure.persistence.adapter;

import com.github.payment.api.application.ports.out.IdempotencyKeyRepository;
import com.github.payment.api.domain.model.IdempotencyKey;
import com.github.payment.api.infrastructure.persistence.entity.IdempotencyKeyEntity;
import com.github.payment.api.infrastructure.persistence.mapper.IdempotencyKeyEntityMapper;
import com.github.payment.api.infrastructure.persistence.repositories.IdempotencyKeyEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IdempotencyKeyRepositoryAdapter implements IdempotencyKeyRepository {

   private final IdempotencyKeyEntityMapper idempotencyKeyEntityMapper;
   private final IdempotencyKeyEntityRepository idempotencyKeyRepository;

    @Override
    public IdempotencyKey save(IdempotencyKey key) {
        IdempotencyKeyEntity entity = idempotencyKeyEntityMapper.toEntity(key);
        IdempotencyKeyEntity savedEntity = idempotencyKeyRepository.save(entity);
        return idempotencyKeyEntityMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<IdempotencyKey> findByKey(UUID key) {
        return idempotencyKeyRepository.findByKey(key)
                .map(idempotencyKeyEntityMapper::toDomain);
    }
}
