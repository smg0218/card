package com.idlecard.game.player.playerDTO.response;

import java.util.UUID;

public record PlayerStateResponse(
        UUID playerId,
        double gold,
        double totalGoldEarned,
        long cardShards,
        int slotCount,
        double nextSlotUnlockCost,
        boolean slotsMaxed,
        double productionPerMinute,
        ActiveEventDto activeEvent
) {
}
