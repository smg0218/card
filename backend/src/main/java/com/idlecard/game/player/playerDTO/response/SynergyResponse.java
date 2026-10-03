package com.idlecard.game.player.playerDTO.response;

import java.util.List;
import java.util.Map;

public record SynergyResponse(
        Map<String, Integer> tagCounts,
        Map<String, Double> tagBonusPercent,
        boolean gradeSynergyActive,
        double gradeSynergyBonusPercent,
        double totalProductionPerMinute,
        List<CardProductionDto> perCardProduction
) {
    public record CardProductionDto(
            Long playerCardId,
            String cardName,
            int slotIndex,
            double baseProductionPerMinute,
            double finalProductionPerMinute
    ) {
    }
}
