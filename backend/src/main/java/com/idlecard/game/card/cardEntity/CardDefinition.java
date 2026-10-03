package com.idlecard.game.card.cardEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "card_definition")
@Comment("카드 도감 정의. 정적 데이터이며 CardCatalogService가 애플리케이션 시작 시 메모리에 캐시해서 사용.")
@Getter
@Setter
@NoArgsConstructor
public class CardDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK.")
    private Long id;

    @Column(nullable = false, unique = true)
    @Comment("카드 고유 코드 (영문, unique). 시더가 이미 존재하는 카드인지 판별하는 기준.")
    private String code;

    @Column(nullable = false)
    @Comment("카드 이름 (화면에 표시되는 한글 표시명).")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("카드 등급 (NORMAL/RARE/UNIQUE/LEGENDARY).")
    private CardGrade grade;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "card_definition_tags", joinColumns = @JoinColumn(name = "card_definition_id"))
    @Column(name = "tag")
    @Comment("태그 값 (예: 자연, 마법, 전투 등). 종류 시너지 집계 기준.")
    private List<String> tags = new ArrayList<>();

    /** Gold per minute produced at star level 1, before multipliers. */
    @Column(nullable = false)
    @Comment("1성 기준 분당 골드 생산량 (강화/시너지 배율 적용 전 기본값).")
    private double baseProductionPerMinute;

    @Column(length = 500)
    @Comment("카드 설명 텍스트 (화면 표시용).")
    private String description;

    /** 왕: 등급 시너지 보너스를 받지 않음 */
    @Column(nullable = false)
    @Comment("등급 시너지 보너스 적용 제외 여부 (예: 왕/여왕).")
    private boolean excludeFromGradeSynergy = false;

    /** 떠돌이 상인: 종류(태그) 시너지 보너스를 받지 않음 */
    @Column(nullable = false)
    @Comment("태그(종류) 시너지 보너스 적용 제외 여부 (예: 떠돌이 상인).")
    private boolean excludeFromTagSynergy = false;

    /**
     * 버프 카드 효과: 특정 태그(또는 "ALL")를 가진 카드들의 생산량에 퍼센트 보너스를 더한다.
     * 예: 드루이드 -> targetTag="자연", bonusPercent=0.15 / 왕 -> targetTag="ALL", bonusPercent=0.20
     */
    @Column
    @Comment("버프 대상 태그. \"ALL\"이면 전체 카드가 대상, null이면 버프 효과 없음.")
    private String buffTargetTag;

    @Column(nullable = false)
    @Comment("버프 대상에게 더해지는 생산량 보너스 비율 (예: 0.15 = +15%).")
    private double buffBonusPercent = 0.0;
}
