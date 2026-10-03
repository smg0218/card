package com.idlecard.game.card.cardDTO.response;

import com.idlecard.game.card.cardEntity.CardDefinition;
import com.idlecard.game.card.cardEntity.CardGrade;
import com.idlecard.game.player.playerService.CardShardTable;

import java.util.List;

public record CardDefinitionDto(
        Long id,
        String code,
        String name,
        CardGrade grade,
        List<String> tags,
        double baseProductionPerMinute,
        String description,
        boolean excludeFromGradeSynergy,
        boolean excludeFromTagSynergy,
        String buffTargetTag,
        double buffBonusPercent,
        int shredYield,
        int craftShardCost
) {
    public static CardDefinitionDto from(CardDefinition c) {
        return new CardDefinitionDto(
                c.getId(), c.getCode(), c.getName(), c.getGrade(), c.getTags(),
                c.getBaseProductionPerMinute(), c.getDescription(),
                c.isExcludeFromGradeSynergy(), c.isExcludeFromTagSynergy(),
                c.getBuffTargetTag(), c.getBuffBonusPercent(),
                CardShardTable.shredYield(c.getGrade()), CardShardTable.craftCost(c.getGrade())
        );
    }
}
