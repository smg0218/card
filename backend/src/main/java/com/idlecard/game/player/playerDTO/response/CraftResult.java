package com.idlecard.game.player.playerDTO.response;

public record CraftResult(
        PlayerCardDto playerCard,
        int shardsSpent,
        long remainingShards,
        GameStateResponse state
) {
}
