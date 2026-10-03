package com.idlecard.game.player.playerDTO.response;

import java.util.List;

/** 프론트엔드가 매 액션마다 4번 나눠 호출하던 것을 한 번의 요청으로 합친 응답. */
public record GameStateResponse(
        PlayerStateResponse player,
        List<PlayerCardDto> cards,
        List<SlotDto> slots,
        SynergyResponse synergy
) {
}
