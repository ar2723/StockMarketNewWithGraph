# ERD

스키마의 기준은 Flyway 마이그레이션(`backend/src/main/resources/db/migration`)이다. 이 문서는 설계 의도를 설명한다.
시각 컬럼은 모두 `timestamptz`(UTC 저장), 거래일은 `date`(지표별 기준 시간대의 날짜)다.

```mermaid
erDiagram
    indicator ||--o{ indicator_price_hourly : "원천 시간봉"
    indicator ||--o{ indicator_price_daily : "일봉"
    indicator ||--o{ batch_job_run : "수집 이력"
    indicator ||--o{ indicator_keyword : "Sprint 3"
    indicator ||--o{ news_indicator : "Sprint 3"
    news ||--o{ news_indicator : "Sprint 3"

    indicator {
        bigint id PK
        varchar code UK "BTC, KOSPI, SPX, USDKRW, GOLD"
        varchar name
        varchar category "INDEX | FX | COMMODITY | CRYPTO"
        varchar currency "KRW, USD"
        varchar source "UPBIT ... (미정이면 null)"
        varchar source_symbol "KRW-BTC"
        boolean enabled "수집 on/off (관리자)"
        numeric spike_threshold_pct "급변 기준, 기본 2.00"
        int display_order
        timestamptz created_at
        timestamptz updated_at
    }
    indicator_price_hourly {
        bigint indicator_id PK,FK
        timestamptz candle_start PK "시간봉 시작 시각"
        numeric open_price
        numeric high_price
        numeric low_price
        numeric close_price
        numeric volume
        timestamptz collected_at
    }
    indicator_price_daily {
        bigint indicator_id PK,FK
        date trade_date PK "BTC 는 KST 날짜"
        numeric open_price
        numeric high_price
        numeric low_price
        numeric close_price
        numeric volume
        numeric change_rate "전일 종가 대비 비율 (0.0213 = +2.13%)"
        boolean complete "그날이 끝났는지 (오늘은 false)"
        timestamptz collected_at
    }
    batch_job_run {
        bigint id PK
        varchar job_name
        bigint indicator_id FK "nullable"
        varchar status "RUNNING | SUCCESS | FAILED"
        int items_processed
        text message "실패 원인"
        timestamptz started_at
        timestamptz finished_at
    }
    news {
        bigint id PK
        varchar url UK
        varchar title
        varchar publisher
        text summary "발췌/요약만, 본문 저장 안 함"
        timestamptz published_at
        varchar source "NAVER, RSS ..."
    }
    news_indicator {
        bigint news_id PK,FK
        bigint indicator_id PK,FK
        numeric relevance_score
        varchar sentiment "POSITIVE | NEGATIVE | NEUTRAL"
        varchar method "KEYWORD | LLM"
        boolean hidden "관리자가 연관성 해제"
    }
    indicator_keyword {
        bigint id PK
        bigint indicator_id FK
        varchar keyword
        numeric weight
    }
```

## 설계 메모

- **시간봉을 따로 저장하는 이유**: 업비트 일봉은 UTC 00시(KST 09시) 경계라서, KST 00시 기준 일봉은 60분봉을 집계해 만든다(`docs/poc/data-sources.md`). 원천 시간봉을 남겨 두면 집계 로직을 고쳤을 때 외부 API를 다시 부르지 않고 재계산할 수 있고, 나중에 BTC 시간 단위 차트에도 쓸 수 있다. 3년치 약 26,300행이라 부담이 작다.
- **일봉 `complete`**: 오늘(KST) 일봉은 매시간 갱신되는 미완성 값이다. 급변일 판정과 뉴스 연관성 계산은 `complete = true` 인 날만 대상으로 한다.
- **`change_rate`는 비율**로 저장한다(업비트 응답과 같은 형식). 급변 판정은 `abs(change_rate) * 100 >= spike_threshold_pct`.
- **키 설계**: 가격 테이블은 `(indicator_id, 시각/날짜)` 복합 PK라 같은 데이터를 다시 받아도 `ON CONFLICT DO UPDATE`로 덮어쓴다(수집 재실행이 안전함).
- 뉴스 관련 테이블(`news`, `news_indicator`, `indicator_keyword`)은 Sprint 3 에서 마이그레이션을 추가한다.
