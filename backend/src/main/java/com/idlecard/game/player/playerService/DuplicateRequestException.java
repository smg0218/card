package com.idlecard.game.player.playerService;

/** 같은 사용자가 같은 요청을 아직 처리 중인데 또 보냈을 때 던진다. */
public class DuplicateRequestException extends RuntimeException {
    public DuplicateRequestException(String message) {
        super(message);
    }
}
