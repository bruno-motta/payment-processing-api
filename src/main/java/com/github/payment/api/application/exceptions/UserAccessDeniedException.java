package com.github.payment.api.application.exceptions;

public class UserAccessDeniedException extends RuntimeException {

    public UserAccessDeniedException() {
        super("Você não tem permissão para acessar este usuário.");
    }
}
