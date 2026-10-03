package com.idlecard.game.player.playerController;

import com.idlecard.game.player.playerDTO.request.*;
import com.idlecard.game.player.playerDTO.response.*;
import com.idlecard.game.player.playerService.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final GameService gameService;

    @GetMapping("/{playerId}")
    public PlayerStateResponse getState(@PathVariable UUID playerId) {
        return gameService.getState(playerId);
    }

    @GetMapping("/{playerId}/game-state")
    public GameStateResponse getFullState(@PathVariable UUID playerId) {
        return gameService.getFullState(playerId);
    }

    @PostMapping("/{playerId}/draw")
    public DrawResult draw(@PathVariable UUID playerId,
                            @RequestParam(defaultValue = "1") int count,
                            @RequestParam(defaultValue = "false") boolean premium) {
        return gameService.draw(playerId, count, premium);
    }

    @PostMapping("/{playerId}/cards/{playerCardId}/upgrade")
    public UpgradeResult upgrade(@PathVariable UUID playerId, @PathVariable Long playerCardId) {
        return gameService.upgrade(playerId, playerCardId);
    }

    @PostMapping("/{playerId}/cards/{playerCardId}/shred")
    public ShredResult shred(@PathVariable UUID playerId, @PathVariable Long playerCardId,
                              @RequestParam(defaultValue = "1") int count) {
        return gameService.shredCard(playerId, playerCardId, count);
    }

    @PostMapping("/{playerId}/craft")
    public CraftResult craft(@PathVariable UUID playerId, @Valid @RequestBody CraftRequest request) {
        return gameService.craftCard(playerId, request.cardDefinitionId());
    }

    @PostMapping("/{playerId}/slots/{slotIndex}/place")
    public GameStateResponse place(@PathVariable UUID playerId, @PathVariable int slotIndex,
                                    @Valid @RequestBody PlaceCardRequest request) {
        return gameService.placeCard(playerId, slotIndex, request.playerCardId());
    }

    @PostMapping("/{playerId}/slots/{slotIndex}/clear")
    public GameStateResponse clear(@PathVariable UUID playerId, @PathVariable int slotIndex) {
        return gameService.clearSlot(playerId, slotIndex);
    }

    @PostMapping("/{playerId}/slots/unlock")
    public GameStateResponse unlockSlot(@PathVariable UUID playerId) {
        return gameService.unlockSlot(playerId);
    }

    @PostMapping("/{playerId}/event/claim")
    public ClaimEventResult claimEvent(@PathVariable UUID playerId) {
        return gameService.claimEvent(playerId);
    }
}
