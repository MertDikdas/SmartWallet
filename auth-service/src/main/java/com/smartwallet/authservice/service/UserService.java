package com.smartwallet.authservice.service;

import com.smartwallet.authservice.dto.response.UserResponse;
import com.smartwallet.authservice.entity.User;
import com.smartwallet.authservice.exception.UserNotFoundException;
import com.smartwallet.authservice.mapper.UserMapper;
import com.smartwallet.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String subject) {
        Long userId = parseUserId(subject);
        User user = userRepository.findById(userId).orElseThrow(()-> new UserNotFoundException(userId));
        return userMapper.toUserResponse(user);
    }
    private Long parseUserId(String subject) {
        try {
            return Long.parseLong(subject);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("JWT subject does not contain a valid user ID");
        }
    }
}
