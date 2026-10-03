package com.idlecard.game.player.playerDTO.request;

import jakarta.validation.constraints.NotNull;

public record CraftRequest(
        @NotNull Long cardDefinitionId
) {
}
