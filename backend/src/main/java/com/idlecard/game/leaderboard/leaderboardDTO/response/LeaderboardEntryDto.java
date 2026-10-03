package com.idlecard.game.leaderboard.leaderboardDTO.response;

public record LeaderboardEntryDto(
        int rank,
        String nickname,
        double totalGoldEarned,
        boolean me
) {
}
