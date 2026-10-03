package com.idlecard.game.user.userDTO.response;

import java.time.Instant;

public record UserInfoResponse(
        String id,
        String nickname,
        Instant createdAt,
        double gold,
        double totalGoldEarned,
        int rank,
        long totalPlayers
) {
}
