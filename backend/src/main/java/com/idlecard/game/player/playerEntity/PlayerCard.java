package com.idlecard.game.player.playerEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "player_card")
@Getter
@Setter
@NoArgsConstructor
public class PlayerCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID playerId;

    /** 카드 정의는 정적 데이터라 인메모리 캐시(CardCatalogService)로 조회한다. */
    @Column(nullable = false)
    private Long cardDefinitionId;

    @Column(nullable = false)
    private int starLevel = 1;

    /** 다음 강화에 소모할 수 있는, 아직 쓰지 않은 동일 카드 복사본 개수. */
    @Column(nullable = false)
    private int spareCopies = 0;

    @Column(nullable = false)
    private Instant acquiredAt;
}
