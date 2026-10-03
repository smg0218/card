package com.idlecard.game.player.playerEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "slot", uniqueConstraints = @UniqueConstraint(columnNames = {"playerId", "slotIndex"}))
@Getter
@Setter
@NoArgsConstructor
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID playerId;

    @Column(nullable = false)
    private int slotIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_card_id")
    private PlayerCard playerCard;

    public Slot(UUID playerId, int slotIndex) {
        this.playerId = playerId;
        this.slotIndex = slotIndex;
    }
}
