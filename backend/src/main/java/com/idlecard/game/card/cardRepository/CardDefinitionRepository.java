package com.idlecard.game.card.cardRepository;

import com.idlecard.game.card.cardEntity.CardDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardDefinitionRepository extends JpaRepository<CardDefinition, Long> {
    Optional<CardDefinition> findByCode(String code);
}
