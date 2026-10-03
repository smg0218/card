package com.idlecard.game.player.playerDTO.response;

import java.util.List;
import java.util.Map;

public record DrawResult(
        List<PulledCardDto> pulls,
        Map<String, Integer> gradeCounts,
        double goldSpent,
        GameStateResponse state
) {
}
