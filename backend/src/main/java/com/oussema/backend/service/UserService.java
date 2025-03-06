package com.oussema.backend.service;


import com.oussema.backend.entity.User;
import com.oussema.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;


    public User getUserById(UUID id) {
        return this.userRepository.findById(id).orElseThrow(
                () -> new RuntimeException("User not found")
        );
    }
    
}
