package com.github.payment.api.application.service;

import com.github.payment.api.application.dtos.request.LoginUserRequest;
import com.github.payment.api.application.dtos.response.LoginUserResponse;
import com.github.payment.api.application.exceptions.InvalidCredentialsException;
import com.github.payment.api.application.ports.in.UserLoginUseCase;
import com.github.payment.api.domain.model.User;
import com.github.payment.api.infrastructure.security.JwtService;
import com.github.payment.api.infrastructure.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthUserService implements UserLoginUseCase {

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    public LoginUserResponse login(LoginUserRequest userRequest) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                User.normalizeEmail(userRequest.email()),
                userRequest.password()
        );

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(authToken);
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken((UserDetailsImpl) userDetails);

        return new LoginUserResponse(token);
    }
}
