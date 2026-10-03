package com.idlecard.game.user.userDTO.request;

import jakarta.validation.constraints.NotBlank;

public record WithdrawRequest(
        @NotBlank String id,
        @NotBlank String password
) {
}
