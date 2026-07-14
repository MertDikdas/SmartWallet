package com.smartwallet.authservice.controller;

import com.smartwallet.authservice.dto.response.UserResponse;
import com.smartwallet.authservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UserResponse response =
                userService.getCurrentUser(jwt.getSubject());

        return ResponseEntity.ok(response);
    }
}