package com.idlecard.game.player.playerService;

import com.idlecard.game.card.cardEntity.CardGrade;

import java.util.Map;

/**
 * 카드 분해(갈기) / 조각 교환 규칙.
 * - 필요 없는 카드를 분해하면 등급에 비례한 카드 조각을 얻는다.
 * - 모은 조각으로 원하는 카드를 지정해서 직접 획득(제작)할 수 있다. 등급이 높을수록 필요 조각이 많다.
 */
public final class CardShardTable {

    private static final Map<CardGrade, Integer> SHRED_YIELD = Map.of(
            CardGrade.NORMAL, 3,
            CardGrade.RARE, 8,
            CardGrade.UNIQUE, 20,
            CardGrade.LEGENDARY, 50
    );

    private static final Map<CardGrade, Integer> CRAFT_COST = Map.of(
            CardGrade.NORMAL, 15,
            CardGrade.RARE, 50,
            CardGrade.UNIQUE, 150,
            CardGrade.LEGENDARY, 400
    );

    private CardShardTable() {
    }

    public static int shredYield(CardGrade grade) {
        return SHRED_YIELD.getOrDefault(grade, 1);
    }

    public static int craftCost(CardGrade grade) {
        return CRAFT_COST.getOrDefault(grade, Integer.MAX_VALUE);
    }
}
