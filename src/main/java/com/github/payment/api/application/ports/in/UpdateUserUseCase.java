package com.github.payment.api.application.ports.in;

import com.github.payment.api.application.dtos.request.UpdateUserRequest;
import com.github.payment.api.application.dtos.response.RegisterUserResponse;

import java.util.UUID;

public interface UpdateUserUseCase {

    RegisterUserResponse update(UUID userId, UUID authenticatedUserId, UpdateUserRequest request);
}
