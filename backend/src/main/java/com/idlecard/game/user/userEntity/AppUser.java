package com.idlecard.game.user.userEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    /** 로그인 ID. 개인정보를 받지 않으므로 사용자가 직접 정하는 아이디를 PK로 사용한다. */
    @Id
    @Column(length = 30)
    private String id;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, unique = true, length = 20)
    private String nickname;

    @Column(nullable = false, unique = true)
    private UUID playerId;

    @Column(nullable = false)
    private Instant createdAt;
}
