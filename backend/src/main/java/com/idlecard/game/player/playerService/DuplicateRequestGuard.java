package com.idlecard.game.player.playerService;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 서버가 하나뿐인 환경(멀티 서버/Redis 불필요)을 전제로 한, JVM 인메모리 중복요청 방지 락이다.
 * 큐잉하지 않고, 같은 키로 처리 중인 요청이 있으면 새 요청은 즉시 거절한다(409).
 *
 * 키는 "유저(플레이어) 식별자 + 요청 내용" 형식으로 만든다. 예: "{playerId}:unlockSlot",
 * "{playerId}:upgrade:{playerCardId}". 이렇게 하면 같은 사용자라도 서로 다른 대상(예: 카드 A
 * 강화와 카드 B 강화)은 동시에 처리할 수 있고, 완전히 같은 요청(예: 슬롯 확장 두 번 연타)만
 * 걸러낸다.
 */
@Component
public class DuplicateRequestGuard {

    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    public <T> T run(String key, Supplier<T> action) {
        if (!inFlight.add(key)) {
            throw new DuplicateRequestException("이미 처리 중인 요청입니다. 잠시 후 다시 시도해주세요.");
        }
        try {
            return action.get();
        } finally {
            inFlight.remove(key);
        }
    }

    public void run(String key, Runnable action) {
        run(key, () -> {
            action.run();
            return null;
        });
    }
}
