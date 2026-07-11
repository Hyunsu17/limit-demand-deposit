# limit-demand-deposit — CLAUDE.md

## 프로젝트 개요

은행 수신 도메인 학습을 위해 **한도요구불(입출금통장) 수신 도메인**을 직접 설계·구현하는 포트폴리오 프로젝트.

- 동시성 락 전략 비교 및 검증은 별도 레포에서 완료 → 검증된 전략을 이 프로젝트에 적용
- 목표: 실제 은행 상품 스펙을 분석하고, 금융 도메인 로직을 코드로 증명하는 것
- 비업무 영역(인증/인프라)은 Claude Code가 담당, 수신 도메인 로직은 직접 이해하며 진행

**커리어 Wiki 참조:**
- `/mnt/c/dev/career-wiki/wiki/index.md`
- `/mnt/c/dev/career-wiki/wiki/projects/한도요구불.md`
- `/mnt/c/dev/career-wiki/wiki/projects/한도요구불-ERD.md`

---

## 현재 상태

| 항목 | 내용 |
|------|------|
| **Week 0** | ✅ 완료 (2026-06-30) — 세팅, 인증, 스켈레톤 |
| **Week 1** | ✅ 완료 (2026-07-11) — 계좌개설 D1 검증 + TX1/TX2 배관 + 전 계층 테스트 (전체 스위트 51개 그린) |
| **다음 단계** | Week 2 (7/12~7/18): `AccountOpenServiceTest` APPROVED 경로 → @SpringBootTest 통합(D7-B 커밋시점 flush 예외타입 포함) + NCIS WireMock |
| **진행 방식 합의** | 인프라 테스트는 Week 3~4부터 하나씩 설명 → 함께 작성 → 리뷰 (일괄 작성 금지, 2026-07-11) |
| **데드라인** | 2026-09-26 |

---

## 기술 스택

| 분류 | 기술 / 버전 |
|------|------------|
| 언어 | Java 21 (Temurin) |
| 프레임워크 | Spring Boot **3.5.0** |
| 빌드 | Gradle **Kotlin DSL** (`build.gradle.kts`) |
| ORM | Spring Data JPA / `ddl-auto: validate` |
| DB | PostgreSQL 16 (Docker Compose) |
| 스키마 관리 | Flyway (`classpath:db/migration`) |
| 인증 | Spring Security + JWT (`jjwt` **0.12.6**) |
| 테스트 | JUnit 5 · Testcontainers BOM **1.20.4** · `@SpringBatchTest` |
| Mock 서버 | WireMock (타행 이체 등 외부 API 대체) |
| 부하 테스트 | k6 |
| 스케줄러 | `@Scheduled` (Quartz 아님 — 단일 인스턴스, 오버스펙) |
| 유틸 | Lombok |

---

## 아키텍처 — 경량 DDD

### 핵심 원칙

**계층형과의 결정적 차이: 비즈니스 로직은 도메인 객체가 소유한다.**

```java
// ❌ 금지 — Service/쿼리에 로직 집중 (공제회 방식)
account.setBalance(account.getBalance() - amount);

// ✅ 필수 — 도메인 객체가 검증 + 변경
account.withdraw(amount);  // Account 내부에서 잔액 검증 후 변경
```

- `Repository` interface는 `domain` 패키지에 위치 (도메인이 인프라에 의존하지 않음)
- 값 객체(VO): `Money`, `AccountStatus` 등은 domain 패키지
- Service는 도메인 객체를 조율하는 역할만

### Repository 패턴 — Port & Adapter (필수, 예외 없음)

**도메인은 프레임워크를 몰라야 한다.** `domain` 패키지에 Spring Data JPA(`JpaRepository` 등) import가 들어가면 안 된다. 파일 3개로 분리:

```java
// 1. domain/XxxRepository.java — 순수 인터페이스 ("포트"). Spring/JPA 어떤 것도 import 금지
public interface AccountOpenApplicationRepository {
    AccountOpenApplication save(AccountOpenApplication application);
    boolean existsByCustomerIdAndAppStatus(Long customerId, ApplicationStatus status);
}

// 2. infrastructure/XxxJpaRepository.java — Spring Data 기술 세부사항, package-private
interface AccountOpenApplicationJpaRepository extends JpaRepository<AccountOpenApplication, Long> {
    boolean existsByCustomerIdAndAppStatus(Long customerId, ApplicationStatus appStatus);
}

// 3. infrastructure/XxxRepositoryImpl.java — 포트를 구현하는 "어댑터", JPA에 위임만
@Repository
@RequiredArgsConstructor
public class AccountOpenApplicationRepositoryImpl implements AccountOpenApplicationRepository {
    private final AccountOpenApplicationJpaRepository jpaRepository;
    @Override
    public AccountOpenApplication save(AccountOpenApplication application) {
        return jpaRepository.save(application);
    }
    // ...
}
```

**이유**: JPA를 다른 구현(네이티브 쿼리, 외부 API 등)으로 교체해도 `domain`/`application`은 손대지 않는다. `AccountRepository`(Week2 전까지 `Account` 엔티티가 없어 구현 불가)처럼, 기술이 정해지지 않은 상태에서도 인터페이스만으로 상위 계층 개발이 가능해진다.

> 참고: `account` 패키지 전체가 이 컨벤션의 레퍼런스. 새 도메인 추가 시 그대로 따라간다.

### 패키지 구조

```
com.hyunsu.limitdeposit
├── account/
│   ├── domain/                       ← 애그리거트별 서브패키지 (2026-07-08 결정)
│   │   ├── Channel.java              ← 공용 VO — 여러 애그리거트가 참조하는 것만 루트
│   │   ├── account/                  ← 계좌 원장 (Account, AccountLedgerHistory, 포트 등)
│   │   ├── opening/                  ← 개설 신청 (AccountOpenApplication, ApplicationStatus 등)
│   │   └── ncis/                     ← NCIS 연동 (NcisCheckHistory, NcisClient 포트 등)
│   ├── application/
│   │   ├── AccountOpenService.java   ← 오케스트레이터 (TX1 → NCIS → TX2 분기)
│   │   └── dto/
│   └── infrastructure/               ← flat 유지 (AccountOpen*/Ncis* 네이밍으로 구분)
│       └── AccountRepositoryImpl.java
├── product/
│   ├── domain/ / infrastructure/     ← 기준정보 도메인 (customer와 동급, 2026-07-08 결정)
├── transaction/
│   ├── domain/ / application/ / infrastructure/
├── interest/
│   ├── domain/ / application/ / infrastructure/
├── customer/
│   ├── domain/ / application/ / infrastructure/
└── common/
    ├── auth/        ← Spring Security + JWT (Claude Code 담당)
    ├── exception/   ← ErrorCode, BusinessException, GlobalExceptionHandler
    └── config/      ← JpaConfig (@EnableJpaAuditing 분리)
```

> `Money` VO(금액 BigDecimal 래핑)는 Week 3 도입 예정 — 위치는 `account/domain/account/vo/` 예정

---

## 도메인 비즈니스 규칙

### 핵심 제약

- **1인 1계좌** — 전 금융기관 기준. 개설 전 NCIS 외부 API 조회 필수
- **입금 한도** — `MONTHLY_LIMT_LEDGER`로 월 누계 관리, 정책 테이블(`DEP_LMT_POLICY_MST`) 참조
- **지급 한도** — 연령대별 이체 한도 (`PYMT_LMT_POLICY_MST`)
- **정책 변경** — 변경 시 변경된 한도 기준 즉시 적용 (이력 관리 필수)

### 이자 계산 공식

```
일별 잔액 × 약정금리 × 1/365  (직전 이자지급일 다음날 ~ 결산일까지 합산)
금리 변경 시 → 변동일부터 변경 금리 적용 → 구간별 계산 필수
절사: 1원 미만 버림
```

이자지급: **매월 네번째 금요일 결산 → 다음날 지급**

### 금액 처리 원칙 (Fintech)

```java
// ❌ 절대 금지 — float/double 사용
double amount = 100.1 + 200.2;  // 300.30000000000004

// ✅ 필수 — BigDecimal
BigDecimal amount = new BigDecimal("100.1").add(new BigDecimal("200.2"));

// DB 컬럼: NUMERIC(19, 4)
```

### 해지 프로세스 (5단계)

1. 해지 가능 여부 검증 (지급제한·대기거래·연계상품 체크)
2. 최종 이자 정산 (금리 구간별 계산)
3. 이자 지급 처리
4. 외부 송금 + 원장 마감 (`ACCT_STATUS=2`)
5. 고객 통지

> ⚠️ 외부 송금 실패 시: 해지 중단 → 재시도 → 불가 시 보상 트랜잭션

### 거래 불변성

- `TRANS_RAW`: 전문 원본 보존, **절대 수정 금지**
- `TRANS_HISTORY`: 확정 거래, **절대 수정 금지**
- 오류 수정 = 반대 거래 추가 (Reversal), 기존 레코드 UPDATE 금지

---

## Flyway 규칙

- **SQL 파일은 개발자가 100% 직접 작성** (JPA `ddl-auto`처럼 자동 생성하지 않음)
- 파일 명명: `V{버전}__{설명}.sql` (언더스코어 **두 개**)
- **한 번 실행된 파일은 절대 수정 금지** (체크섬 불일치 에러)
- 수정이 필요하면 반드시 새 버전 파일 추가

```
db/migration/
├── V1__create_customer.sql         ← Week 0 (완료)
├── V2__create_account.sql          ← Phase 3
├── V3__create_transaction.sql      ← Phase 4
└── V4__create_interest.sql         ← Phase 5
```

---

## 역할 분담

**판단 기준: "이 코드가 면접에서 설명을 요구받을 알고리즘인가?" → Yes면 사용자, No면 Claude Code.**

| 영역 | 담당 | 이유 |
|------|------|------|
| 인증 / Spring Security + JWT / 공통 인프라 | Claude Code 90%+ | 비업무 영역 |
| 배관·오케스트레이션 (컨트롤러 와이어링, RepositoryImpl, DTO 변환, TX 경계, 단순 상태전이) | Claude Code 작성 → 사용자 **리뷰** | 반복 학습가치 낮음. 사용자는 "의도(D1~D10) 부합 + 버그 여부"를 리뷰로 검증 |
| 도메인 알고리즘 (이자 계산, 해지 정산, 한도 누적, 동시성) | 사용자 작성, Claude는 힌트/검증 | 면접에서 설명을 요구받는 핵심. 경계값·계산 로직은 직접 증명 |
| Entity 설계 / 패키지 구조 / 의사결정 | 합의 후 진행 | 되돌리기 어려운 결정 |

> **주석 규칙**: Claude가 작성한 주석은 `// [Claude]`로 표시하여 사용자 주석과 구분한다 (리뷰 소통용).

---

## 테스트 전략

| 영역 | 방식 | 비고 |
|------|------|------|
| 이자 계산 로직 | Mockito 단위 테스트 | 순수 계산, DB 불필요 |
| 계좌 개설 / 입출금 / 해지 | Testcontainers 통합 테스트 | TC JDBC URL 방식 |
| 이자 산출 배치 | `@SpringBatchTest` | Spring Batch 전용 슬라이스 |
| 동시 이체 정합성 | k6 시나리오 1개 (100 VU) | 재현 가능하게 스크립트 유지 |
| 인증 / 로그인 | 스킵 또는 최소화 | 비업무 영역 |

---

## 행동 원칙 — Karpathy 4원칙

### 1. Think Before Coding — 추측 말고 질문
- 요청이 모호하면 멈추고 가정을 드러낸다
- 여러 해석이 가능하면 임의로 고르지 않고 전부 제시한다
- 더 단순한 접근이 있으면 먼저 말한다

### 2. Simplicity First — 요청한 것만 만든다
- 요청한 것만 구현. 추측성 기능 추가 금지
- 단일 사용 코드에 추상화 금지
- self-check: "시니어가 보면 과하다고 할까?" → Yes면 다시

### 3. Surgical Changes — 요청한 곳만 건드린다
- 고쳐야 할 곳만 수정. 관련 없는 dead code 발견 시 언급만, 삭제 금지
- 내 변경으로 생긴 unused import/변수만 정리
- self-check: "변경된 모든 라인이 요청에서 직접 비롯됐는가?"

### 4. Goal-Driven Execution — 성공 기준을 먼저 정의한다
- 코딩 전에 "완료"가 무엇인지 정의
- "고쳐줘" → "이 테스트를 통과시켜라"로 전환
- 강한 성공 기준이 있으면 독립적으로 루프 가능

---

## 코딩 규칙

### 일반
- 메서드 하나의 역할은 하나
- 매직 넘버 금지 — `ErrorCode` enum 또는 상수로
- 주석은 "왜"를 설명 ("무엇"은 코드가 말함)
- 커밋 메시지: `[feat|fix|refactor|test|docs] 한 줄 설명`

### 경량 DDD
- 비즈니스 로직은 Service가 아닌 도메인 객체에
- Repository interface는 반드시 `domain` 패키지에
- `@Transactional(readOnly = true)` 조회 기본
- 트랜잭션 경계는 `application` Service에서만

### 금융 도메인
- 금액은 `BigDecimal` 필수, float/double 절대 금지
- 이자 계산은 단위 테스트로 모든 경계값 검증
- 거래 레코드(`TRANS_RAW`, `TRANS_HISTORY`) 수정 금지, Reversal로 보정

### DB / Flyway
- Entity 변경 시 반드시 새 Flyway 마이그레이션 파일 추가
- 실행된 마이그레이션 파일 수정 금지
- N+1 쿼리 발생 시 즉시 보고 후 수정 방향 제시

---

## 의사결정 기록 방법

구현 중 기술 결정이 생기면:
1. `career-wiki/wiki/decisions/YYYY-MM-DD-결정제목.md` 생성
2. `career-wiki/log.md`에 `[DECISION]` 태그로 기록
3. 아래 기준을 충족하면 **주요 결정 이력**에 한 줄 추가

### 기록 기준

> **"이 결정을 모르면 Claude가 잘못된 방향으로 코딩할 가능성이 있는가?"**

**기록해야 하는 것**
- 패키지 구조 / 테이블 구조 등 되돌리기 어려운 구조적 제약
- 기술 스택 교체 / 아키텍처 변경
- 도메인 규칙 해석 (특정 검증 로직이 어디에 있어야 하는지)

**기록하지 않아도 되는 것** → `decisions/*.md` + `log.md`로 충분
- 구현 로드맵, 단계별 계획
- 테스트 시나리오 세부 설계
- 각 기능의 세부 구현 방식

### 주요 결정 이력

| 날짜 | 결정 | 링크 |
|------|------|------|
| 2026-06-29 | 경량 DDD 채택 — 도메인 중심 패키지, Rich Domain Model | decisions/2026-06-29-한도요구불-아키텍처-패키지구조.md |
| 2026-06-29 | 기술 스택 확정 — Java 21, Gradle Kotlin DSL, @Scheduled, WireMock | decisions/2026-06-29-한도요구불-구현계획.md |
| 2026-06-30 | Week 0 세팅 완료 — Spring Boot 3.5.0 + JWT + Flyway V1, compileJava 통과 | decisions/2026-06-30-week0-세팅완료.md |
| 2026-07-04 | 역할분담 재정의 (배관=Claude+리뷰 / 알고리즘=직접) + Repository Port & Adapter 3파일 컨벤션 | decisions/2026-07-04-역할분담-재정의-repository-컨벤션.md |
| 2026-07-08 | `product` 최상위 패키지 신설 (account 하위 아님 — customer와 동급 기준정보 도메인) | decisions/2026-07-08-product-패키지-신설.md |
| 2026-07-08 | `account.domain`을 애그리거트별 서브패키지(account/opening/ncis)로 분리, Channel은 공용 VO로 루트 유지 | decisions/2026-07-08-domain-패키지-애그리거트-분리.md |
| 2026-07-08 | D7-B UNIQUE 충돌은 시스템오류 아닌 반려 — `DataIntegrityViolationException` → `REJECTED(3)` + `DUPLICATE_ACCOUNT`(409) | log.md 2026-07-08 [DECISION] |
| 2026-07-11 | 인프라 테스트 설계 기준 + @DataJpaTest 4종 세트(`Replace.NONE`+`@Import(어댑터, JpaConfig)`+test 프로파일) + 예외변환은 `saveAndFlush` 경유 + BigDecimal assert는 `isEqualByComparingTo` | decisions/2026-07-11-인프라테스트-testcontainers-컨벤션.md |

---

## 작업 시작 전 체크리스트

새 기능 구현 시:
- [ ] career-wiki에서 관련 맥락 확인 (한도요구불.md, 해당 decisions)
- [ ] 비즈니스 규칙 설계 문서 참조 (ERD, 해지 프로세스 등)
- [ ] 테스트 시나리오 먼저 정의 (무엇이 통과되면 완료인가)
- [ ] Entity 변경 시 Flyway 마이그레이션 파일 준비
- [ ] 도메인 로직 위치 확인 (Service가 아닌 도메인 객체에)
- [ ] 구조적 결정이 생기면 decisions/ 기록 후 주요 결정 이력 추가
