# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트

StockGraph: 주요 금융 지표(KOSPI, S&P500, USD/KRW, 금, BTC) 차트와 그 지표에 연관된 뉴스를 함께 보여주는 서비스. 포트폴리오용 프로젝트다. 요구사항, 결정사항, 단계별 계획은 **`project.md`가 기준**이다. 기술 선택의 이유는 `docs/adr/`에 남긴다.

- 모노레포: `backend/`(Spring Boot), `frontend/`(React), `infra/`(docker compose, nginx), `scripts/`(운영 스모크), `docs/`
- 저장소는 **public**이다. 시크릿은 환경변수로만 받는다(`.env.example` 참고). API 키를 코드나 설정 파일에 넣지 않는다.
- 문서와 커밋 메시지는 한국어로 쓴다. 커밋 제목은 Conventional Commits(`feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`) + 한국어 설명. 브랜치는 GitHub Flow(`main` + `feat/...`).

## 응답 언어

사용자에게 보이는 글은 모두 한국어로 쓴다. 최종 답변뿐 아니라 작업 중간의 진행 상황 설명, 질문, 오류 보고도 마찬가지다. 코드, 명령어, 파일 경로, 로그 원문, 오류 메시지 원문, 클래스·설정 이름은 번역하지 않고 그대로 쓴다.

## 토큰 절약 지침

**읽기·탐색**
- 위치를 모르면 먼저 Grep/Glob으로 찾고, 파일은 필요한 범위만 읽는다. 이미 읽은 파일은 다시 읽지 않는다.
- 이 문서와 `project.md`, `docs/adr/`에 정리된 내용은 코드를 다시 훑어서 확인하지 않는다.
- 서브에이전트는 요청받았을 때와 아래 "테스트" 규칙에서만 쓴다.

**명령 실행**
- 긴 출력은 필터해서 본다. Gradle은 `--console=plain 2>&1 | grep -E 'BUILD|FAILED|error:'`, 로그는 마지막 몇십 줄만 본다.
- 테스트는 고친 부분만 먼저 돌리고(`--tests ...`), 전체 테스트는 커밋이나 push 직전에 한 번만 돌린다.
- 같은 명령을 고치지 않은 채 반복 실행하지 않는다. 실패하면 원인을 먼저 본다.

**답변**
- 결론부터 짧게 쓴다. 이미 말한 내용을 다시 요약하지 않고, 하지 않을 선택지는 늘어놓지 않는다.

**환경 정보** (다시 찾지 않도록)
- 경로에 한글과 공백이 있다(`바탕 화면`). 셸 명령에서 항상 따옴표로 감싼다.
- Gradle에 `JAVA_HOME`이 없으면 `~/.jdks`의 JDK 21을 그 명령에만 지정한다. Bash: `export JAVA_HOME="$HOME/.jdks/jdk-21.0.13+9"`, PowerShell: `$env:JAVA_HOME="$HOME\.jdks\jdk-21.0.13+9"`
- `gh`는 로그인되어 있다. PATH에 없으면 `C:\Program Files\GitHub CLI\gh.exe`.
- 백엔드 통합 테스트는 Testcontainers라 Docker Desktop이 켜져 있어야 한다.

## 사용자와 역할 나누기

사용자는 3년차 주니어 개발자이고 이 프로젝트의 관리자(PM)다. 기본 개념은 따로 설명하지 않는다. 판단의 이유와 놓치기 쉬운 부분은 설명한다. 요구사항 확정은 사용자와 대화로 한다.

- **사용자에게 요청할 일**: 브라우저 로그인·승인, 화면 확인(사용자 테스트), 운영 서버 확인(SSH 결과 일부 붙여 달라고 요청), Docker Desktop·IDE 같은 로컬 환경 조작, 외부 API 키 발급, 디자인 견본 선택, 모호한 요구사항
- **직접 하는 일**: 코드 수정, 테스트 실행, git 작업(commit, push 포함), `gh`와 `curl`을 쓰는 읽기 전용 확인
- **요청하는 방법**: `! <명령>` 형태로 바로 실행할 수 있게 주고, 결과에서 무엇을 보면 되는지 한 줄로 알려준다.

## 명령어

```bash
# 저장소 루트
docker compose -f infra/docker-compose.local.yml up -d       # 로컬 Postgres(5432) + Redis(6379)

# backend/
./gradlew bootRun --args='--spring.profiles.active=local'    # 앱 실행, http://localhost:8080
./gradlew bootTestRun                                        # DB 없이 Testcontainers로 앱 실행
./gradlew test                                               # 전체 테스트 (Docker 필요)
./gradlew test --tests 'com.ar2723.stockgraph.SomeTest'      # 테스트 클래스 하나
./gradlew bootJar                                            # build/libs/*.jar

# frontend/  (Node 22.12+ 필요)
npm run dev                                                  # http://localhost:5173, /api 는 8080 으로 프록시
npm run lint                                                 # oxlint
npm test -- --run                                            # Vitest
npm run build                                                # tsc + vite build
```

## 구조와 규칙

- **백엔드** (`com.ar2723.stockgraph`): Spring Boot 4.1, Java 21. 기능별 패키지(`indicator`, `news`, `admin`, ...) 안에서 `controller → service → repository`. 공개 API는 `/api/**`, 관리자 API는 `/api/admin/**`.
- **스키마**는 Flyway(`backend/src/main/resources/db/migration`)만 바꾼다. `ddl-auto=validate`. 이미 적용된 `V*.sql`은 고치지 않고 새 `V{n}__*.sql`을 추가한다.
- **시간**: DB 저장은 UTC(`timestamptz`), 화면 표시는 KST. 지표 날짜(일봉)는 해당 시장의 현지 거래일 기준 `date`로 저장한다.
- **설정**: `application.yml`은 환경변수만 참조한다(기본값 없음). 로컬 기본값은 `application-local.yml`.
- **외부 API**: 무료 API만 쓴다(LLM 제외, ADR 참고). 호출부는 실패·한도 초과를 가정하고(재시도, 마지막 정상값 유지), 테스트는 실제 호출 대신 WireMock 등으로 대체한다.
- **뉴스 저장 원칙**: 제목, 링크, 언론사, 발행시각, 짧은 요약만 저장한다. 기사 본문 전체를 저장하거나 다시 배포하지 않는다.

## 테스트

- `test-runner` 에이전트(`.claude/agents/test-runner.md`)가 테스트를 맡는다. `.claude/hooks/after-git.js` 훅이 커밋 직후와 `main` push 직후 호출 시점을 알려준다. 훅 지시가 오면 다음 작업 전에 test-runner를 호출하고, 실패 보고가 오면 고친 뒤 다시 커밋한다.
- 백엔드 통합 테스트는 `TestcontainersConfiguration`을 `@Import` 한다. 이미지 태그는 `infra/` compose와 같게 유지한다(`postgres:17-alpine`, `redis:7.4-alpine`).
- 운영 스모크는 `scripts/smoke.sh`(읽기 전용).
