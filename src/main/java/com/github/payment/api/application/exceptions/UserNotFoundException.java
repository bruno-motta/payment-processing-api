package com.github.payment.api.application.exceptions;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("Usuário não encontrado.");
    }
}
