package com.smartwallet.authservice.service;


import com.smartwallet.authservice.dto.request.RegisterRequest;
import com.smartwallet.authservice.dto.response.UserResponse;
import com.smartwallet.authservice.entity.Role;
import com.smartwallet.authservice.entity.User;
import com.smartwallet.authservice.exception.EmailAlreadyExistsException;
import com.smartwallet.authservice.mapper.UserMapper;
import com.smartwallet.authservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import jdk.jshell.spi.ExecutionControl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if(userRepository.existsByEmailIgnoreCase(normalizedEmail)){
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        User user = User.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        return userMapper.toUserResponse(savedUser);
    }

    private String normalizeEmail(String email) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
