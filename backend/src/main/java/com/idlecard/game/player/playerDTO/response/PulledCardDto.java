package com.idlecard.game.player.playerDTO.response;

import com.idlecard.game.card.cardEntity.CardGrade;

public record PulledCardDto(
        Long cardDefinitionId,
        String name,
        CardGrade grade,
        boolean firstTimeObtained
) {
}
