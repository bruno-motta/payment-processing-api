package com.github.payment.api.application.exceptions;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException() {
        super("E-mail já cadastrado.");
    }
}
