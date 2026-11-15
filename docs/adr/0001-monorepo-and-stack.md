# ADR 0001: 모노레포와 기본 기술 스택

- 상태: 채택 (2026-11-15)

## 맥락
1인 개발 포트폴리오 프로젝트다. 백엔드(Spring Boot)와 프론트(React)를 분리한 REST 구조로 만들되, 같은 미니 PC 한 대에 배포한다.

## 결정
- 저장소 하나에 `backend/`, `frontend/`, `infra/`를 둔다(모노레포).
- 백엔드: Spring Boot 4.1 + Java 21(LTS), Gradle 9. Postgres 17 + Flyway, Redis 7.4.
- 프론트: React 19 + Vite + TypeScript.
- 외부 금융 데이터는 무료 API만 쓴다. 단, 뉴스 연관성 2단계의 LLM API는 유료를 허용한다.

## 이유
- 모노레포: API 계약 변경과 화면 변경을 한 PR에서 같이 리뷰·테스트할 수 있고, CI/CD와 에이전트(test-runner) 설정을 한 곳에서 관리한다. 1인 개발이라 저장소를 나눠 얻는 이점(팀별 권한, 독립 릴리스)이 작다.
- Java 21: Bookmate 운영 서버와 로컬에 이미 있고, Boot 4의 최소 요구(17)를 넘는 LTS다.
- Flyway: Bookmate에서 스키마 불일치로 고생한 경험이 있어, 스키마는 처음부터 마이그레이션으로만 바꾼다.

## 결과
- CI는 경로 필터(`backend/**`, `frontend/**`)로 필요한 쪽만 빌드한다.
- 로컬 Node는 22.12 이상이어야 한다(Vite 8 요구사항).
