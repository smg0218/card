package com.idlecard.game.player.playerService;

import com.idlecard.game.player.playerEntity.EventType;

import java.time.Duration;
import java.util.Map;

/**
 * 개인별 랜덤 이벤트 규칙.
 * - 지속시간 1분, 내부쿨타임(=다음 판정까지 간격) 5분.
 * - 쿨타임이 지나면 5분마다 10% 확률로 새 이벤트가 발생한다.
 * - 이벤트를 놓쳐도 불이익은 없다 (그냥 보너스를 못 받을 뿐).
 */
public final class EventTable {

    public static final Duration EVENT_DURATION = Duration.ofMinutes(1);
    public static final Duration ROLL_INTERVAL = Duration.ofMinutes(5);
    public static final double TRIGGER_PROBABILITY = 0.10;

    /** 오프라인으로 오래 비웠을 때 무한정 판정을 몰아서 하지 않도록 하는 안전장치. */
    public static final int MAX_CATCHUP_ROLLS = 20;

    public static final double GOLD_RUSH_MULTIPLIER = 1.5;
    public static final double TREASURE_CHEST_GOLD_REWARD = 300;
    public static final int SHARD_CACHE_REWARD = 10;

    private static final EventType[] ROLL_POOL = {
            EventType.GOLD_RUSH, EventType.TREASURE_CHEST, EventType.SHARD_CACHE
    };

    private static final Map<EventType, String> NAMES = Map.of(
            EventType.GOLD_RUSH, "황금 시간",
            EventType.TREASURE_CHEST, "보물상자",
            EventType.SHARD_CACHE, "조각 더미"
    );

    private static final Map<EventType, String> DESCRIPTIONS = Map.of(
            EventType.GOLD_RUSH, "1분간 생산량이 +50% 증가합니다 (자동 적용)",
            EventType.TREASURE_CHEST, "클릭해서 골드 " + (int) TREASURE_CHEST_GOLD_REWARD + "을 받으세요",
            EventType.SHARD_CACHE, "클릭해서 카드 조각 " + SHARD_CACHE_REWARD + "개를 받으세요"
    );

    private EventTable() {
    }

    public static EventType[] rollPool() {
        return ROLL_POOL;
    }

    public static boolean isClaimable(EventType type) {
        return type == EventType.TREASURE_CHEST || type == EventType.SHARD_CACHE;
    }

    public static String nameOf(EventType type) {
        return NAMES.getOrDefault(type, type.name());
    }

    public static String descriptionOf(EventType type) {
        return DESCRIPTIONS.getOrDefault(type, "");
    }
}
