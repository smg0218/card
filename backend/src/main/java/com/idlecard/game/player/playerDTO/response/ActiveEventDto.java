package com.idlecard.game.player.playerDTO.response;

import com.idlecard.game.player.playerEntity.EventType;

public record ActiveEventDto(
        EventType type,
        String name,
        String description,
        long remainingSeconds,
        boolean claimable,
        boolean claimed
) {
}
