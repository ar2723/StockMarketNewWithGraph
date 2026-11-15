-- MVP 지표 5개. 소스가 확정되지 않은 지표는 source 를 비우고 비활성으로 둔다(Phase 2 PoC 에서 채움).
INSERT INTO indicator (code, name, category, currency, source, source_symbol, enabled, display_order) VALUES
    ('KOSPI',  'KOSPI',    'INDEX',     'KRW', NULL,    NULL,      FALSE, 1),
    ('SPX',    'S&P 500',  'INDEX',     'USD', NULL,    NULL,      FALSE, 2),
    ('USDKRW', 'USD/KRW',  'FX',        'KRW', NULL,    NULL,      FALSE, 3),
    ('GOLD',   '금',        'COMMODITY', 'USD', NULL,    NULL,      FALSE, 4),
    ('BTC',    '비트코인',  'CRYPTO',    'KRW', 'UPBIT', 'KRW-BTC', TRUE,  5);
