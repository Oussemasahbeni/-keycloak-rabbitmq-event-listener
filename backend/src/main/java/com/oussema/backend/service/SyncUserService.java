package com.oussema.backend.service;

import com.oussema.backend.dto.SyncUserRequest;
import com.oussema.backend.entity.User;
import com.oussema.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import static com.oussema.backend.config.RabbitmqConfig.SYNC_QUEUE;

@Service
@RequiredArgsConstructor
@Log4j2
public class SyncUserService {

    private final UserRepository userRepository;

    // This method listens to the SYNC_QUEUE and saves the user to the database if it doesn't already exist
    @RabbitListener(queues = SYNC_QUEUE)
    public void receiveMessage(SyncUserRequest message) {
        if (userRepository.existsById(message.id())) {
            log.info("User with id {} already exists", message.id());
            return;
        }

        User user = User.builder()
                .id(message.id())
                .email(message.email())
                .username(message.username())
                .firstName(message.firstName())
                .lastName(message.lastName())
                .enabled(message.enabled())
                .emailVerified(message.emailVerified())
                .build();
        userRepository.save(user);
        log.info("User with id {} saved", message.id());
    }
}
