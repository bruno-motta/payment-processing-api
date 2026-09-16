package com.github.payment.api.application.ports.in;

import com.github.payment.api.application.dtos.response.RegisterUserResponse;

import java.util.UUID;

public interface FindUserByIdUseCase {

    RegisterUserResponse findById(UUID userId, UUID authenticatedUserId);
}
