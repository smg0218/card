package com.idlecard.game.leaderboard.leaderboardService;

import com.idlecard.game.leaderboard.leaderboardDTO.response.LeaderboardEntryDto;
import com.idlecard.game.player.playerEntity.Player;
import com.idlecard.game.player.playerRepository.PlayerRepository;
import com.idlecard.game.user.userEntity.AppUser;
import com.idlecard.game.user.userRepository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** 지금까지 벌어들인 누적 골드(totalGoldEarned) 기준 랭킹. */
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final PlayerRepository playerRepository;
    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> topPlayers(UUID viewerPlayerId) {
        List<Player> top = playerRepository.findTop50ByOrderByTotalGoldEarnedDesc();
        List<UUID> ids = top.stream().map(Player::getId).toList();
        Map<UUID, String> nicknameByPlayerId = appUserRepository.findByPlayerIdIn(ids).stream()
                .collect(Collectors.toMap(AppUser::getPlayerId, AppUser::getNickname));

        List<LeaderboardEntryDto> result = new java.util.ArrayList<>();
        int rank = 1;
        for (Player p : top) {
            String nickname = nicknameByPlayerId.getOrDefault(p.getId(), "(알 수 없음)");
            result.add(new LeaderboardEntryDto(rank, nickname, p.getTotalGoldEarned(), p.getId().equals(viewerPlayerId)));
            rank++;
        }
        return result;
    }
}
