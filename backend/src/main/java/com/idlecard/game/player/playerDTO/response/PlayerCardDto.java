package com.idlecard.game.player.playerDTO.response;

import com.idlecard.game.card.cardDTO.response.CardDefinitionDto;
import com.idlecard.game.card.cardEntity.CardDefinition;
import com.idlecard.game.player.playerEntity.PlayerCard;
import com.idlecard.game.player.playerService.UpgradeTable;

public record PlayerCardDto(
        Long id,
        CardDefinitionDto cardDefinition,
        int starLevel,
        int spareCopies,
        Integer placedSlotIndex,
        Integer nextUpgradeCopyCost,
        Double nextUpgradeGoldCost,
        Double nextUpgradeSuccessProbability,
        boolean maxLevel
) {
    public static PlayerCardDto from(PlayerCard pc, CardDefinition def, Integer placedSlotIndex) {
        UpgradeTable.Step next = UpgradeTable.stepFor(pc.getStarLevel() + 1);
        return new PlayerCardDto(
                pc.getId(),
                CardDefinitionDto.from(def),
                pc.getStarLevel(),
                pc.getSpareCopies(),
                placedSlotIndex,
                next == null ? null : UpgradeTable.REQUIRED_COPIES,
                next == null ? null : next.goldCost(),
                next == null ? null : next.successProbability(),
                next == null
        );
    }
}
