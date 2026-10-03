package com.idlecard.game.player.playerEntity;

public enum EventType {
    /** 1분간 생산량 부스트 (자동 적용, 클릭 불필요) */
    GOLD_RUSH,
    /** 클릭해서 골드 보상을 받는 이벤트 */
    TREASURE_CHEST,
    /** 클릭해서 카드 조각 보상을 받는 이벤트 */
    SHARD_CACHE
}
