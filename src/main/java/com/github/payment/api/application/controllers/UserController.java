package com.github.payment.api.application.controllers;

import com.github.payment.api.application.dtos.request.RegisterUserRequest;
import com.github.payment.api.application.dtos.request.UpdateUserRequest;
import com.github.payment.api.application.dtos.response.RegisterUserResponse;
import com.github.payment.api.application.ports.in.CreateUserUseCase;
import com.github.payment.api.application.ports.in.DeactivateUserUseCase;
import com.github.payment.api.application.ports.in.FindUserByIdUseCase;
import com.github.payment.api.application.ports.in.UpdateUserUseCase;
import com.github.payment.api.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final FindUserByIdUseCase findUserByIdUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeactivateUserUseCase deactivateUserUseCase;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> createUser(@RequestBody @Valid RegisterUserRequest registerUserRequest) {
        RegisterUserResponse response = createUserUseCase.createUser(registerUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<RegisterUserResponse> findById(
            @PathVariable UUID userId,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(findUserByIdUseCase.findById(userId, extractUserId(authHeader)));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<RegisterUserResponse> update(
            @PathVariable UUID userId,
            @RequestBody @Valid UpdateUserRequest request,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(updateUserUseCase.update(userId, extractUserId(authHeader), request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<RegisterUserResponse> deactivate(
            @PathVariable UUID userId,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(deactivateUserUseCase.deactivate(userId, extractUserId(authHeader)));
    }

    private UUID extractUserId(String authHeader) {
        return UUID.fromString(jwtService.extractUserId(authHeader.replace("Bearer ", "")));
    }
}
