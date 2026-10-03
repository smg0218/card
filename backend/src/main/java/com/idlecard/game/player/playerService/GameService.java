package com.idlecard.game.player.playerService;

import com.idlecard.game.card.cardEntity.*;
import com.idlecard.game.card.cardService.CardCatalogService;
import com.idlecard.game.player.playerDTO.response.*;
import com.idlecard.game.player.playerEntity.*;
import com.idlecard.game.player.playerRepository.PlayerCardRepository;
import com.idlecard.game.player.playerRepository.PlayerRepository;
import com.idlecard.game.player.playerRepository.SlotRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 원격 DB(Neon, ap-southeast-1)는 지역 특성상 쿼리 1건마다 네트워크 왕복 지연이 있다.
 * 그래서 이 서비스는 요청 하나당 (1) 같은 데이터를 두 번 조회하지 않고, (2) 액션 응답에
 * 최신 전체 상태를 함께 실어 보내 프론트엔드가 액션 후 별도로 재조회하지 않도록 한다.
 */
@Service
@RequiredArgsConstructor
public class GameService {

    /**
     * 슬롯은 게임에서 가장 중요한 자원이라 갈수록 비용이 지수적으로 폭증하도록 한다.
     * cost(n) = BASE * GROWTH^(n - 시작슬롯수). 시작 3개 이후 4~10번째 슬롯까지만 확장 가능.
     */
    private static final double SLOT_UNLOCK_BASE_COST = 1000;
    private static final double SLOT_UNLOCK_GROWTH = 3.0;
    private static final int MAX_SLOT_COUNT = 10;

    private static final double DRAW_COST = 100;
    private static final double PREMIUM_DRAW_COST = 1000;
    private static final int MAX_DRAW_COUNT = 100;

    /**
     * 서버는 별도의 "접속 상태"를 저장하지 않으므로, 직전 정산 이후 경과 시간의 크기로
     * 온라인/오프라인을 구분한다: 프론트엔드가 4초 간격으로 폴링하므로, 그보다 훨씬 긴
     * 간격이 있었다면 그 사이 접속이 끊겨 있었던(=로그아웃 등) 것으로 본다.
     */
    private static final long ONLINE_TICK_GAP_SECONDS = 30;
    private static final double OFFLINE_PRODUCTION_RATE = 0.8;
    private static final double OFFLINE_MAX_MINUTES = 12 * 60;

    private static final Map<CardGrade, Double> GRADE_RATE = Map.of(
            CardGrade.NORMAL, 0.70,
            CardGrade.RARE, 0.20,
            CardGrade.UNIQUE, 0.08,
            CardGrade.LEGENDARY, 0.02
    );

    /** 고급 뽑기: 노멀을 제외하고 레어/유니크/전설의 기존 비율(20:8:2)을 유지한 채 재분배한다. */
    private static final Map<CardGrade, Double> PREMIUM_GRADE_RATE = Map.of(
            CardGrade.RARE, 20.0 / 30.0,
            CardGrade.UNIQUE, 8.0 / 30.0,
            CardGrade.LEGENDARY, 2.0 / 30.0
    );

    private final PlayerRepository playerRepository;
    private final PlayerCardRepository playerCardRepository;
    private final SlotRepository slotRepository;
    private final CardCatalogService cardCatalogService;
    private final SynergyService synergyService;
    private final DuplicateRequestGuard duplicateRequestGuard;
    private final Random random = new Random();

    private record Ticked(Player player, List<Slot> slots) {
    }

    private static final int STARTER_CARD_COUNT = 3;

    @Transactional
    public Player createPlayer() {
        Player player = Player.createNew();
        playerRepository.save(player);
        for (int i = 0; i < player.getSlotCount(); i++) {
            slotRepository.save(new Slot(player.getId(), i));
        }

        // 모두에게 동일한 출발선을 주기 위해 골드 없이, 노멀 등급 카드 3장만 고정으로 지급한다.
        List<CardDefinition> starters = cardCatalogService.byGrade(CardGrade.NORMAL).stream()
                .sorted(java.util.Comparator.comparing(CardDefinition::getId))
                .limit(STARTER_CARD_COUNT)
                .toList();
        for (CardDefinition def : starters) {
            PlayerCard card = new PlayerCard();
            card.setPlayerId(player.getId());
            card.setCardDefinitionId(def.getId());
            card.setStarLevel(1);
            card.setSpareCopies(0);
            card.setAcquiredAt(Instant.now());
            playerCardRepository.save(card);
        }

        return player;
    }

    @Transactional
    public PlayerStateResponse getState(UUID playerId) {
        Ticked t = tick(playerId);
        double production = synergyService.totalProductionPerMinute(t.slots())
                * eventProductionMultiplier(t.player(), Instant.now());
        return toPlayerState(t.player(), production);
    }

    /** 한 번의 호출로 상태/보유카드/슬롯/시너지를 모두 반환한다 (프론트 왕복 횟수 절감). */
    @Transactional
    public GameStateResponse getFullState(UUID playerId) {
        Ticked t = tick(playerId);
        return buildState(t.player(), t.slots());
    }

    /** 경과 시간만큼 골드를 정산하고 lastTickAt을 갱신한다 (오프라인 보상 포함). slots는 재사용을 위해 함께 반환. */
    private Ticked tick(UUID playerId) {
        Player player = getPlayerOrThrow(playerId);
        List<Slot> slots = slotRepository.findByPlayerIdOrderBySlotIndexAsc(playerId);
        Instant now = Instant.now();
        Duration elapsed = Duration.between(player.getLastTickAt(), now);
        double elapsedMinutes = elapsed.toMillis() / 60000.0;

        processEvents(player, now);

        if (elapsedMinutes > 0) {
            double productionPerMinute = synergyService.totalProductionPerMinute(slots)
                    * eventProductionMultiplier(player, now);

            boolean wasOnline = elapsed.getSeconds() <= ONLINE_TICK_GAP_SECONDS;
            double effectiveMinutes = wasOnline ? elapsedMinutes : Math.min(elapsedMinutes, OFFLINE_MAX_MINUTES);
            double rate = wasOnline ? 1.0 : OFFLINE_PRODUCTION_RATE;

            double earned = productionPerMinute * effectiveMinutes * rate;
            player.setGold(player.getGold() + earned);
            player.setTotalGoldEarned(player.getTotalGoldEarned() + earned);
            player.setLastTickAt(now);
        }

        // 과거 동시 요청 등으로 player.slotCount가 실제 슬롯 개수와 어긋난 경우 자동으로 맞춘다.
        if (player.getSlotCount() != slots.size()) {
            player.setSlotCount(slots.size());
        }

        return new Ticked(player, slots);
    }

    /**
     * 개인별 랜덤 이벤트 판정. 지속시간 1분, 5분마다 10% 확률로 새로 발생한다.
     * 오프라인으로 오래 비웠던 경우에도 무한정 몰아서 판정하지 않도록 MAX_CATCHUP_ROLLS로 제한한다
     * (놓친 이벤트는 그냥 못 받는 것으로 처리 — 불이익은 없다).
     */
    private void processEvents(Player player, Instant now) {
        if (player.getActiveEventType() != null && player.getActiveEventEndsAt() != null
                && !now.isBefore(player.getActiveEventEndsAt())) {
            player.setActiveEventType(null);
            player.setActiveEventEndsAt(null);
            player.setActiveEventClaimed(false);
        }

        if (player.getNextEventRollAt() == null) {
            player.setNextEventRollAt(now.plus(EventTable.ROLL_INTERVAL));
            return;
        }

        if (player.getActiveEventType() == null) {
            int rolls = 0;
            while (player.getActiveEventType() == null
                    && !now.isBefore(player.getNextEventRollAt())
                    && rolls < EventTable.MAX_CATCHUP_ROLLS) {
                Instant rollAt = player.getNextEventRollAt();
                if (random.nextDouble() < EventTable.TRIGGER_PROBABILITY) {
                    EventType[] pool = EventTable.rollPool();
                    EventType chosen = pool[random.nextInt(pool.length)];
                    Instant endsAt = rollAt.plus(EventTable.EVENT_DURATION);
                    if (endsAt.isAfter(now)) {
                        // 지금도 살아있는 이벤트만 실제로 띄운다. 이미 지나간 판정은 놓친 것으로 처리.
                        player.setActiveEventType(chosen);
                        player.setActiveEventEndsAt(endsAt);
                        player.setActiveEventClaimed(false);
                    }
                }
                player.setNextEventRollAt(rollAt.plus(EventTable.ROLL_INTERVAL));
                rolls++;
            }
        }
    }

    private double eventProductionMultiplier(Player player, Instant now) {
        if (player.getActiveEventType() == EventType.GOLD_RUSH
                && player.getActiveEventEndsAt() != null && now.isBefore(player.getActiveEventEndsAt())) {
            return EventTable.GOLD_RUSH_MULTIPLIER;
        }
        return 1.0;
    }

    private ActiveEventDto buildActiveEventDto(Player player, Instant now) {
        if (player.getActiveEventType() == null || player.getActiveEventEndsAt() == null) {
            return null;
        }
        // 만료 여부는 eventProductionMultiplier()와 동일한 기준(now.isBefore(endsAt))으로 판단한다.
        // Duration.getSeconds()는 0.x초 남은 경우 0으로 내림되어 "만료됨"으로 잘못 취급될 수 있어
        // 그 값을 만료 판정에 쓰지 않는다 — 표시용 초는 올림 처리해서 최소 1초로 보여준다.
        if (!now.isBefore(player.getActiveEventEndsAt())) {
            return null;
        }
        Duration remainingDuration = Duration.between(now, player.getActiveEventEndsAt());
        long remainingSeconds = Math.max(1, (remainingDuration.toMillis() + 999) / 1000);
        EventType type = player.getActiveEventType();
        return new ActiveEventDto(type, EventTable.nameOf(type), EventTable.descriptionOf(type),
                remainingSeconds, EventTable.isClaimable(type), player.isActiveEventClaimed());
    }

    private SynergyResponse applyEventBoost(SynergyResponse synergy, double multiplier) {
        if (multiplier == 1.0) {
            return synergy;
        }
        List<SynergyResponse.CardProductionDto> boosted = synergy.perCardProduction().stream()
                .map(p -> new SynergyResponse.CardProductionDto(
                        p.playerCardId(), p.cardName(), p.slotIndex(),
                        p.baseProductionPerMinute(), p.finalProductionPerMinute() * multiplier))
                .toList();
        return new SynergyResponse(
                synergy.tagCounts(), synergy.tagBonusPercent(), synergy.gradeSynergyActive(),
                synergy.gradeSynergyBonusPercent(), synergy.totalProductionPerMinute() * multiplier, boosted
        );
    }

    /** 이미 조회된 player/slots로 전체 상태 응답을 구성한다 (추가 slots 조회 없음). */
    private GameStateResponse buildState(Player player, List<Slot> slots) {
        List<PlayerCard> owned = playerCardRepository.findByPlayerId(player.getId());
        double multiplier = eventProductionMultiplier(player, Instant.now());
        SynergyResponse synergy = applyEventBoost(synergyService.compute(slots), multiplier);

        List<SlotDto> slotDtos = slots.stream().map(this::toSlotDto).toList();
        List<PlayerCardDto> cardDtos = owned.stream()
                .map(pc -> PlayerCardDto.from(pc, cardCatalogService.getOrThrow(pc.getCardDefinitionId()),
                        findSlotIndex(slots, pc.getId())))
                .toList();

        return new GameStateResponse(
                toPlayerState(player, synergy.totalProductionPerMinute()),
                cardDtos, slotDtos, synergy
        );
    }

    /**
     * 카드 뽑기. count(최대 MAX_DRAW_COUNT)만큼 한 번에 뽑을 수 있다.
     * 서버 부하 방지를 위해 뽑기 결과는 메모리에서 카드 종류별로 집계한 뒤,
     * 실제 DB 쓰기는 이번 뽑기에서 실제로 등장한 카드 종류 수만큼만 수행한다
     * (예: 100연차를 뽑아도 카드 도감이 10종이면 최대 10건만 저장).
     */
    @Transactional
    public DrawResult draw(UUID playerId, int count, boolean premium) {
        return duplicateRequestGuard.run(playerId + ":draw", () -> doDraw(playerId, count, premium));
    }

    private DrawResult doDraw(UUID playerId, int count, boolean premium) {
        if (count < 1 || count > MAX_DRAW_COUNT) {
            throw new IllegalArgumentException("뽑기 개수는 1~" + MAX_DRAW_COUNT + " 사이여야 합니다.");
        }
        Ticked t = tick(playerId);
        Player player = t.player();

        double costPerDraw = premium ? PREMIUM_DRAW_COST : DRAW_COST;
        double totalCost = costPerDraw * count;
        if (player.getGold() < totalCost) {
            throw new IllegalStateException("골드가 부족합니다. (필요: " + totalCost + ")");
        }
        player.setGold(player.getGold() - totalCost);

        Map<Long, PlayerCard> ownedByDefId = playerCardRepository.findByPlayerId(playerId).stream()
                .collect(java.util.stream.Collectors.toMap(PlayerCard::getCardDefinitionId, pc -> pc));
        java.util.Set<Long> ownedBeforeIds = new java.util.HashSet<>(ownedByDefId.keySet());
        java.util.Set<Long> seenInBatch = new java.util.HashSet<>();
        Map<Long, Integer> tally = new java.util.LinkedHashMap<>();
        List<PulledCardDto> pulls = new java.util.ArrayList<>(count);
        Map<String, Integer> gradeCounts = new java.util.LinkedHashMap<>();

        for (int i = 0; i < count; i++) {
            CardDefinition drawn = premium ? rollPremiumCard() : rollCard();
            tally.merge(drawn.getId(), 1, Integer::sum);
            boolean firstTime = !ownedBeforeIds.contains(drawn.getId()) && seenInBatch.add(drawn.getId());
            pulls.add(new PulledCardDto(drawn.getId(), drawn.getName(), drawn.getGrade(), firstTime));
            gradeCounts.merge(drawn.getGrade().name(), 1, Integer::sum);
        }

        Instant now = Instant.now();
        for (var entry : tally.entrySet()) {
            Long cardDefId = entry.getKey();
            int n = entry.getValue();
            PlayerCard existing = ownedByDefId.get(cardDefId);
            if (existing != null) {
                existing.setSpareCopies(existing.getSpareCopies() + n);
                playerCardRepository.save(existing);
            } else {
                PlayerCard card = new PlayerCard();
                card.setPlayerId(playerId);
                card.setCardDefinitionId(cardDefId);
                card.setStarLevel(1);
                card.setSpareCopies(n - 1);
                card.setAcquiredAt(now);
                playerCardRepository.save(card);
            }
        }
        playerRepository.save(player);

        GameStateResponse state = buildState(player, t.slots());
        return new DrawResult(pulls, gradeCounts, totalCost, state);
    }

    /**
     * 카드 강화. 목표 성급(현재+1)마다 카드 복사본 1개 + 골드를 소모하고 확률에 따라 성공 여부를 결정한다.
     * 실패해도 소모한 복사본과 골드는 돌려주지 않는다.
     */
    @Transactional
    public UpgradeResult upgrade(UUID playerId, Long playerCardId) {
        return duplicateRequestGuard.run(playerId + ":upgrade:" + playerCardId,
                () -> doUpgrade(playerId, playerCardId));
    }

    private UpgradeResult doUpgrade(UUID playerId, Long playerCardId) {
        Ticked t = tick(playerId);
        Player player = t.player();
        PlayerCard card = playerCardRepository.findById(playerCardId)
                .filter(pc -> pc.getPlayerId().equals(playerId))
                .orElseThrow(() -> new EntityNotFoundException("보유하지 않은 카드입니다: " + playerCardId));

        int targetLevel = card.getStarLevel() + 1;
        UpgradeTable.Step step = UpgradeTable.stepFor(targetLevel);
        if (step == null) {
            throw new IllegalStateException("이미 최대 강화 단계입니다.");
        }
        if (card.getSpareCopies() < UpgradeTable.REQUIRED_COPIES) {
            throw new IllegalStateException(
                    "카드 복사본이 부족합니다. (필요: " + UpgradeTable.REQUIRED_COPIES + ", 보유: " + card.getSpareCopies() + ")");
        }
        if (player.getGold() < step.goldCost()) {
            throw new IllegalStateException("골드가 부족합니다. (필요: " + step.goldCost() + ")");
        }

        card.setSpareCopies(card.getSpareCopies() - UpgradeTable.REQUIRED_COPIES);
        player.setGold(player.getGold() - step.goldCost());
        boolean success = random.nextDouble() < step.successProbability();
        if (success) {
            card.setStarLevel(targetLevel);
        }
        playerCardRepository.save(card);
        playerRepository.save(player);

        CardDefinition def = cardCatalogService.getOrThrow(card.getCardDefinitionId());
        Integer slotIndex = findSlotIndex(t.slots(), card.getId());
        GameStateResponse state = buildState(player, t.slots());

        return new UpgradeResult(
                success, PlayerCardDto.from(card, def, slotIndex), UpgradeTable.REQUIRED_COPIES,
                step.successProbability(), state);
    }

    /**
     * 카드를 분해해서 조각을 얻는다. 보유한 복사본(spareCopies) 범위 내에서만 가능하며,
     * 슬롯에 배치된 카드 본체나 강화에 쓰인 성급은 사라지지 않는다.
     */
    @Transactional
    public ShredResult shredCard(UUID playerId, Long playerCardId, int count) {
        return duplicateRequestGuard.run(playerId + ":shred:" + playerCardId,
                () -> doShredCard(playerId, playerCardId, count));
    }

    private ShredResult doShredCard(UUID playerId, Long playerCardId, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("분해 개수는 1개 이상이어야 합니다.");
        }
        Ticked t = tick(playerId);
        Player player = t.player();
        PlayerCard card = playerCardRepository.findById(playerCardId)
                .filter(pc -> pc.getPlayerId().equals(playerId))
                .orElseThrow(() -> new EntityNotFoundException("보유하지 않은 카드입니다: " + playerCardId));

        if (card.getSpareCopies() < count) {
            throw new IllegalStateException(
                    "분해할 복사본이 부족합니다. (요청: " + count + ", 보유: " + card.getSpareCopies() + ")");
        }

        CardDefinition def = cardCatalogService.getOrThrow(card.getCardDefinitionId());
        int shardsGained = CardShardTable.shredYield(def.getGrade()) * count;

        card.setSpareCopies(card.getSpareCopies() - count);
        player.setCardShards(player.getCardShards() + shardsGained);
        playerCardRepository.save(card);
        playerRepository.save(player);

        GameStateResponse state = buildState(player, t.slots());
        return new ShredResult(count, shardsGained, player.getCardShards(), state);
    }

    /** 카드 조각을 소모해서 원하는 카드를 직접 획득한다 (등급이 높을수록 필요 조각이 많다). */
    @Transactional
    public CraftResult craftCard(UUID playerId, Long cardDefinitionId) {
        return duplicateRequestGuard.run(playerId + ":craft:" + cardDefinitionId,
                () -> doCraftCard(playerId, cardDefinitionId));
    }

    private CraftResult doCraftCard(UUID playerId, Long cardDefinitionId) {
        Ticked t = tick(playerId);
        Player player = t.player();
        CardDefinition def = cardCatalogService.getOrThrow(cardDefinitionId);
        int cost = CardShardTable.craftCost(def.getGrade());

        if (player.getCardShards() < cost) {
            throw new IllegalStateException("카드 조각이 부족합니다. (필요: " + cost + ", 보유: " + player.getCardShards() + ")");
        }
        player.setCardShards(player.getCardShards() - cost);

        var existing = playerCardRepository.findByPlayerIdAndCardDefinitionId(playerId, cardDefinitionId);
        PlayerCard playerCard;
        if (existing.isPresent()) {
            playerCard = existing.get();
            playerCard.setSpareCopies(playerCard.getSpareCopies() + 1);
        } else {
            playerCard = new PlayerCard();
            playerCard.setPlayerId(playerId);
            playerCard.setCardDefinitionId(cardDefinitionId);
            playerCard.setStarLevel(1);
            playerCard.setSpareCopies(0);
            playerCard.setAcquiredAt(Instant.now());
        }
        playerCardRepository.save(playerCard);
        playerRepository.save(player);

        GameStateResponse state = buildState(player, t.slots());
        return new CraftResult(
                PlayerCardDto.from(playerCard, def, findSlotIndex(t.slots(), playerCard.getId())),
                cost, player.getCardShards(), state);
    }

    /** 클릭해서 받는 타입의 이벤트(보물상자, 조각 더미)를 수령한다. */
    @Transactional
    public ClaimEventResult claimEvent(UUID playerId) {
        return duplicateRequestGuard.run(playerId + ":claimEvent", () -> doClaimEvent(playerId));
    }

    private ClaimEventResult doClaimEvent(UUID playerId) {
        Ticked t = tick(playerId);
        Player player = t.player();

        EventType type = player.getActiveEventType();
        Instant now = Instant.now();
        if (type == null || player.getActiveEventEndsAt() == null || now.isAfter(player.getActiveEventEndsAt())) {
            throw new IllegalStateException("현재 받을 수 있는 이벤트가 없습니다.");
        }
        if (!EventTable.isClaimable(type)) {
            throw new IllegalStateException("이 이벤트는 자동으로 적용되며 별도로 받을 필요가 없습니다.");
        }
        if (player.isActiveEventClaimed()) {
            throw new IllegalStateException("이미 보상을 받았습니다.");
        }

        double goldGained = 0;
        int shardsGained = 0;
        if (type == EventType.TREASURE_CHEST) {
            goldGained = EventTable.TREASURE_CHEST_GOLD_REWARD;
            player.setGold(player.getGold() + goldGained);
            player.setTotalGoldEarned(player.getTotalGoldEarned() + goldGained);
        } else if (type == EventType.SHARD_CACHE) {
            shardsGained = EventTable.SHARD_CACHE_REWARD;
            player.setCardShards(player.getCardShards() + shardsGained);
        }
        player.setActiveEventClaimed(true);
        playerRepository.save(player);

        GameStateResponse state = buildState(player, t.slots());
        return new ClaimEventResult(type, EventTable.descriptionOf(type), goldGained, shardsGained, state);
    }

    @Transactional
    public GameStateResponse placeCard(UUID playerId, int slotIndex, Long playerCardId) {
        return duplicateRequestGuard.run(playerId + ":slots", () -> doPlaceCard(playerId, slotIndex, playerCardId));
    }

    private GameStateResponse doPlaceCard(UUID playerId, int slotIndex, Long playerCardId) {
        Ticked t = tick(playerId);
        playerRepository.save(t.player());

        Slot targetSlot = slotRepository.findByPlayerIdAndSlotIndex(playerId, slotIndex)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 슬롯입니다: " + slotIndex));

        PlayerCard card = playerCardRepository.findById(playerCardId)
                .filter(pc -> pc.getPlayerId().equals(playerId))
                .orElseThrow(() -> new EntityNotFoundException("보유하지 않은 카드입니다: " + playerCardId));

        // 이미 다른 슬롯에 배치되어 있다면 그 슬롯을 비운다 (이동 처리)
        slotRepository.findByPlayerIdAndPlayerCard_Id(playerId, playerCardId)
                .ifPresent(s -> s.setPlayerCard(null));

        targetSlot.setPlayerCard(card);
        slotRepository.save(targetSlot);

        List<Slot> freshSlots = slotRepository.findByPlayerIdOrderBySlotIndexAsc(playerId);
        return buildState(t.player(), freshSlots);
    }

    @Transactional
    public GameStateResponse clearSlot(UUID playerId, int slotIndex) {
        return duplicateRequestGuard.run(playerId + ":slots", () -> doClearSlot(playerId, slotIndex));
    }

    private GameStateResponse doClearSlot(UUID playerId, int slotIndex) {
        Ticked t = tick(playerId);
        playerRepository.save(t.player());

        Slot slot = slotRepository.findByPlayerIdAndSlotIndex(playerId, slotIndex)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 슬롯입니다: " + slotIndex));
        slot.setPlayerCard(null);
        slotRepository.save(slot);

        List<Slot> freshSlots = slotRepository.findByPlayerIdOrderBySlotIndexAsc(playerId);
        return buildState(t.player(), freshSlots);
    }

    @Transactional
    public GameStateResponse unlockSlot(UUID playerId) {
        return duplicateRequestGuard.run(playerId + ":slots", () -> doUnlockSlot(playerId));
    }

    private GameStateResponse doUnlockSlot(UUID playerId) {
        Ticked t = tick(playerId);
        Player player = t.player();

        // player.slotCount는 표시용 캐시일 뿐이고, 다음 슬롯 인덱스는 항상 실제 슬롯 개수/최대
        // 인덱스를 기준으로 계산한다. slotCount가 실제 슬롯 데이터와 어긋나 있더라도(과거 동시
        // 요청 등으로) 여기서 항상 맞는 다음 인덱스를 구하도록 해서 유니크 제약 충돌을 막는다.
        int nextIndex = t.slots().stream().mapToInt(Slot::getSlotIndex).max().orElse(-1) + 1;
        int nextSlotCount = nextIndex + 1;

        if (nextSlotCount > MAX_SLOT_COUNT) {
            throw new IllegalStateException("이미 최대 슬롯 수(" + MAX_SLOT_COUNT + "개)에 도달했습니다.");
        }

        double cost = slotUnlockCost(nextSlotCount);
        if (player.getGold() < cost) {
            throw new IllegalStateException("골드가 부족합니다. (필요: " + cost + ")");
        }
        player.setGold(player.getGold() - cost);
        player.setSlotCount(nextSlotCount);
        playerRepository.save(player);
        slotRepository.save(new Slot(playerId, nextIndex));

        List<Slot> freshSlots = slotRepository.findByPlayerIdOrderBySlotIndexAsc(playerId);
        return buildState(player, freshSlots);
    }

    private PlayerStateResponse toPlayerState(Player player, double productionPerMinute) {
        boolean maxed = player.getSlotCount() >= MAX_SLOT_COUNT;
        double nextSlotCost = maxed ? 0 : slotUnlockCost(player.getSlotCount() + 1);
        ActiveEventDto activeEvent = buildActiveEventDto(player, Instant.now());
        return new PlayerStateResponse(player.getId(), player.getGold(), player.getTotalGoldEarned(),
                player.getCardShards(), player.getSlotCount(), nextSlotCost, maxed, productionPerMinute, activeEvent);
    }

    private SlotDto toSlotDto(Slot s) {
        if (s.getPlayerCard() == null) {
            return new SlotDto(s.getSlotIndex(), null);
        }
        CardDefinition def = cardCatalogService.getOrThrow(s.getPlayerCard().getCardDefinitionId());
        return new SlotDto(s.getSlotIndex(), PlayerCardDto.from(s.getPlayerCard(), def, s.getSlotIndex()));
    }

    private double slotUnlockCost(int slotNumber) {
        return SLOT_UNLOCK_BASE_COST * Math.pow(SLOT_UNLOCK_GROWTH, slotNumber - Player.STARTING_SLOT_COUNT);
    }

    private CardDefinition rollCard() {
        return rollCardFromRates(GRADE_RATE);
    }

    private CardDefinition rollPremiumCard() {
        return rollCardFromRates(PREMIUM_GRADE_RATE);
    }

    private CardDefinition rollCardFromRates(Map<CardGrade, Double> rates) {
        double roll = random.nextDouble();
        double cumulative = 0;
        CardGrade selectedGrade = null;
        for (var entry : rates.entrySet()) {
            cumulative += entry.getValue();
            if (roll <= cumulative) {
                selectedGrade = entry.getKey();
                break;
            }
        }
        if (selectedGrade == null) {
            selectedGrade = rates.keySet().iterator().next();
        }
        List<CardDefinition> candidates = cardCatalogService.byGrade(selectedGrade);
        if (candidates.isEmpty()) {
            candidates = cardCatalogService.all();
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private Integer findSlotIndex(List<Slot> slots, Long playerCardId) {
        return slots.stream()
                .filter(s -> s.getPlayerCard() != null && s.getPlayerCard().getId().equals(playerCardId))
                .map(Slot::getSlotIndex)
                .findFirst()
                .orElse(null);
    }

    private Player getPlayerOrThrow(UUID playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new EntityNotFoundException("플레이어를 찾을 수 없습니다: " + playerId));
    }
}
