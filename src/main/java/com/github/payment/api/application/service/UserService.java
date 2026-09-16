package com.github.payment.api.application.service;

import com.github.payment.api.application.dtos.request.RegisterUserRequest;
import com.github.payment.api.application.dtos.request.UpdateUserRequest;
import com.github.payment.api.application.dtos.response.RegisterUserResponse;
import com.github.payment.api.application.exceptions.EmailAlreadyRegisteredException;
import com.github.payment.api.application.exceptions.UserAccessDeniedException;
import com.github.payment.api.application.exceptions.UserNotFoundException;
import com.github.payment.api.application.mapper.UserMapper;
import com.github.payment.api.application.ports.in.CreateUserUseCase;
import com.github.payment.api.application.ports.in.DeactivateUserUseCase;
import com.github.payment.api.application.ports.in.FindUserByIdUseCase;
import com.github.payment.api.application.ports.in.UpdateUserUseCase;
import com.github.payment.api.application.ports.out.UserRepository;
import com.github.payment.api.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements CreateUserUseCase,
        FindUserByIdUseCase,
        UpdateUserUseCase,
        DeactivateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public RegisterUserResponse createUser(RegisterUserRequest userRequest) {
        String normalizedEmail = User.normalizeEmail(userRequest.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        String passwordHash = passwordEncoder.encode(userRequest.password());
        User savedUser = userRepository.save(User.create(userRequest.name(), normalizedEmail, passwordHash));

        return UserMapper.toResponse(savedUser);
    }

    @Override
    public RegisterUserResponse findById(UUID userId, UUID authenticatedUserId) {
        return UserMapper.toResponse(findOwnedUser(userId, authenticatedUserId));
    }

    @Override
    public RegisterUserResponse update(UUID userId, UUID authenticatedUserId, UpdateUserRequest request) {
        User user = findOwnedUser(userId, authenticatedUserId);
        String normalizedEmail = User.normalizeEmail(request.email());

        if (!user.getEmail().equals(normalizedEmail) && userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        String passwordHash = request.password() == null ? null : passwordEncoder.encode(request.password());
        user.updateProfile(request.name(), normalizedEmail, passwordHash);

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public RegisterUserResponse deactivate(UUID userId, UUID authenticatedUserId) {
        User user = findOwnedUser(userId, authenticatedUserId);
        user.deactivate();

        return UserMapper.toResponse(userRepository.save(user));
    }

    private User findOwnedUser(UUID userId, UUID authenticatedUserId) {
        if (!userId.equals(authenticatedUserId)) {
            throw new UserAccessDeniedException();
        }

        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }
}
