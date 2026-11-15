#!/usr/bin/env bash
# 운영 스모크 테스트 (읽기 전용: 데이터를 만들거나 바꾸는 요청은 보내지 않는다)
# Bookmate(scripts/smoke.sh) 구조를 가져왔다. 화면/API 가 늘어나면 2번 목록에 추가한다.
#
#   BASE_URL=https://<도메인> scripts/smoke.sh          # 전체 점검
#   BASE_URL=https://<도메인> scripts/smoke.sh --quick  # 기동 확인(health UP)만. 문서만 바뀐 배포용
#
# 출력: 실패한 항목만 한 줄씩, 마지막에 "스모크: 통과 N/M" 또는 "스모크: 실패 K/M".
# 종료 코드: 0 통과, 1 점검 실패, 2 앱이 뜨지 않음(502 등), 3 연결 불가(네트워크 문제 추정), 4 사용법 오류

# TODO(Sprint 1): 운영 도메인이 정해지면 기본값으로 넣는다.
BASE_URL="${BASE_URL:-}"
[ -z "$BASE_URL" ] && { echo "스모크: BASE_URL 이 없다. 예) BASE_URL=https://example.p-e.kr $0"; exit 4; }
HOST="${BASE_URL#https://}"; HOST="${HOST%%/*}"
WAIT_TRIES="${WAIT_TRIES:-12}"   # 기동 대기: 10초 간격
QUICK=0; [ "$1" = "--quick" ] && QUICK=1
HEALTH="$BASE_URL/actuator/health"

pass=0; fail=0
ok()  { pass=$((pass+1)); }
bad() { fail=$((fail+1)); echo "FAIL  $1  (기대: $2, 실제: $3)"; }

# 상태코드와 Location 을 "코드 Location" 형태로 돌려준다. 연결 실패면 000.
req() {  # req METHOD URL
	if [ "$1" = HEAD ]; then
		curl -sI -o /dev/null --max-time 10 -w '%{http_code} %{redirect_url}' "$2"
	else
		curl -s -o /dev/null --max-time 10 -X "$1" -w '%{http_code} %{redirect_url}' "$2"
	fi
}

expect_code() {  # expect_code METHOD PATH CODE
	local r; r=$(req "$1" "$BASE_URL$2"); r="${r%% *}"
	[ "$r" = "$3" ] && ok || bad "$1 $2" "$3" "$r"
}

expect_redirect() {  # expect_redirect METHOD URL CODE LOCATION_PREFIX
	local r code loc; r=$(req "$1" "$2"); code="${r%% *}"; loc="${r#* }"
	if [ "$code" = "$3" ] && [ "${loc#"$4"}" != "$loc" ]; then ok; else bad "$1 $2" "$3 -> $4*" "$code -> $loc"; fi
}

# 1. 기동 대기: 백엔드 health 가 UP 인지 (nginx -> frontend -> backend 경로 전체를 지난다)
body=""; code=000
for i in $(seq 1 "$WAIT_TRIES"); do
	body=$(curl -s --max-time 10 -w '\n%{http_code}' "$HEALTH"); code="${body##*$'\n'}"
	[ "$code" = 200 ] && echo "$body" | grep -q '"status":"UP"' && break
	[ "$i" -lt "$WAIT_TRIES" ] && sleep 10
done
if [ "$code" != 200 ]; then
	if [ "$code" = 000 ]; then
		echo "스모크: 연결 불가 ($BASE_URL). 앱이 아니라 네트워크/공유기 문제일 가능성이 높다."; exit 3
	fi
	echo "스모크: 앱 미기동 (/actuator/health $code, ${WAIT_TRIES}회 확인). 서버 로그 확인 필요: ssh hannoon 'docker compose -p stockgraph logs --tail 80 backend'"; exit 2
fi
[ "$i" -gt 1 ] && echo "INFO  기동 대기 $(( (i-1)*10 ))초 후 health UP"

if [ "$QUICK" = 1 ]; then echo "스모크(quick): 통과 1/1"; exit 0; fi

# 2. 공개 화면/API: GET 200 (화면은 SPA 라 index.html 이 내려온다)
for p in / ; do
	expect_code GET "$p" 200
	expect_code HEAD "$p" 200
done
# TODO(Sprint 1~): /api/indicators 등 공개 API 추가

# 3. 노출되면 안 되는 것: actuator 의 health/info 외 엔드포인트
expect_code GET /actuator/env 404

# 4. http -> https
expect_redirect GET "http://$HOST/" 301 "https://$HOST/"

# 5. 인증서 만료 30일 이내 경고 (실패로 세지 않음)
if command -v openssl >/dev/null; then
	if ! echo | openssl s_client -servername "$HOST" -connect "$HOST:443" 2>/dev/null | openssl x509 -noout -checkend $((30*86400)) >/dev/null 2>&1; then
		end=$(echo | openssl s_client -servername "$HOST" -connect "$HOST:443" 2>/dev/null | openssl x509 -noout -enddate 2>/dev/null | cut -d= -f2)
		echo "WARN  인증서 만료 임박 또는 확인 불가: ${end:-알 수 없음}"
	fi
fi

total=$((pass+fail))
if [ "$fail" = 0 ]; then echo "스모크: 통과 $pass/$total"; exit 0; fi
echo "스모크: 실패 $fail/$total"; exit 1
