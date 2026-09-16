package com.github.payment.api.application.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 200, message = "Máximo de 200 caracteres permitido.")
        String name,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email
        @Size(max = 200, message = "Máximo de 200 caracteres permitido.")
        String email,

        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres.")
        String password
) {
}
