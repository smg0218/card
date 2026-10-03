package com.idlecard.game.player.playerRepository;

import com.idlecard.game.player.playerEntity.PlayerCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerCardRepository extends JpaRepository<PlayerCard, Long> {
    List<PlayerCard> findByPlayerId(UUID playerId);

    Optional<PlayerCard> findByPlayerIdAndCardDefinitionId(UUID playerId, Long cardDefinitionId);
}
