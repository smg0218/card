# CLAUDE.md

이 파일은 이 저장소에서 작업하는 Claude Code(claude.ai/code)에게 제공하는 가이드입니다.

## 프로젝트 개요

방치형(AFK) 카드 수집 게임입니다. 핵심 루프: 카드 획득 → 강화/복사본 축적 → 제한된 슬롯에 배치 → 슬롯 시너지(태그/등급 기준)로 골드 생산량 배율 상승 → 골드/조각으로 카드·강화·슬롯 추가 구매. 전체 게임 기획(등급, 시너지, 강화, 이벤트 규칙 등)은 `방치형_카드게임_베타_기획안_최종.md`에 있습니다.

한 저장소에 독립된 두 프로젝트가 있고, 둘 사이에 공유 툴링은 없습니다:
- `backend/` — Spring Boot 3.3.4 / Java 21 API 서버
- `frontend/` — React 19 + TypeScript + Vite SPA

## 명령어

### 백엔드 (`backend/`)
Maven이 전역에 설치되어 있지 않습니다 — `mvn`이 PATH에 없다면 번들로 포함된 `tools/apache-maven-3.9.9/bin/mvn.cmd` (Windows)를 사용하세요.

```
mvn clean compile       # 컴파일
mvn spring-boot:run     # API 서버 실행 (DB_URL/DB_USERNAME/DB_PASSWORD 환경변수 필요 — 아래 참고)
```
`src/test`에 아직 백엔드 테스트는 없습니다.

### 프론트엔드 (`frontend/`)
```
npm install
npm run dev       # Vite 개발 서버
npm run build     # tsc -b && vite build
npm run lint       # oxlint
```

## 환경설정 / DB 설정

DB 접속 정보는 `application.yml`에 **절대 하드코딩하지 않고**, 런타임에 환경변수로 주입받습니다:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 백엔드 실행 전에 반드시 설정해야 합니다 (로컬: IntelliJ Run Configuration의 Environment variables / 배포: 플랫폼 환경변수). 하나라도 없으면 시작 시점에 placeholder 해석 에러로 즉시 실패합니다 — 이건 버그가 아니라 의도된 동작입니다.
- DB는 Postgres(Neon, `ap-southeast-1`)이고, `spring.jpa.hibernate.ddl-auto: update`라 스키마 변경(컬럼 추가, 주석 등)은 애플리케이션 시작 시 자동으로 반영됩니다. 별도 마이그레이션 툴(Flyway/Liquibase 등)은 없습니다.
- 컬럼/테이블 설명은 raw SQL이 아니라 Hibernate의 `@Comment` 어노테이션을 엔티티 필드/클래스에 직접 붙여서 관리합니다 — DB 클라이언트로 열어보면 바로 보입니다. 새 엔티티 필드를 추가할 때도 별도 SQL 주석 스크립트가 아니라 `@Comment`를 계속 사용하세요.

## 백엔드 아키텍처

패키지는 레이어 우선이 아니라 **기능(feature) 우선**으로 구성되어 있습니다. `com.idlecard.game` 아래 각 기능 폴더는 자신만의 `*Controller` / `*Service` / `*Repository` / `*DTO` / `*Entity` 하위 패키지를 가집니다:

```
user/        — 계정/인증 (AppUser, 로그인/가입/탈퇴, BCrypt)
card/        — 카드 도감 (CardDefinition, CardGrade) — 정적 데이터, 메모리 캐시
player/      — 핵심 게임플레이: Player/PlayerCard/Slot, 뽑기/강화/분해/제작, 슬롯, 이벤트, 시너지
leaderboard/ — totalGoldEarned 기준 랭킹 (player + user 패키지를 읽음)
common/      — 전역 예외 처리 (ApiExceptionHandler)
config/      — 전역 설정 (WebConfig / CORS)
```
각 기능의 `*DTO` 폴더 안은 다시 `request/`, `response/` 하위 패키지로 나뉩니다.

기능 간 교차 import는 이 구조에서 정상이고 의도된 것입니다 (예: `user.userService.AuthService`는 `player.playerEntity.Player`와 `player.playerRepository.PlayerRepository`에 의존하고, `leaderboard`는 `user`와 `player` 양쪽을 읽습니다) — 이 분리는 기능 단위 분리이며, 레이어 간 결합을 막기 위한 경계가 아닙니다.

핵심 아키텍처 포인트:
- **`player` API는 UUID 기반으로 인증 없이 동작합니다.** `PlayerController`의 엔드포인트(`/api/players/{playerId}/...`)는 토큰 검증 없이 `playerId`(UUID)만으로 호출됩니다. `AppUser`(문자열 로그인 ID)와 `Player`(UUID 게임 데이터)를 의도적으로 별개의 엔티티/PK로 분리해 둔 이유는, 원래 계정 없이 UUID만으로 동작하던 게임 데이터 모델 위에 계정을 나중에 얹었기 때문이고, UUID를 쓰면 무인증 엔드포인트를 추측/열거하기도 어렵기 때문입니다. 두 ID를 섞어서 생각하지 마세요.
- **`GameService`가 `player`의 트랜잭션 핵심**입니다: 상태를 변경하는 모든 액션(`draw`, `upgrade`, `shredCard`, `craftCard`, `placeCard`, `clearSlot`, `unlockSlot`, `claimEvent`)은 먼저 내부 `tick()`을 호출해 경과 시간만큼 골드 생산을 정산(오프라인 생산은 감소된 비율로, `OFFLINE_MAX_MINUTES`로 상한)한 뒤 액션을 적용하고, 매번 전체 `GameStateResponse`를 돌려줘서 프론트엔드가 추가 조회를 할 필요가 없게 합니다.
- **`DuplicateRequestGuard`**가 상태를 변경하는 모든 서비스 메서드를 인메모리 키(`playerId:action[:targetId]`) 기반 락으로 감싸서, 동일한 요청이 처리 중일 때 두 번째 요청은 큐에 넣지 않고 바로 거절(409)합니다. 이건 서버가 하나뿐이라는 전제이며, Redis 등으로 분산 처리되지 않습니다.
- **게임 밸런스 고정값은 DB가 아니라 코드 상수**로 관리됩니다: `UpgradeTable`(강화 비용/성공률/성급별 생산 배율), `CardShardTable`(등급별 분해 수율/제작 비용), `EventTable`(랜덤 이벤트 주기/보상)은 모두 `player.playerService`에 `final` 클래스 + static `Map`으로 존재합니다. 밸런스를 조정하려면 이 클래스들을 수정하는 것이지, DB에 새 행을 넣는 게 아닙니다.
- **`CardCatalogService`**는 모든 `CardDefinition` 행을 메모리에 캐시하고, `CardDataSeeder`(`CommandLineRunner`)가 `code` 기준으로 없는 카드만 추가 삽입을 마친 뒤 `ApplicationReadyEvent` 시점에 한 번 갱신됩니다. 시딩 이후 카드 데이터는 읽기 전용입니다 — 새 카드를 추가하려면 `CardDataSeeder`에 항목을 추가하는 것이고, 별도 관리자 API는 없습니다.
- **`SynergyService.compute()`**는 태그 카운트 시너지, 전체 등급 보유 시너지, 버프 카드 배율을 호출할 때마다 현재 배치된 `Slot` 기준으로 매번 새로 계산합니다 (캐시/저장되지 않는 순수 파생 상태입니다).

## 프론트엔드 아키텍처

라우터 없는 단일 페이지 앱입니다: `main.tsx` → `App.tsx`. 인증 상태(세션 토큰이 포함된 `AuthResponse`)는 `localStorage`의 `idle-card-game.auth` 키에 저장되고 로드 시 다시 읽어옵니다. 저장된 인증 정보가 없으면 `AuthScreen.tsx`가 렌더링됩니다.

`api.ts`가 백엔드와의 계약 전체입니다: 손으로 작성한 `fetch` 래퍼 + 백엔드 DTO를 1:1로 그대로 옮긴 TypeScript 인터페이스(`CardDefinitionDto`, `PlayerCardDto`, `GameStateResponse` 등)로 구성됩니다. 코드 생성 도구는 없으므로, 백엔드 DTO 구조가 바뀌면 `api.ts`의 대응 인터페이스를 손으로 맞춰야 합니다. `API_BASE`는 `http://localhost:8080/api`로 하드코딩되어 있고 아직 환경변수 기반 설정은 없습니다.

앱은 주기적으로 백엔드를 폴링합니다 (`GameService`의 `ONLINE_TICK_GAP_SECONDS` 관련 주석 참고 — 그 간격보다 빠르게 폴링하는 것을 백엔드가 "온라인"으로, 그보다 느리면 "오프라인/접속 끊김"으로 간주하는 기준입니다).

## 커밋 메시지 규칙

### 6가지 규칙
1. 제목과 본문은 빈 행으로 구분한다.
2. 제목은 50글자 이내로 제한한다.
3. 제목의 첫 글자는 대문자로 작성한다.
4. 제목 끝에는 마침표를 넣지 않는다.
5. 제목은 명령문으로 작성하며 과거형을 사용하지 않는다.
6. 어떻게(How)보다는 무엇(What)과 왜(Why)를 설명한다.

### 커밋 메시지 구조
```
타입(스코프): 주제(제목)        ← Header (필수, 스코프는 생략 가능)
예1) fix(user) : 로그인 안되는 버그 수정
예2) refateor(card,slot) : card 및 slot에서 공용 코드를 추출하여 하나의 메서드로 통합
예3) docs : CLAUDE.md 문서 파일 내용 추가

본문                              ← Body (생략 가능)
예1) - 아이디와 비밀번호가 일치했는데도 로그인이 되지 않던 버그 수정
     - 비밀번호를 수정하지 못하던 버그 수정
예2) - card에서 카드명을 가져오는 코드가 중복되어 하나의 메서드로 통합
     - slot에서 slot 정보를 가져오는 코드가 중복되어 하나의 메서드로 통합
예3) - (생략되어서 본문이 없음)
```
Header, Body는 서로 빈 행으로 구분합니다.

### 타입 목록
| 타입 | 내용 |
|---|---|
| `feat` | 새로운 기능에 대한 커밋 |
| `fix` | 버그 수정에 대한 커밋 |
| `build` | 빌드 관련 파일 수정 / 모듈 설치 또는 삭제에 대한 커밋 |
| `chore` | 그 외 자잘한 수정에 대한 커밋 |
| `ci` | CI 관련 설정 수정에 대한 커밋 |
| `docs` | 문서 수정에 대한 커밋 |
| `style` | 코드 스타일 혹은 포맷 등에 관한 커밋 |
| `refactor` | 코드 리팩토링에 대한 커밋 |
| `test` | 테스트 코드 수정에 대한 커밋 |
| `perf` | 성능 개선에 대한 커밋 |

### Body / Footer 사용 기준
- Body는 Header에서 다 표현할 수 없는 상세 내용을 적습니다. Header로 충분하면 생략 가능합니다.

### 커밋을 묶는 기준
- 비슷한 기능에 대한 변경이면 하나의 커밋으로 묶어서 올려도 됩니다.
- 서로 다른 기능이면 커밋을 나눠서 올려야 합니다.
- 다만, 연관된 기능끼리 억지로 나눠서 올리면 중간 커밋 하나만 체크아웃했을 때 빌드/실행이 안 되는 상태가 될 수 있습니다. 이런 경우에는 기능이 여러 개라도, **그 커밋 하나만으로 실행했을 때 문제가 없는 단위**로 묶어서 커밋해야 합니다.
