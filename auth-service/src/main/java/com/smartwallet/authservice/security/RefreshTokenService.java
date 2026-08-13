package com.smartwallet.authservice.security;

import com.smartwallet.authservice.config.RefreshTokenProperties;
import com.smartwallet.authservice.entity.RefreshToken;
import com.smartwallet.authservice.entity.User;
import com.smartwallet.authservice.exception.InvalidRefreshTokenException;
import com.smartwallet.authservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTE_LENGTH = 48;

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties refreshTokenProperties;

    private final SecureRandom secureRandom = new SecureRandom();

    public IssuedRefreshToken issue(User user) {
        String rawToken = generateRawToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(hash(rawToken))
                .user(user)
                .expiresAt(
                        Instant.now().plus(
                                refreshTokenProperties.ttl()
                        )
                )
                .build();

        refreshTokenRepository.save(refreshToken);

        return new IssuedRefreshToken(user, rawToken);
    }


    public IssuedRefreshToken rotate(String rawToken) {
        RefreshToken currentToken = findValidToken(rawToken);

        User user = currentToken.getUser();

        currentToken.revoke();

        return issue(user);
    }

    public void revoke(String rawToken) {
        RefreshToken refreshToken = findValidToken(rawToken);
        refreshToken.revoke();
    }

    private RefreshToken findValidToken(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            throw new InvalidRefreshTokenException();
        }

        return refreshToken;
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    exception
            );
        }
    }

    public record IssuedRefreshToken(
            User user,
            String value
    ) {
    }
}