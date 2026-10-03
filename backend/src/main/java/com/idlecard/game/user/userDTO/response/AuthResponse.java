package com.idlecard.game.user.userDTO.response;

import java.util.UUID;

public record AuthResponse(
        String id,
        String nickname,
        UUID playerId,
        String token
) {
}
