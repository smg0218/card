package com.idlecard.game.player.playerDTO.response;

import com.idlecard.game.player.playerEntity.EventType;

public record ClaimEventResult(
        EventType type,
        String rewardDescription,
        double goldGained,
        int shardsGained,
        GameStateResponse state
) {
}
