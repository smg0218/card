package com.idlecard.game.player.playerDTO.response;

public record UpgradeResult(
        boolean success,
        PlayerCardDto playerCard,
        int consumedCopies,
        double successProbability,
        GameStateResponse state
) {
}
