package com.idlecard.game.player.playerEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "player")
@Getter
@Setter
@NoArgsConstructor
public class Player {

    public static final int STARTING_SLOT_COUNT = 3;

    @Id
    private UUID id;

    @Column(nullable = false)
    private double gold;

    @Column(nullable = false)
    private int slotCount;

    /** 카드를 분해해서 얻는 조각. 모아서 원하는 카드로 교환(제작)할 수 있다. */
    @Column(nullable = false)
    @ColumnDefault("0")
    private long cardShards = 0;

    /** 소비 여부와 상관없이 지금까지 생산으로 벌어들인 골드 누계 (랭킹 산정 기준). 절대 감소하지 않는다. */
    @Column(nullable = false)
    @ColumnDefault("0")
    private double totalGoldEarned = 0;

    @Column(nullable = false)
    private Instant lastTickAt;

    @Column(nullable = false)
    private Instant createdAt;

    /** 다음 이벤트 판정 시각. null이면 아직 초기화 전이라는 뜻이며, 다음 tick에서 자동으로 채워진다. */
    private Instant nextEventRollAt;

    @Enumerated(EnumType.STRING)
    private EventType activeEventType;

    private Instant activeEventEndsAt;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean activeEventClaimed = false;

    public static Player createNew() {
        Player p = new Player();
        p.setId(UUID.randomUUID());
        p.setGold(0);
        p.setSlotCount(STARTING_SLOT_COUNT);
        p.setLastTickAt(Instant.now());
        p.setCreatedAt(Instant.now());
        return p;
    }
}
