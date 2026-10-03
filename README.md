# 방치형 카드 수집 게임 (Idle Card Game)

카드를 모으고, 강화하고, 제한된 슬롯에 배치해서 시너지를 맞추는 방치형(Idle/AFK) 카드 수집 게임입니다.

> 핵심 철학: **안 해도 성장한다. 자주 확인하면 조금 더 빠르게 성장한다. 좋은 카드를 모으는 것보다 좋은 카드 조합을 만드는 것이 중요하다.**

전체 게임 기획 의도는 [`방치형_카드게임_베타_기획안_최종.md`](./방치형_카드게임_베타_기획안_최종.md) 문서에 정리되어 있습니다.

## 게임 루프

1. 골드로 카드를 뽑는다 (등급별 확률은 아래 "카드 등급" 참고)
2. 중복으로 뽑은 카드는 강화(최대 10성) 또는 분해해서 카드 조각으로 교환
3. 제한된 슬롯(최대 10개, 골드로 확장 가능)에 카드를 배치
4. 같은 태그(종류)를 모으면 종류 시너지, 6개 등급을 모두 갖추면 등급 시너지가 추가로 발동
5. 시간이 지나며 자동으로 골드가 쌓이고(오프라인 중에도 일정 비율로 생산), 랜덤 이벤트(황금 시간/보물상자/조각 더미)가 가끔 발생
6. 쌓인 골드/조각으로 다시 뽑기·강화·슬롯 확장 → 반복

## 카드 등급

흔함 → 희귀 순으로 COMMON < UNCOMMON < RARE < UNIQUE < EPIC < LEGENDARY 6단계입니다.

| 등급 | 일반 뽑기 | 고급 뽑기 |
|---|---:|---:|
| COMMON | 62% | - |
| UNCOMMON | 25% | - |
| RARE | 10% | 86% |
| UNIQUE | 2.5% | 10% |
| EPIC | 0.4% | 3.5% |
| LEGENDARY | 0.1% | 0.5% |

고급 뽑기는 COMMON/UNCOMMON을 제외하고 RARE 이상에서만 나옵니다.

## 기술 스택

| | |
|---|---|
| **Backend** | Java 21, Spring Boot 3.3.4, Spring Data JPA (Hibernate), PostgreSQL (Neon), Spring Security Crypto(BCrypt), Lombok |
| **Frontend** | React 19, TypeScript, Vite |

## 프로젝트 구조

```
backend/   Spring Boot API 서버
frontend/  React + Vite SPA
```

백엔드는 레이어가 아니라 **기능(feature) 단위**로 패키지가 나뉘어 있습니다 (`user`, `card`, `player`, `leaderboard`). 각 기능 폴더 안에 `*Controller` / `*Service` / `*Repository` / `*DTO`(`request`/`response` 하위 분리) / `*Entity`가 들어있습니다. 더 자세한 아키텍처 설명은 [`CLAUDE.md`](./CLAUDE.md)를 참고하세요.

## 시작하기

### 백엔드

Postgres DB(예: Neon)와 다음 3개 환경변수가 필요합니다. yml 파일에는 DB 접속 정보를 직접 적지 않고 환경변수로만 주입받습니다.

```
DB_URL=jdbc:postgresql://<host>/<db>?sslmode=require
DB_USERNAME=<username>
DB_PASSWORD=<password>
```

IntelliJ에서 실행한다면 Run Configuration의 Environment variables에 위 3개를 등록하면 됩니다.

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```
(전역에 Maven이 없다면 `tools/apache-maven-3.9.9/bin/mvn.cmd`를 사용하세요.)

서버는 기본적으로 `8080` 포트에서 실행되고, 시작 시 `CardDataSeeder`가 카드 도감을 자동으로 채웁니다. 스키마는 `ddl-auto: update`로 자동 반영됩니다.

### 프론트엔드

```bash
cd frontend
npm install
npm run dev
```

`frontend/src/api.ts`의 `API_BASE`가 `http://localhost:8080/api`로 고정되어 있으므로, 백엔드를 먼저 그 포트로 띄워야 합니다.

## 주요 기능

- **계정**: 개인정보 없이 ID/비밀번호/닉네임만으로 가입·로그인·비밀번호 변경·탈퇴
- **카드 뽑기**: 일반/고급 뽑기, 최대 100연차까지 한 번에 처리
- **강화·분해·제작**: 카드 복사본으로 강화, 필요 없는 복사본은 분해해서 조각으로, 조각을 모아 원하는 카드를 제작
- **슬롯 배치 & 시너지**: 슬롯에 카드를 배치하면 태그 시너지·등급 시너지·버프 카드 효과가 실시간으로 계산되어 생산량에 반영
- **오프라인 보상**: 접속하지 않은 동안에도 일정 비율로 골드가 쌓임 (최대 상한 있음)
- **랜덤 이벤트**: 황금 시간(생산량 부스트), 보물상자, 조각 더미가 주기적으로 랜덤 발생
- **랭킹**: 누적 획득 골드 기준 리더보드
