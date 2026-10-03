package com.idlecard.game.user.userRepository;

import com.idlecard.game.user.userEntity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, String> {
    boolean existsByNickname(String nickname);

    List<AppUser> findByPlayerIdIn(Collection<UUID> playerIds);

    Optional<AppUser> findByPlayerId(UUID playerId);
}
