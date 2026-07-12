package com.smartwallet.authservice.service;


import com.smartwallet.authservice.config.JwtProperties;
import com.smartwallet.authservice.dto.request.LoginRequest;
import com.smartwallet.authservice.dto.request.RegisterRequest;
import com.smartwallet.authservice.dto.response.AuthResponse;
import com.smartwallet.authservice.dto.response.UserResponse;
import com.smartwallet.authservice.entity.Role;
import com.smartwallet.authservice.entity.User;
import com.smartwallet.authservice.exception.EmailAlreadyExistsException;
import com.smartwallet.authservice.mapper.UserMapper;
import com.smartwallet.authservice.repository.UserRepository;
import com.smartwallet.authservice.security.TokenService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

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

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizedEmail,
                        request.password()
                )
        );

        User user = userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow();

        String accessToken =
                tokenService.generateAccessToken(user);

        return new AuthResponse(
                accessToken,
                "Bearer",
                jwtProperties.accessTokenTtl().toSeconds(),
                userMapper.toUserResponse(user)
        );
    }

    private String normalizeEmail(String email) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
