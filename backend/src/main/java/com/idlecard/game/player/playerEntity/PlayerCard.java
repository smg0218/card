package com.idlecard.game.player.playerEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "player_card")
@Comment("플레이어가 보유한 카드 1건 (카드 정의 1종당 1행, 성급/복사본 수를 함께 관리).")
@Getter
@Setter
@NoArgsConstructor
public class PlayerCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK.")
    private Long id;

    @Column(nullable = false)
    @Comment("소유 플레이어 ID (FK 성격, player.id 참조).")
    private UUID playerId;

    /** 카드 정의는 정적 데이터라 인메모리 캐시(CardCatalogService)로 조회한다. */
    @Column(nullable = false)
    @Comment("카드 정의 ID (card_definition.id 참조). 정적 데이터라 캐시로 조회.")
    private Long cardDefinitionId;

    @Column(nullable = false)
    @Comment("강화 성급 (1~10). 성급이 높을수록 생산량 배율이 커짐.")
    private int starLevel = 1;

    /** 다음 강화에 소모할 수 있는, 아직 쓰지 않은 동일 카드 복사본 개수. */
    @Column(nullable = false)
    @Comment("다음 강화/분해에 쓸 수 있는, 아직 사용하지 않은 동일 카드 복사본 개수.")
    private int spareCopies = 0;

    @Column(nullable = false)
    @Comment("이 카드를 최초로 획득한 시각.")
    private Instant acquiredAt;
}
