# StockGraph

주요 금융 지표(KOSPI, S&P500, USD/KRW, 금, BTC) 차트와, 그 지표가 움직인 시점의 관련 뉴스를 함께 보여주는 서비스입니다.

> 개발 중입니다. 요구사항과 계획은 [project.md](project.md), 기술 결정은 [docs/adr](docs/adr)에 있습니다.

## 구조

```
backend/   Spring Boot 4.1 (Java 21) REST API, 수집 배치
frontend/  React 19 + Vite + TypeScript
infra/     docker compose, nginx 설정
scripts/   운영 스모크 테스트
docs/      ADR, 트러블슈팅 기록
```

## 로컬 실행

```bash
docker compose -f infra/docker-compose.local.yml up -d      # Postgres + Redis
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'
curl http://localhost:8080/actuator/health

cd frontend && npm install && npm run dev                   # http://localhost:5173 (/api, /actuator 는 8080 으로 프록시)
```

필요한 것: JDK 21, Docker, Node 22.12+
