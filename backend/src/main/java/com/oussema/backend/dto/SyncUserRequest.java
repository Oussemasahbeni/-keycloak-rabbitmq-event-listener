package com.oussema.backend.dto;

import java.util.UUID;

public record SyncUserRequest(
         UUID id,
            String email,
            String username,
            String firstName,
            String lastName,
            boolean enabled,
            boolean emailVerified
) {
}
