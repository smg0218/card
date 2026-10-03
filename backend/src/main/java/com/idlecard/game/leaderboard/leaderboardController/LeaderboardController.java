package com.idlecard.game.leaderboard.leaderboardController;

import com.idlecard.game.leaderboard.leaderboardDTO.response.LeaderboardEntryDto;
import com.idlecard.game.leaderboard.leaderboardService.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    public List<LeaderboardEntryDto> top(@RequestParam(required = false) UUID viewerPlayerId) {
        return leaderboardService.topPlayers(viewerPlayerId);
    }
}
