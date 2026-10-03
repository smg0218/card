package com.idlecard.game.card.cardService;

import com.idlecard.game.card.cardEntity.CardDefinition;
import com.idlecard.game.card.cardEntity.CardGrade;
import com.idlecard.game.card.cardRepository.CardDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 카드 정의는 정적 데이터이며 자주 바뀌지 않으므로 매 요청마다 원격 DB(Neon)를 조회하지 않고
 * 애플리케이션 시작 시 1회 메모리에 캐시해서 사용한다. 원격 DB 왕복(네트워크 지연)을 줄여
 * API 응답 속도를 크게 개선한다.
 */
@Service
@RequiredArgsConstructor
public class CardCatalogService {

    private final CardDefinitionRepository repository;

    private volatile Map<Long, CardDefinition> byId = Map.of();
    private volatile Map<CardGrade, List<CardDefinition>> byGrade = new EnumMap<>(CardGrade.class);
    private volatile List<CardDefinition> all = List.of();

    // CardDataSeeder(CommandLineRunner)가 초기 데이터 삽입을 마친 뒤에 캐시를 채우기 위해
    // 컨텍스트 초기화가 완전히 끝나는 ApplicationReadyEvent 시점에 로드한다.
    @EventListener(ApplicationReadyEvent.class)
    public void refresh() {
        // 카드가 추가된 순서(DB 삽입 순서)와 무관하게, 목록에서는 항상 등급순으로 보이게 정렬한다.
        List<CardDefinition> defs = repository.findAll().stream()
                .sorted(java.util.Comparator
                        .comparing(CardDefinition::getGrade)
                        .thenComparing(CardDefinition::getId))
                .toList();
        this.all = List.copyOf(defs);
        this.byId = defs.stream().collect(Collectors.toMap(CardDefinition::getId, d -> d));
        this.byGrade = defs.stream().collect(Collectors.groupingBy(CardDefinition::getGrade));
    }

    public CardDefinition getOrThrow(Long id) {
        CardDefinition def = byId.get(id);
        if (def == null) {
            throw new IllegalStateException("존재하지 않는 카드 정의입니다: " + id);
        }
        return def;
    }

    public List<CardDefinition> all() {
        return all;
    }

    public List<CardDefinition> byGrade(CardGrade grade) {
        return byGrade.getOrDefault(grade, List.of());
    }
}
