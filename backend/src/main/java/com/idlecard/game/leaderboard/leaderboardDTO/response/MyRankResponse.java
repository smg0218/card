package com.idlecard.game.leaderboard.leaderboardDTO.response;

public record MyRankResponse(
        int rank,
        double totalGoldEarned,
        long totalPlayers
) {
}
