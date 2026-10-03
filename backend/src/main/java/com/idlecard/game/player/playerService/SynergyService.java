package com.idlecard.game.player.playerService;

import com.idlecard.game.card.cardEntity.CardDefinition;
import com.idlecard.game.card.cardEntity.CardGrade;
import com.idlecard.game.card.cardService.CardCatalogService;
import com.idlecard.game.player.playerDTO.response.SynergyResponse;
import com.idlecard.game.player.playerEntity.PlayerCard;
import com.idlecard.game.player.playerEntity.Slot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SynergyService {

    // 종류(태그) 시너지 단계: N장 이상이면 보너스 % 적용 (최고 단계만 적용)
    private static final int[] TAG_THRESHOLDS = {2, 4, 6, 8};
    private static final double[] TAG_BONUS = {0.10, 0.25, 0.50, 0.70};

    private static final double GRADE_SYNERGY_BONUS = 0.08;

    private final CardCatalogService cardCatalogService;

    public SynergyResponse compute(List<Slot> slots) {
        List<Slot> placed = slots.stream().filter(s -> s.getPlayerCard() != null).toList();

        // 1. 태그 카운트 (시너지 제외 카드는 카운트에서 빠짐)
        Map<String, Integer> tagCounts = new TreeMap<>();
        for (Slot s : placed) {
            CardDefinition def = cardCatalogService.getOrThrow(s.getPlayerCard().getCardDefinitionId());
            if (def.isExcludeFromTagSynergy()) continue;
            for (String tag : def.getTags()) {
                tagCounts.merge(tag, 1, Integer::sum);
            }
        }

        // 2. 태그별 달성 보너스 %
        Map<String, Double> tagBonusPercent = new TreeMap<>();
        for (Map.Entry<String, Integer> e : tagCounts.entrySet()) {
            tagBonusPercent.put(e.getKey(), bonusForCount(e.getValue()));
        }

        // 3. 등급 시너지: 왕/여왕처럼 "등급 시너지 적용 불가"인 카드도 해당 등급 칸을 채운 것으로는
        // 인정한다. 다만 그 카드 자신은 아래에서 gradeMultiplier 계산 시 보너스를 받지 못한다.
        // 6개 등급(COMMON~LEGENDARY)을 전부 보유해야 활성화된다.
        Set<CardGrade> gradesPresent = new HashSet<>();
        for (Slot s : placed) {
            CardDefinition def = cardCatalogService.getOrThrow(s.getPlayerCard().getCardDefinitionId());
            gradesPresent.add(def.getGrade());
        }
        boolean gradeSynergyActive = gradesPresent.containsAll(List.of(CardGrade.values()));

        // 4. 버프 카드 효과 수집 (드루이드, 왕 등)
        record Buff(String targetTag, double bonusPercent) {}
        List<Buff> buffs = new ArrayList<>();
        for (Slot s : placed) {
            CardDefinition def = cardCatalogService.getOrThrow(s.getPlayerCard().getCardDefinitionId());
            if (def.getBuffTargetTag() != null && def.getBuffBonusPercent() != 0) {
                buffs.add(new Buff(def.getBuffTargetTag(), def.getBuffBonusPercent()));
            }
        }

        // 5. 카드별 최종 생산량 계산
        List<SynergyResponse.CardProductionDto> perCard = new ArrayList<>();
        double total = 0;
        for (Slot s : placed) {
            PlayerCard pc = s.getPlayerCard();
            CardDefinition def = cardCatalogService.getOrThrow(pc.getCardDefinitionId());

            double base = def.getBaseProductionPerMinute() * UpgradeTable.multiplierFor(pc.getStarLevel());

            double tagMultiplier = 1.0;
            if (!def.isExcludeFromTagSynergy()) {
                for (String tag : def.getTags()) {
                    tagMultiplier *= (1 + tagBonusPercent.getOrDefault(tag, 0.0));
                }
            }

            double gradeMultiplier = (gradeSynergyActive && !def.isExcludeFromGradeSynergy())
                    ? (1 + GRADE_SYNERGY_BONUS) : 1.0;

            double buffMultiplier = 1.0;
            for (Buff b : buffs) {
                if ("ALL".equals(b.targetTag()) || def.getTags().contains(b.targetTag())) {
                    buffMultiplier += b.bonusPercent();
                }
            }

            double finalProduction = base * tagMultiplier * gradeMultiplier * buffMultiplier;
            total += finalProduction;

            perCard.add(new SynergyResponse.CardProductionDto(
                    pc.getId(), def.getName(), s.getSlotIndex(), base, finalProduction
            ));
        }

        return new SynergyResponse(
                tagCounts, tagBonusPercent, gradeSynergyActive, gradeSynergyActive ? GRADE_SYNERGY_BONUS : 0.0,
                total, perCard
        );
    }

    public double totalProductionPerMinute(List<Slot> slots) {
        return compute(slots).totalProductionPerMinute();
    }

    private double bonusForCount(int count) {
        double bonus = 0.0;
        for (int i = 0; i < TAG_THRESHOLDS.length; i++) {
            if (count >= TAG_THRESHOLDS[i]) {
                bonus = TAG_BONUS[i];
            }
        }
        return bonus;
    }
}
