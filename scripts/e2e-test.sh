#!/usr/bin/env bash
set -Eeuo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
POLL_ATTEMPTS="${POLL_ATTEMPTS:-30}"
POLL_DELAY_SECONDS="${POLL_DELAY_SECONDS:-2}"

HTTP_STATUS=""
HTTP_BODY=""

log() {
  printf '\n[E2E] %s\n' "$1"
}

fail() {
  printf '\n[E2E][ERROR] %s\n' "$1" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 \
    || fail "Required command is missing: $1"
}

request() {
  local method="$1"
  local path="$2"
  local expected_status="$3"
  local body="${4:-}"
  local token="${5:-}"
  local response_file
  local status

  response_file="$(mktemp)"

  local -a args=(
    --silent
    --show-error
    --output "$response_file"
    --write-out "%{http_code}"
    --request "$method"
    --header "Accept: application/json"
  )

  if [[ -n "$token" ]]; then
    args+=(--header "Authorization: Bearer $token")
  fi

  if [[ -n "$body" ]]; then
    args+=(
      --header "Content-Type: application/json"
      --data "$body"
    )
  fi

  status="$(curl "${args[@]}" "${BASE_URL}${path}")"
  HTTP_BODY="$(cat "$response_file")"
  HTTP_STATUS="$status"
  rm -f "$response_file"

  if [[ "$HTTP_STATUS" != "$expected_status" ]]; then
    printf '\nRequest failed\n' >&2
    printf 'Method   : %s\n' "$method" >&2
    printf 'URL      : %s%s\n' "$BASE_URL" "$path" >&2
    printf 'Expected : %s\n' "$expected_status" >&2
    printf 'Actual   : %s\n' "$HTTP_STATUS" >&2
    printf 'Body     : %s\n' "$HTTP_BODY" >&2
    exit 1
  fi
}

json_get() {
  local json="$1"
  local path="$2"

  JSON_INPUT="$json" python3 - "$path" <<'PY'
import json
import os
import sys

data = json.loads(os.environ["JSON_INPUT"])

for part in sys.argv[1].split("."):
    if isinstance(data, list):
        data = data[int(part)]
    else:
        data = data[part]

if data is None:
    print("")
elif isinstance(data, bool):
    print(str(data).lower())
else:
    print(data)
PY
}

json_decimal_equals() {
  local json="$1"
  local path="$2"
  local expected="$3"

  JSON_INPUT="$json" python3 - "$path" "$expected" <<'PY'
from decimal import Decimal
import json
import os
import sys

data = json.loads(os.environ["JSON_INPUT"])
for part in sys.argv[1].split("."):
    data = data[int(part)] if isinstance(data, list) else data[part]

actual = Decimal(str(data))
expected = Decimal(sys.argv[2])
sys.exit(0 if actual == expected else 1)
PY
}

monthly_analytics_ready() {
  local json="$1"

  JSON_INPUT="$json" python3 - <<'PY'
from decimal import Decimal
import json
import os
import sys

data = json.loads(os.environ["JSON_INPUT"])
expense = Decimal(str(data.get("totalExpense", 0)))
count = int(data.get("transactionCount", 0))
sys.exit(0 if expense >= Decimal("150.00") and count >= 1 else 1)
PY
}

notification_exists() {
  local json="$1"
  local budget_id="$2"
  local category_id="$3"

  JSON_INPUT="$json" python3 - "$budget_id" "$category_id" <<'PY'
import json
import os
import sys

data = json.loads(os.environ["JSON_INPUT"])
budget_id = int(sys.argv[1])
category_id = int(sys.argv[2])

found = any(
    item.get("type") == "BUDGET_EXCEEDED"
    and item.get("budgetId") == budget_id
    and item.get("categoryId") == category_id
    for item in data.get("content", [])
)

sys.exit(0 if found else 1)
PY
}

transaction_page_contains() {
  local json="$1"
  local transaction_id="$2"
  local account_id="$3"
  local category_id="$4"

  JSON_INPUT="$json" python3 - \
    "$transaction_id" \
    "$account_id" \
    "$category_id" <<'PY'
import json
import os
import sys

data = json.loads(os.environ["JSON_INPUT"])

transaction_id = int(sys.argv[1])
account_id = int(sys.argv[2])
category_id = int(sys.argv[3])

content = data.get("content", [])

transaction_found = any(
    item.get("id") == transaction_id
    and item.get("accountId") == account_id
    and item.get("categoryId") == category_id
    and item.get("type") == "EXPENSE"
    for item in content
)

pagination_valid = (
    data.get("page") == 0
    and data.get("size") == 10
    and data.get("totalElements", 0) >= 1
)

sys.exit(
    0 if transaction_found and pagination_valid else 1
)
PY
}


require_command curl
require_command python3

YEAR="$(date -u +%Y)"
MONTH_PADDED="$(date -u +%m)"
MONTH="$((10#$MONTH_PADDED))"
TRANSACTION_DATE="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
RUN_SUFFIX="${GITHUB_RUN_ID:-local}-$(date +%s)-${RANDOM}"

PRIMARY_EMAIL="e2e-primary-${RUN_SUFFIX}@smartwallet.test"
SECONDARY_EMAIL="e2e-secondary-${RUN_SUFFIX}@smartwallet.test"
PASSWORD="E2ePassword123!"

log "Registering primary user"
request POST "/api/auth/register" 201 "{
  \"firstName\": \"E2E\",
  \"lastName\": \"Primary\",
  \"email\": \"${PRIMARY_EMAIL}\",
  \"password\": \"${PASSWORD}\"
}"
PRIMARY_USER_ID="$(json_get "$HTTP_BODY" "id")"
echo "Primary user id: ${PRIMARY_USER_ID}"

log "Logging in primary user"
request POST "/api/auth/login" 200 "{
  \"email\": \"${PRIMARY_EMAIL}\",
  \"password\": \"${PASSWORD}\"
}"
PRIMARY_TOKEN="$(json_get "$HTTP_BODY" "accessToken")"
[[ -n "$PRIMARY_TOKEN" ]] || fail "Primary access token is empty"

log "Creating checking account"
request POST "/api/accounts" 201 "{
  \"name\": \"E2E Main Account\",
  \"type\": \"CHECKING\",
  \"currency\": \"TRY\",
  \"initialBalance\": 1000.00
}" "$PRIMARY_TOKEN"
ACCOUNT_ID="$(json_get "$HTTP_BODY" "id")"
echo "Account id: ${ACCOUNT_ID}"

log "Creating destination cash account"

request POST "/api/accounts" 201 "{
  \"name\": \"E2E Cash Account ${RUN_SUFFIX}\",
  \"type\": \"CASH\",
  \"currency\": \"TRY\",
  \"initialBalance\": 200.00
}" "$PRIMARY_TOKEN"

DESTINATION_ACCOUNT_ID="$(json_get "$HTTP_BODY" "id")"

[[ -n "$DESTINATION_ACCOUNT_ID" ]] \
  || fail "Destination account id is empty"

echo "Destination account id: ${DESTINATION_ACCOUNT_ID}"

log "Creating expense category"
request POST "/api/categories" 201 "{
  \"name\": \"E2E Food ${RUN_SUFFIX}\",
  \"type\": \"EXPENSE\"
}" "$PRIMARY_TOKEN"
CATEGORY_ID="$(json_get "$HTTP_BODY" "id")"
echo "Category id: ${CATEGORY_ID}"

log "Registering secondary user for ownership check"
request POST "/api/auth/register" 201 "{
  \"firstName\": \"E2E\",
  \"lastName\": \"Secondary\",
  \"email\": \"${SECONDARY_EMAIL}\",
  \"password\": \"${PASSWORD}\"
}"

log "Logging in secondary user"
request POST "/api/auth/login" 200 "{
  \"email\": \"${SECONDARY_EMAIL}\",
  \"password\": \"${PASSWORD}\"
}"
SECONDARY_TOKEN="$(json_get "$HTTP_BODY" "accessToken")"
[[ -n "$SECONDARY_TOKEN" ]] || fail "Secondary access token is empty"

log "Verifying that another user cannot use the primary user's category"
request POST "/api/budgets" 400 "{
  \"categoryId\": ${CATEGORY_ID},
  \"limitAmount\": 100.00,
  \"year\": ${YEAR},
  \"month\": ${MONTH}
}" "$SECONDARY_TOKEN"
echo "Foreign category correctly rejected"

log "Creating budget with the category owner"
request POST "/api/budgets" 201 "{
  \"categoryId\": ${CATEGORY_ID},
  \"limitAmount\": 100.00,
  \"year\": ${YEAR},
  \"month\": ${MONTH}
}" "$PRIMARY_TOKEN"
BUDGET_ID="$(json_get "$HTTP_BODY" "id")"
INITIAL_BUDGET_STATUS="$(json_get "$HTTP_BODY" "status")"
[[ "$INITIAL_BUDGET_STATUS" == "ACTIVE" ]] \
  || fail "New budget should be ACTIVE, actual: ${INITIAL_BUDGET_STATUS}"
echo "Budget id: ${BUDGET_ID}"

log "Creating an expense that exceeds the budget"
request POST "/api/transactions" 201 "{
  \"accountId\": ${ACCOUNT_ID},
  \"categoryId\": ${CATEGORY_ID},
  \"type\": \"EXPENSE\",
  \"amount\": 150.00,
  \"description\": \"SmartWallet CI end-to-end expense\",
  \"transactionDate\": \"${TRANSACTION_DATE}\"
}" "$PRIMARY_TOKEN"
TRANSACTION_ID="$(json_get "$HTTP_BODY" "id")"
echo "Transaction id: ${TRANSACTION_ID}"

log "Verifying transaction filtering and pagination"

request GET \
  "/api/transactions?type=EXPENSE&accountId=${ACCOUNT_ID}&categoryId=${CATEGORY_ID}&page=0&size=10" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

transaction_page_contains \
  "$HTTP_BODY" \
  "$TRANSACTION_ID" \
  "$ACCOUNT_ID" \
  "$CATEGORY_ID" \
  || fail "Filtered transaction page is incorrect: ${HTTP_BODY}"

echo "Transaction filtering and pagination are correct"

log "Verifying invalid transaction date range rejection"

request GET \
  "/api/transactions?startDate=2026-08-01T00:00:00Z&endDate=2026-07-01T00:00:00Z&page=0&size=10" \
  400 \
  "" \
  "$PRIMARY_TOKEN"

echo "Invalid transaction date range correctly rejected"

log "Verifying synchronous account balance update"
request GET "/api/accounts/${ACCOUNT_ID}" 200 "" "$PRIMARY_TOKEN"
json_decimal_equals "$HTTP_BODY" "balance" "850.00" \
  || fail "Expected account balance 850.00, response: ${HTTP_BODY}"
echo "Account balance is correct"

log "Transferring money between owned accounts"

request POST "/api/transfers" 201 "{
  \"fromAccountId\": ${ACCOUNT_ID},
  \"toAccountId\": ${DESTINATION_ACCOUNT_ID},
  \"amount\": 300.00,
  \"description\": \"SmartWallet CI account transfer\"
}" "$PRIMARY_TOKEN"

TRANSFER_ID="$(json_get "$HTTP_BODY" "id")"
TRANSFER_FROM_ACCOUNT_ID="$(json_get "$HTTP_BODY" "fromAccountId")"
TRANSFER_TO_ACCOUNT_ID="$(json_get "$HTTP_BODY" "toAccountId")"

[[ -n "$TRANSFER_ID" ]] \
  || fail "Transfer id is empty"

[[ "$TRANSFER_FROM_ACCOUNT_ID" == "$ACCOUNT_ID" ]] \
  || fail "Transfer source account is incorrect: ${HTTP_BODY}"

[[ "$TRANSFER_TO_ACCOUNT_ID" == "$DESTINATION_ACCOUNT_ID" ]] \
  || fail "Transfer destination account is incorrect: ${HTTP_BODY}"

json_decimal_equals \
  "$HTTP_BODY" \
  "amount" \
  "300.00" \
  || fail "Transfer amount is incorrect: ${HTTP_BODY}"

echo "Transfer id: ${TRANSFER_ID}"

log "Verifying account balances after transfer"

request GET \
  "/api/accounts/${ACCOUNT_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_decimal_equals \
  "$HTTP_BODY" \
  "balance" \
  "550.00" \
  || fail "Source account balance is incorrect after transfer: ${HTTP_BODY}"

request GET \
  "/api/accounts/${DESTINATION_ACCOUNT_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_decimal_equals \
  "$HTTP_BODY" \
  "balance" \
  "500.00" \
  || fail "Destination account balance is incorrect after transfer: ${HTTP_BODY}"

echo "Transfer balances are correct"

log "Verifying insufficient balance rejection"

request POST "/api/transfers" 409 "{
  \"fromAccountId\": ${ACCOUNT_ID},
  \"toAccountId\": ${DESTINATION_ACCOUNT_ID},
  \"amount\": 10000.00,
  \"description\": \"This transfer must fail\"
}" "$PRIMARY_TOKEN"

echo "Insufficient balance correctly rejected"

log "Verifying balances remained unchanged after rejected transfer"

request GET \
  "/api/accounts/${ACCOUNT_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_decimal_equals \
  "$HTTP_BODY" \
  "balance" \
  "550.00" \
  || fail "Source balance changed after rejected transfer: ${HTTP_BODY}"

request GET \
  "/api/accounts/${DESTINATION_ACCOUNT_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_decimal_equals \
  "$HTTP_BODY" \
  "balance" \
  "500.00" \
  || fail "Destination balance changed after rejected transfer: ${HTTP_BODY}"

echo "Rejected transfer did not modify balances"

log "Waiting for Budget Service to consume the transaction event"
BUDGET_READY=false
for ((attempt = 1; attempt <= POLL_ATTEMPTS; attempt++)); do
  request GET "/api/budgets/${BUDGET_ID}" 200 "" "$PRIMARY_TOKEN"

  budget_status="$(json_get "$HTTP_BODY" "status")"
  spent_amount="$(json_get "$HTTP_BODY" "spentAmount")"

  echo "Budget poll ${attempt}/${POLL_ATTEMPTS}: status=${budget_status}, spent=${spent_amount}"

  if [[ "$budget_status" == "EXCEEDED" ]] \
      && json_decimal_equals "$HTTP_BODY" "spentAmount" "150.00"; then
    BUDGET_READY=true
    break
  fi

  sleep "$POLL_DELAY_SECONDS"
done

[[ "$BUDGET_READY" == "true" ]] \
  || fail "Budget did not become EXCEEDED in time"

log "Waiting for Notification Service to consume the budget event"
NOTIFICATION_READY=false
for ((attempt = 1; attempt <= POLL_ATTEMPTS; attempt++)); do
  request GET "/api/notifications?unreadOnly=true&page=0&size=20" 200 "" "$PRIMARY_TOKEN"

  echo "Notification poll ${attempt}/${POLL_ATTEMPTS}"

  if notification_exists "$HTTP_BODY" "$BUDGET_ID" "$CATEGORY_ID"; then
    NOTIFICATION_READY=true
    break
  fi

  sleep "$POLL_DELAY_SECONDS"
done

[[ "$NOTIFICATION_READY" == "true" ]] \
  || fail "BUDGET_EXCEEDED notification was not created in time"

log "Waiting for Analytics Service to consume the transaction event"
ANALYTICS_READY=false
for ((attempt = 1; attempt <= POLL_ATTEMPTS; attempt++)); do
  request GET "/api/analytics/monthly?year=${YEAR}&month=${MONTH}" 200 "" "$PRIMARY_TOKEN"

  total_expense="$(json_get "$HTTP_BODY" "totalExpense")"
  transaction_count="$(json_get "$HTTP_BODY" "transactionCount")"
  echo "Analytics poll ${attempt}/${POLL_ATTEMPTS}: expense=${total_expense}, count=${transaction_count}"

  if monthly_analytics_ready "$HTTP_BODY"; then
    ANALYTICS_READY=true
    break
  fi

  sleep "$POLL_DELAY_SECONDS"
done

[[ "$ANALYTICS_READY" == "true" ]] \
  || fail "Analytics projection was not updated in time"

log "End-to-end flow passed"
echo "Primary user id : ${PRIMARY_USER_ID}"
echo "Account id      : ${ACCOUNT_ID}"
echo "Category id     : ${CATEGORY_ID}"
echo "Budget id       : ${BUDGET_ID}"
echo "Transaction id  : ${TRANSACTION_ID}"