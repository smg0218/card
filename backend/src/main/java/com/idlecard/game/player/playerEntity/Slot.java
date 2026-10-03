package com.idlecard.game.player.playerEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.UUID;

@Entity
@Table(name = "slot", uniqueConstraints = @UniqueConstraint(columnNames = {"playerId", "slotIndex"}))
@Comment("플레이어의 카드 배치 슬롯. 슬롯에 배치된 카드만 생산량에 기여함.")
@Getter
@Setter
@NoArgsConstructor
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK.")
    private Long id;

    @Column(nullable = false)
    @Comment("소유 플레이어 ID (FK 성격, player.id 참조).")
    private UUID playerId;

    @Column(nullable = false)
    @Comment("슬롯 번호 (0부터 시작). player_id + slot_index는 유니크.")
    private int slotIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_card_id")
    @Comment("이 슬롯에 배치된 카드 (FK, player_card.id). null이면 빈 슬롯.")
    private PlayerCard playerCard;

    public Slot(UUID playerId, int slotIndex) {
        this.playerId = playerId;
        this.slotIndex = slotIndex;
    }
}
