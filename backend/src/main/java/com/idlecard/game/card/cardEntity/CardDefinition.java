package com.idlecard.game.card.cardEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "card_definition")
@Getter
@Setter
@NoArgsConstructor
public class CardDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardGrade grade;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "card_definition_tags", joinColumns = @JoinColumn(name = "card_definition_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    /** Gold per minute produced at star level 1, before multipliers. */
    @Column(nullable = false)
    private double baseProductionPerMinute;

    @Column(length = 500)
    private String description;

    /** 왕: 등급 시너지 보너스를 받지 않음 */
    @Column(nullable = false)
    private boolean excludeFromGradeSynergy = false;

    /** 떠돌이 상인: 종류(태그) 시너지 보너스를 받지 않음 */
    @Column(nullable = false)
    private boolean excludeFromTagSynergy = false;

    /**
     * 버프 카드 효과: 특정 태그(또는 "ALL")를 가진 카드들의 생산량에 퍼센트 보너스를 더한다.
     * 예: 드루이드 -> targetTag="자연", bonusPercent=0.15 / 왕 -> targetTag="ALL", bonusPercent=0.20
     */
    @Column
    private String buffTargetTag;

    @Column(nullable = false)
    private double buffBonusPercent = 0.0;
}
