package com.idlecard.game.player.playerRepository;

import com.idlecard.game.player.playerEntity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    @Query("select s from Slot s left join fetch s.playerCard where s.playerId = :playerId order by s.slotIndex asc")
    List<Slot> findByPlayerIdOrderBySlotIndexAsc(@Param("playerId") UUID playerId);

    Optional<Slot> findByPlayerIdAndSlotIndex(UUID playerId, int slotIndex);

    Optional<Slot> findByPlayerIdAndPlayerCard_Id(UUID playerId, Long playerCardId);
}
