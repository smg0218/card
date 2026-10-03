package com.idlecard.game.player.playerService;

import java.util.Map;

/**
 * 카드 강화 규칙:
 * - 최대 10성까지 강화 가능
 * - 소모하는 카드 복사본은 항상 1개로 고정한다. 대신 성급이 오를수록 골드 비용이 커진다.
 * - 확정 강화는 2성(1->2)뿐이며, 이후 단계로 갈수록 성공 확률이 낮아진다.
 * - 8성 구간(7->8)부터는 확률이 급격히 낮아진다.
 * - 강화 실패 시에도 소모한 복사본과 골드는 반환되지 않는다 (하이리스크 하이리턴).
 */
public final class UpgradeTable {

    public static final int MAX_STAR_LEVEL = 10;

    /** 모든 성급 강화에 공통으로 소모되는 카드 복사본 개수. */
    public static final int REQUIRED_COPIES = 1;

    public record Step(int targetLevel, double goldCost, double successProbability) {
    }

    // key: 도달하려는 목표 성급 (2~10)
    private static final Map<Integer, Step> STEPS = Map.ofEntries(
            Map.entry(2, new Step(2, 50, 1.00)),
            Map.entry(3, new Step(3, 120, 0.90)),
            Map.entry(4, new Step(4, 300, 0.75)),
            Map.entry(5, new Step(5, 700, 0.60)),
            Map.entry(6, new Step(6, 1500, 0.45)),
            Map.entry(7, new Step(7, 3000, 0.30)),
            Map.entry(8, new Step(8, 6000, 0.12)),
            Map.entry(9, new Step(9, 12000, 0.06)),
            Map.entry(10, new Step(10, 25000, 0.03))
    );

    // 강화 레벨별 생산량 배율 (1성 기준 상대값, 예시 수치이며 밸런스 테스트로 조정)
    private static final Map<Integer, Double> STAR_MULTIPLIER = Map.ofEntries(
            Map.entry(1, 1.0),
            Map.entry(2, 2.4),
            Map.entry(3, 6.0),
            Map.entry(4, 16.0),
            Map.entry(5, 50.0),
            Map.entry(6, 120.0),
            Map.entry(7, 280.0),
            Map.entry(8, 650.0),
            Map.entry(9, 1500.0),
            Map.entry(10, 3500.0)
    );

    private UpgradeTable() {
    }

    /** 목표 레벨(현재 레벨 + 1)에 대한 강화 정보. 이미 최대 레벨이면 null. */
    public static Step stepFor(int targetLevel) {
        return STEPS.get(targetLevel);
    }

    public static double multiplierFor(int starLevel) {
        return STAR_MULTIPLIER.getOrDefault(starLevel, 1.0);
    }
}
