# 너빼고 다옴 (All But You) - Backend

> 늦는 친구 위치까지 고려해서, 먼저 온 사람들이 다음 장소와 할 일을 정한다.

🔗 **서비스 바로가기:** https://allbutyou.devnoco.com

MEDILUX × 잇타 × UMC 하프해커톤 (2026.11.14)

<br>

## 📌 서비스 소개

모임에서 버려지는 **"늦는 친구를 기다리는 10분"**을 재설계합니다.

- **실시간 위치 확인**: 멤버들이 어디쯤 왔는지, 모임 장소까지 얼마나 남았는지 확인
- **부재 중 투표**: 방장이 메뉴·장소 같은 투표를 만들면, 지각자가 없어도 먼저 온 사람들끼리 결정
- **지각자 벌칙 게임**: 약속 시간이 지나면 지각자 팀 vs 도착자 팀으로 T-Rex 게임 대결, 결과에 따라 지각비 면제 또는 2배

<br>

## 🛠 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.1, Spring Data JPA |
| Database | MySQL 8 |
| Realtime | WebSocket (STOMP) |
| Docs | Swagger (springdoc-openapi 3) |
| Infra | Docker, AWS EC2, GitHub Actions |

<br>

## 🏗 아키텍처

```
[Frontend (Vercel)]
        │  REST + WebSocket(STOMP)
        ▼
[Spring Boot]
 ├─ room       모임 생성·참여·조회, 도착 관리
 ├─ location   위치 갱신, 거리 계산, 100m 진입 판정
 ├─ poll       투표 생성·선택지·투표·최종 결정
 ├─ game       게임 시작, 점수 제출, 팀 평균 판정, 지각비
 └─ common     인증, WebSocket 방송, 공통 응답·예외
        │
        ▼
    [MySQL]
```

- 도메인별 패키지로 나누고, 도메인 간 직접 의존 대신 이벤트로 연결
- 상태 변경은 **트랜잭션 커밋 후(AFTER_COMMIT)** WebSocket으로 방송
- 회원가입 없이 **모임 참여 시 발급되는 토큰**(`X-Participant-Token`)으로 인증

<br>

## 📡 API

| 도메인 | Method | API | 설명 |
|---|---|---|---|
| Room | POST | `/api/rooms` | 모임 생성 + 참여 코드 발급 + 방장 등록 |
| Room | POST | `/api/rooms/join` | 참여 코드 + 닉네임으로 입장 |
| Room | GET | `/api/rooms/{roomId}` | 모임 정보·참가자·약속 시간 조회 |
| Location | PATCH | `/api/rooms/{roomId}/members/{memberId}/location` | 위치 갱신·거리 계산·100m 판정 |
| Arrival | PATCH | `/api/rooms/{roomId}/members/{memberId}/arrival` | 방장의 도착 확인·도착 시각 수정 |
| Poll | POST | `/api/rooms/{roomId}/polls` | 투표 생성 (방장) |
| Poll | POST | `/api/polls/{pollId}/options` | 선택지 추가 (방장) |
| Poll | GET | `/api/rooms/{roomId}/polls` | 투표 목록·득표 수·미응답 인원 |
| Poll | PUT | `/api/polls/{pollId}/votes` | 투표·투표 변경 (1인 1표) |
| Poll | PATCH | `/api/polls/{pollId}/decision` | 최종 결정 (방장) |
| Game | POST | `/api/rooms/{roomId}/game/start` | 게임 시작 (방장, 약속 시간 이후) |
| Game | POST | `/api/rooms/{roomId}/game/scores` | 게임 점수 제출 |
| Game | POST | `/api/rooms/{roomId}/game/finish` | 게임 종료 (방장) |
| Game | GET | `/api/rooms/{roomId}/game/result` | 팀 평균·승패·지각비 결과 |

상세 명세는 서버 실행 후 Swagger에서 확인할 수 있습니다: `/swagger-ui/index.html`

### WebSocket

- 엔드포인트: `/ws`
- 구독: `/topic/rooms/{roomId}`
- 메시지 형식: `{ "type": "...", "data": {...}, "sentAt": "..." }`

| type | 시점 |
|---|---|
| `MEMBER_UPDATE` | 멤버 참여 |
| `ARRIVED` | 도착 처리 |
| `POLL_UPDATE` | 투표 생성·선택지 추가·투표 |
| `POLL_DECIDED` | 투표 최종 결정 |
| `GAME_START` | 게임 시작 (팀 구성 포함) |
| `GAME_RESULT` | 점수 제출·게임 종료 |

<br>

## 🎮 핵심 로직

### 지각 시간

```
지각 시간(분) = max(0, 실제 도착 시각 - 약속 시각)
```

### 게임 판정

게임 시작 순간 팀을 고정합니다. 약속 시간까지 도착한 사람은 **도착자 팀**, 아직 안 왔거나 늦게 온 사람은 **지각자 팀**입니다.

| 결과 | 조건 | 지각자 1인당 지각비 |
|---|---|---|
| 승리 | 지각자 평균 > 도착자 평균 | 0원 |
| 패배 | 지각자 평균 < 도착자 평균 | 기본 지각비 × 2 |
| 무승부 | 지각자 평균 = 도착자 평균 | 기본 지각비 |
| 대기 | 한 팀이라도 제출자 0명 | 기본 지각비 |

<br>

## 🚀 로컬 실행

```bash
# 1. MySQL 실행
docker compose up -d

# 2. 서버 실행
./gradlew bootRun

# 3. Swagger 접속
open http://localhost:8080/swagger-ui/index.html
```

**Swagger로 테스트하기**

1. `POST /api/rooms`로 모임 생성 → 응답의 `token` 복사
2. 오른쪽 위 **Authorize**에 토큰 입력
3. 나머지 API 테스트

<br>

## 👥 팀원

| 이름 | 역할 | GitHub |
|---|---|---|
| 이성훈 | Backend (모임·도착·투표·게임, 공통 설정) | [@shlee811](https://github.com/shlee811) |
| 이가연 |Backend (위치기반 지도 구현) [@leeeeegayeon](https://github.com/leeeeegayeon) |

<br>

## 🔀 협업 규칙

- 브랜치: `main` ← `dev` ← `feat/#이슈번호-기능명`
- 커밋: `feat:`, `fix:`, `chore:`, `refactor:` + 이슈 번호
- 이슈 → 브랜치 → PR(base: dev) → 머지 순서로 작업
