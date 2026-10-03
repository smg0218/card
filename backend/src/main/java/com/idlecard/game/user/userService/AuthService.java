package com.idlecard.game.user.userService;

import com.idlecard.game.user.userEntity.AppUser;
import com.idlecard.game.player.playerEntity.Player;
import com.idlecard.game.user.userDTO.response.AuthResponse;
import com.idlecard.game.user.userDTO.response.UserInfoResponse;
import com.idlecard.game.user.userRepository.AppUserRepository;
import com.idlecard.game.player.playerRepository.PlayerCardRepository;
import com.idlecard.game.player.playerRepository.PlayerRepository;
import com.idlecard.game.player.playerRepository.SlotRepository;
import com.idlecard.game.player.playerService.DuplicateRequestGuard;
import com.idlecard.game.player.playerService.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 개인정보 없이 ID/PW/닉네임만으로 동작하는 최소 계정 시스템.
 * 게임 데이터(Player/PlayerCard/Slot)는 그대로 두고, 계정은 그 데이터의 소유권만 부여한다.
 *
 * 참고: 토큰은 서버 메모리에만 저장되는 단순한 세션 토큰이며, 게임 API 자체(/api/players/**)는
 * 여전히 playerId만으로 호출 가능하다 (이번 요청 범위는 로그인/가입/탈퇴 흐름 구현까지).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PlayerRepository playerRepository;
    private final PlayerCardRepository playerCardRepository;
    private final SlotRepository slotRepository;
    private final GameService gameService;
    private final DuplicateRequestGuard duplicateRequestGuard;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final Map<String, String> tokenToUserId = new ConcurrentHashMap<>();

    public boolean isIdAvailable(String id) {
        return !appUserRepository.existsById(id);
    }

    public boolean isNicknameAvailable(String nickname) {
        return !appUserRepository.existsByNickname(nickname);
    }

    @Transactional
    public AuthResponse register(String id, String password, String nickname) {
        return duplicateRequestGuard.run("register:" + id, () -> doRegister(id, password, nickname));
    }

    private AuthResponse doRegister(String id, String password, String nickname) {
        if (appUserRepository.existsById(id)) {
            throw new IllegalStateException("이미 사용 중인 아이디입니다.");
        }
        if (appUserRepository.existsByNickname(nickname)) {
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        }

        Player player = gameService.insertPlayer();

        AppUser user = new AppUser();
        user.setId(id);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setPlayerId(player.getId());
        user.setCreatedAt(Instant.now());
        appUserRepository.save(user);

        String token = issueToken(id);
        return new AuthResponse(id, nickname, player.getId(), token);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(String id, String password) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("아이디 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalStateException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        String token = issueToken(id);
        return new AuthResponse(user.getId(), user.getNickname(), user.getPlayerId(), token);
    }

    @Transactional(readOnly = true)
    public UserInfoResponse findUserInfo(String id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 계정입니다."));
        Player player = playerRepository.findById(user.getPlayerId())
                .orElseThrow(() -> new IllegalStateException("플레이어를 찾을 수 없습니다."));

        long ahead = playerRepository.countByTotalGoldEarnedGreaterThan(player.getTotalGoldEarned());
        long totalPlayers = playerRepository.count();

        return new UserInfoResponse(
                user.getId(), user.getNickname(), user.getCreatedAt(),
                player.getGold(), player.getTotalGoldEarned(), (int) ahead + 1, totalPlayers
        );
    }

    @Transactional
    public void changePassword(String id, String oldPassword, String newPassword) {
        duplicateRequestGuard.run("changePassword:" + id, () -> doChangePassword(id, oldPassword, newPassword));
    }

    private void doChangePassword(String id, String oldPassword, String newPassword) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("아이디 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalStateException("현재 비밀번호가 올바르지 않습니다.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        appUserRepository.save(user);
    }

    /** 탈퇴: 비밀번호 재확인 후 계정과 게임 데이터(카드/슬롯/플레이어)를 모두 삭제한다. */
    @Transactional
    public void withdraw(String id, String password) {
        duplicateRequestGuard.run("withdraw:" + id, () -> doWithdraw(id, password));
    }

    private void doWithdraw(String id, String password) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("아이디 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalStateException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        UUID playerId = user.getPlayerId();
        // slot이 player_card를 FK로 참조하므로, 슬롯에 카드가 배치된 상태라면 반드시 슬롯을 먼저 지워야 한다.
        slotRepository.deleteAll(slotRepository.findByPlayerIdOrderBySlotIndexAsc(playerId));
        playerCardRepository.deleteAll(playerCardRepository.findByPlayerId(playerId));
        playerRepository.deleteById(playerId);
        appUserRepository.deleteById(id);

        tokenToUserId.values().removeIf(uid -> uid.equals(id));
    }

    private String issueToken(String userId) {
        String token = UUID.randomUUID().toString();
        tokenToUserId.put(token, userId);
        return token;
    }
}
