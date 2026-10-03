package com.idlecard.game.card.cardBootstrap;

import com.idlecard.game.card.cardEntity.CardDefinition;
import com.idlecard.game.card.cardEntity.CardGrade;
import com.idlecard.game.card.cardRepository.CardDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 카드 도감을 채운다. 코드(code) 기준으로 이미 있는 카드는 건드리지 않고, 없는 카드만 추가한다.
 * 그래서 이미 플레이 중인 계정이 있어도(카드가 이미 지급/뽑혀 있어도) 안전하게 카드 종류를
 * 늘릴 수 있다.
 */
@Component
@RequiredArgsConstructor
public class CardDataSeeder implements CommandLineRunner {

    private final CardDefinitionRepository repository;

    @Override
    public void run(String... args) {
        for (CardDefinition def : allDefinitions()) {
            if (repository.findByCode(def.getCode()).isEmpty()) {
                repository.save(def);
            }
        }
    }

    private List<CardDefinition> allDefinitions() {
        return List.of(
                // ── NORMAL (16) ──
                card("farmer", "농부", CardGrade.NORMAL, List.of("자연", "생산"), 30,
                        "10초마다 골드를 생산하는 기본 카드", false, false, null, 0),
                card("miner", "광부", CardGrade.NORMAL, List.of("광물", "생산"), 32,
                        "광물을 채굴해 골드를 생산한다", false, false, null, 0),
                card("fisherman", "어부", CardGrade.NORMAL, List.of("자연", "생산"), 28,
                        "물고기를 잡아 골드를 생산한다", false, false, null, 0),
                card("shepherd", "목동", CardGrade.NORMAL, List.of("자연", "생산"), 29,
                        "가축을 길러 골드를 생산한다", false, false, null, 0),
                card("lumberjack", "벌목꾼", CardGrade.NORMAL, List.of("자연", "생산"), 31,
                        "목재를 베어 골드를 생산한다", false, false, null, 0),
                card("blacksmith", "대장장이", CardGrade.NORMAL, List.of("기계", "생산"), 33,
                        "도구를 만들어 골드를 생산한다", false, false, null, 0),
                card("quarryman", "채석공", CardGrade.NORMAL, List.of("광물", "생산"), 30,
                        "돌을 캐내 골드를 생산한다", false, false, null, 0),
                card("peddler", "행상인", CardGrade.NORMAL, List.of("경제", "인간"), 27,
                        "이곳저곳 물건을 팔아 골드를 생산한다", false, false, null, 0),
                card("scout", "정찰병", CardGrade.NORMAL, List.of("탐험", "인간"), 26,
                        "주변을 탐색해 소량의 골드를 생산한다", false, false, null, 0),
                card("apprentice", "견습생", CardGrade.NORMAL, List.of("마법", "생산"), 25,
                        "아직 미숙하지만 마법으로 골드를 생산한다", false, false, null, 0),
                card("carpenter", "목수", CardGrade.NORMAL, List.of("기계", "생산"), 32,
                        "가구를 만들어 골드를 생산한다", false, false, null, 0),
                card("sheepdog", "양치기개", CardGrade.NORMAL, List.of("자연"), 24,
                        "가축을 지키며 소량의 골드를 생산한다", false, false, null, 0),
                card("potter", "도공", CardGrade.NORMAL, List.of("생산"), 28,
                        "도자기를 빚어 골드를 생산한다", false, false, null, 0),
                card("porter", "등짐장수", CardGrade.NORMAL, List.of("경제", "탐험"), 27,
                        "짐을 날라 골드를 생산한다", false, false, null, 0),
                card("soldier", "병사", CardGrade.NORMAL, List.of("인간", "전투"), 30,
                        "훈련받은 병사, 소량의 골드를 생산한다", false, false, null, 0),
                card("novice_mage", "견습 마법사", CardGrade.NORMAL, List.of("마법"), 26,
                        "마법을 배우는 중인 초보 마법사", false, false, null, 0),

                // ── RARE (14) ──
                card("alchemist", "연금술사", CardGrade.RARE, List.of("마법", "생산"), 90,
                        "마법 카드 3장 이상이면 생산 시너지가 강화된다", false, false, null, 0),
                card("merchant", "상인", CardGrade.RARE, List.of("경제", "인간"), 80,
                        "무역을 통해 골드를 생산한다", false, false, null, 0),
                card("knight", "기사", CardGrade.RARE, List.of("인간", "전투"), 85,
                        "전투 훈련으로 단련된 카드", false, false, null, 0),
                card("sage", "현자", CardGrade.RARE, List.of("마법", "지식"), 75,
                        "지식을 통해 소량의 골드를 생산한다", false, false, null, 0),
                card("mechanic", "기계공", CardGrade.RARE, List.of("기계", "생산"), 85,
                        "정교한 기계를 다뤄 골드를 생산한다", false, false, null, 0),
                card("mercenary", "용병", CardGrade.RARE, List.of("인간", "전투"), 88,
                        "돈을 받고 싸우는 전투 전문가", false, false, null, 0),
                card("explorer", "탐험가", CardGrade.RARE, List.of("탐험", "인간"), 80,
                        "미지의 땅을 탐험해 골드를 생산한다", false, false, null, 0),
                card("shaman", "주술사", CardGrade.RARE, List.of("자연", "마법"), 82,
                        "자연의 힘을 빌려 골드를 생산한다", false, false, null, 0),
                card("banker", "은행가", CardGrade.RARE, List.of("경제"), 90,
                        "돈을 굴려 골드를 생산한다", false, false, null, 0),
                card("mine_foreman", "광산 감독관", CardGrade.RARE, List.of("광물", "생산"), 85,
                        "광산을 관리하며 생산을 늘린다", false, false, null, 0),
                card("knight_captain", "기사단장", CardGrade.RARE, List.of("인간", "전투"), 90,
                        "기사단을 이끄는 노련한 지휘관", false, false, null, 0),
                card("researcher", "연구원", CardGrade.RARE, List.of("마법", "지식"), 78,
                        "새로운 마법을 연구해 골드를 생산한다", false, false, null, 0),
                card("merchant_lord", "대상인", CardGrade.RARE, List.of("경제", "인간"), 92,
                        "큰 규모의 무역을 하는 상인", false, false, null, 0),
                card("beast_tamer", "야수조련사", CardGrade.RARE, List.of("자연", "전투"), 80,
                        "야수를 길들여 골드를 생산한다", false, false, null, 0),

                // ── UNIQUE (7) ──
                card("druid", "드루이드", CardGrade.UNIQUE, List.of("자연", "마법", "버프"), 40,
                        "자연 카드 생산량을 +15% 버프한다", false, false, "자연", 0.15),
                card("wandering_merchant", "떠돌이 상인", CardGrade.UNIQUE, List.of("경제"), 35,
                        "종류 시너지를 받지 않는 대신 이벤트 보상이 늘어난다 (베타: 생산에는 영향 없음)",
                        false, true, null, 0),
                card("elder", "대장로", CardGrade.UNIQUE, List.of("인간", "버프"), 38,
                        "인간 카드 생산량을 +12% 버프한다", false, false, "인간", 0.12),
                card("machinist", "기계 설계자", CardGrade.UNIQUE, List.of("기계", "버프"), 38,
                        "기계 카드 생산량을 +15% 버프한다", false, false, "기계", 0.15),
                card("abyss_watcher", "심연의 관측자", CardGrade.UNIQUE, List.of("탐험", "마법"), 42,
                        "미지의 힘으로 골드를 생산한다", false, false, null, 0),
                card("shadow_thief", "그림자 도적", CardGrade.UNIQUE, List.of("경제", "탐험"), 42,
                        "은밀하게 골드를 훔쳐온다", false, false, null, 0),
                card("storm_spirit", "폭풍의 정령", CardGrade.UNIQUE, List.of("자연", "마법", "버프"), 36,
                        "자연 카드 생산량을 +10% 버프한다 (드루이드와 중첩)", false, false, "자연", 0.10),

                // ── LEGENDARY (3, 기존 1 + 신규 2) ──
                card("king", "왕", CardGrade.LEGENDARY, List.of("인간", "지배", "버프"), 260,
                        "등급 시너지를 받지 않는 대신 전체 생산량을 +20% 버프한다",
                        true, false, "ALL", 0.20),
                card("queen", "여왕", CardGrade.LEGENDARY, List.of("인간", "지배", "버프"), 250,
                        "등급 시너지를 받지 않는 대신 전체 생산량을 +18% 버프한다",
                        true, false, "ALL", 0.18),
                card("archmage", "대현자", CardGrade.LEGENDARY, List.of("마법", "지식", "버프"), 240,
                        "마법 카드 생산량을 +25% 버프한다", false, false, "마법", 0.25)
        );
    }

    private CardDefinition card(String code, String name, CardGrade grade, List<String> tags,
                                 double baseProductionPerMinute, String description,
                                 boolean excludeFromGradeSynergy, boolean excludeFromTagSynergy,
                                 String buffTargetTag, double buffBonusPercent) {
        CardDefinition c = new CardDefinition();
        c.setCode(code);
        c.setName(name);
        c.setGrade(grade);
        c.setTags(tags);
        c.setBaseProductionPerMinute(baseProductionPerMinute);
        c.setDescription(description);
        c.setExcludeFromGradeSynergy(excludeFromGradeSynergy);
        c.setExcludeFromTagSynergy(excludeFromTagSynergy);
        c.setBuffTargetTag(buffTargetTag);
        c.setBuffBonusPercent(buffBonusPercent);
        return c;
    }
}
