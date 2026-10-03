package com.idlecard.game.user.userDTO.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String id,
        @NotBlank String password
) {
}
