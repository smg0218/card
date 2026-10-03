package com.idlecard.game.player.playerEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "player")
@Comment("플레이어의 게임 진행 데이터 (골드/슬롯/이벤트 등). 계정(app_user)과 1:1로 연결됨.")
@Getter
@Setter
@NoArgsConstructor
public class Player {

    public static final int STARTING_SLOT_COUNT = 3;

    @Id
    @Comment("PK (UUID). app_user.player_id가 이 값을 참조.")
    private UUID id;

    @Column(nullable = false)
    @Comment("현재 보유 골드 (소비 가능).")
    private double gold;

    @Column(nullable = false)
    @Comment("현재 해금된 슬롯 수 (표시용 캐시. 실제 기준은 slot 테이블의 행 수).")
    private int slotCount;

    /** 카드를 분해해서 얻는 조각. 모아서 원하는 카드로 교환(제작)할 수 있다. */
    @Column(nullable = false)
    @ColumnDefault("0")
    @Comment("보유 카드 조각 수. 카드 분해로 얻고, 카드 제작에 소모.")
    private long cardShards = 0;

    /** 소비 여부와 상관없이 지금까지 생산으로 벌어들인 골드 누계 (랭킹 산정 기준). 절대 감소하지 않는다. */
    @Column(nullable = false)
    @ColumnDefault("0")
    @Comment("지금까지 생산으로 벌어들인 골드 누계. 절대 감소하지 않으며 랭킹 산정 기준.")
    private double totalGoldEarned = 0;

    @Column(nullable = false)
    @Comment("마지막으로 골드를 정산한 시각 (오프라인 보상 계산 기준점).")
    private Instant lastTickAt;

    @Column(nullable = false)
    @Comment("플레이어 생성 시각.")
    private Instant createdAt;

    /** 다음 이벤트 판정 시각. null이면 아직 초기화 전이라는 뜻이며, 다음 tick에서 자동으로 채워진다. */
    @Comment("다음 랜덤 이벤트 판정 시각. null이면 아직 초기화 전.")
    private Instant nextEventRollAt;

    @Enumerated(EnumType.STRING)
    @Comment("현재 활성화된 이벤트 종류 (GOLD_RUSH/TREASURE_CHEST/SHARD_CACHE). null이면 이벤트 없음.")
    private EventType activeEventType;

    @Comment("현재 활성화된 이벤트의 종료 시각.")
    private Instant activeEventEndsAt;

    @Column(nullable = false)
    @ColumnDefault("false")
    @Comment("현재 이벤트 보상을 이미 수령했는지 여부 (클릭형 이벤트에만 의미 있음).")
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
