-- BTC 는 하루 변동이 커서 ±2%면 급변일이 24.7%(4일 중 1일). ±4%(6.1%, 3년간 67일)로 올린다. docs/poc/data-sources.md 참고
UPDATE indicator SET spike_threshold_pct = 4.00, updated_at = now() WHERE code = 'BTC';
