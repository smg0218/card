package com.idlecard.game.player.playerRepository;

import com.idlecard.game.player.playerEntity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlayerRepository extends JpaRepository<Player, UUID> {
    List<Player> findTop50ByOrderByTotalGoldEarnedDesc();

    long countByTotalGoldEarnedGreaterThan(double totalGoldEarned);
}
