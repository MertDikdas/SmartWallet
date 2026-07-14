package com.smartwallet.authservice.repository;

import com.smartwallet.authservice.entity.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}