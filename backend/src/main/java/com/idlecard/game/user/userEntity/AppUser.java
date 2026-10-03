package com.idlecard.game.user.userEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
@Comment("로그인 계정. 개인정보 없이 ID/비밀번호/닉네임만으로 동작하는 최소 계정 시스템.")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    /** 로그인 ID. 개인정보를 받지 않으므로 사용자가 직접 정하는 아이디를 PK로 사용한다. */
    @Id
    @Column(length = 30)
    @Comment("로그인 ID (PK). 사용자가 직접 정하는 아이디.")
    private String id;

    @Column(nullable = false)
    @Comment("BCrypt로 해시된 비밀번호. 평문 비밀번호는 저장하지 않음.")
    private String passwordHash;

    @Column(nullable = false, unique = true, length = 20)
    @Comment("닉네임 (유일). 랭킹 등 화면에 표시되는 이름.")
    private String nickname;

    @Column(nullable = false, unique = true)
    @Comment("연결된 player(게임 데이터)의 ID. 계정은 이 ID를 통해 게임 데이터의 소유권만 가짐.")
    private UUID playerId;

    @Column(nullable = false)
    @Comment("계정 생성 시각.")
    private Instant createdAt;
}
