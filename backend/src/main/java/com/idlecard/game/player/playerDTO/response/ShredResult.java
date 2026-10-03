package com.idlecard.game.player.playerDTO.response;

public record ShredResult(
        int shredded,
        int shardsGained,
        long totalShards,
        GameStateResponse state
) {
}
