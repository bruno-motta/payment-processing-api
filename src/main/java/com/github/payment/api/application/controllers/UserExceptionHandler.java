package com.github.payment.api.application.controllers;

import com.github.payment.api.application.exceptions.EmailAlreadyRegisteredException;
import com.github.payment.api.application.exceptions.InvalidCredentialsException;
import com.github.payment.api.application.exceptions.UserAccessDeniedException;
import com.github.payment.api.application.exceptions.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {UserController.class, AuthController.class})
public class UserExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<Map<String, String>> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler({InvalidCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "E-mail ou senha inválidos."));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(UserAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleUserAccessDenied(UserAccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
    }
}
