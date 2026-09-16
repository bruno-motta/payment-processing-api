package com.github.payment.api.application.controllers;

import com.github.payment.api.application.dtos.request.LoginUserRequest;
import com.github.payment.api.application.dtos.response.LoginUserResponse;
import com.github.payment.api.application.dtos.response.RegisterUserResponse;
import com.github.payment.api.application.ports.in.FindUserByIdUseCase;
import com.github.payment.api.application.ports.in.UserLoginUseCase;
import com.github.payment.api.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserLoginUseCase userLoginUseCase;
    private final FindUserByIdUseCase findUserByIdUseCase;
    private final JwtService jwtService;

    @PostMapping
    public ResponseEntity<LoginUserResponse> login(@RequestBody @Valid LoginUserRequest request) {
        return ResponseEntity.ok(userLoginUseCase.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<RegisterUserResponse> me(@RequestHeader("Authorization") String authHeader) {
        UUID userId = UUID.fromString(jwtService.extractUserId(authHeader.replace("Bearer ", "")));
        return ResponseEntity.ok(findUserByIdUseCase.findById(userId, userId));
    }
}
