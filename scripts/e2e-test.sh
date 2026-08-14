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
  local idempotency_key="${6:-}"
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

  if [[ -n "$idempotency_key" ]]; then
    args+=(
      --header "Idempotency-Key: $idempotency_key"
    )
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

monthly_analytics_matches() {
  local json="$1"
  local expected_currency="$2"
  local expected_expense="$3"
  local expected_count="$4"

  JSON_INPUT="$json" python3 - \
    "$expected_currency" \
    "$expected_expense" \
    "$expected_count" <<'PY'
from decimal import Decimal
import json
import os
import sys

data = json.loads(os.environ["JSON_INPUT"])
expected_currency = sys.argv[1]
expected_expense = Decimal(sys.argv[2])
expected_count = int(sys.argv[3])

currency = data.get("currency")
expense = Decimal(str(data.get("totalExpense", 0)))
count = int(data.get("transactionCount", 0))

valid = (
    currency == expected_currency
    and expense == expected_expense
    and count == expected_count
)

sys.exit(0 if valid else 1)
PY
}

monthly_category_analytics_matches() {
  local json="$1"
  local expected_currency="$2"
  local expected_category_id="$3"
  local expected_expense="$4"

  JSON_INPUT="$json" python3 - \
    "$expected_currency" \
    "$expected_category_id" \
    "$expected_expense" <<'PY'
from decimal import Decimal
import json
import os
import sys

data = json.loads(os.environ["JSON_INPUT"])
expected_currency = sys.argv[1]
expected_category_id = int(sys.argv[2])
expected_expense = Decimal(sys.argv[3])

if data.get("currency") != expected_currency:
    sys.exit(1)

if Decimal(str(data.get("totalExpense", 0))) != expected_expense:
    sys.exit(1)

categories = data.get("categories", [])
found = any(
    isinstance(item, dict)
    and item.get("categoryId") == expected_category_id
    and Decimal(str(item.get("totalExpense", 0))) == expected_expense
    for item in categories
)

sys.exit(0 if found else 1)
PY
}

monthly_trend_matches() {
  local json="$1"
  local expected_currency="$2"
  local expected_year="$3"
  local expected_month="$4"
  local expected_expense="$5"

  JSON_INPUT="$json" python3 - \
    "$expected_currency" \
    "$expected_year" \
    "$expected_month" \
    "$expected_expense" <<'PY'
from decimal import Decimal
import json
import os
import sys

data = json.loads(os.environ["JSON_INPUT"])

expected_currency = sys.argv[1]
expected_year = int(sys.argv[2])
expected_month = int(sys.argv[3])
expected_expense = Decimal(sys.argv[4])

if data.get("currency") != expected_currency:
    sys.exit(1)

months = data.get("months", [])

found = any(
    isinstance(item, dict)
    and item.get("year") == expected_year
    and item.get("month") == expected_month
    and Decimal(str(item.get("totalExpense", 0))) == expected_expense
    for item in months
)

sys.exit(0 if found else 1)
PY
}

daily_cash_flow_matches() {
  local json="$1"
  local expected_currency="$2"
  local expected_day="$3"
  local expected_expense="$4"

  JSON_INPUT="$json" python3 - \
    "$expected_currency" \
    "$expected_day" \
    "$expected_expense" <<'PY'
from decimal import Decimal
import json
import os
import sys

data = json.loads(os.environ["JSON_INPUT"])

expected_currency = sys.argv[1]
expected_day = int(sys.argv[2])
expected_expense = Decimal(sys.argv[3])

# Currency artık days[] item'larında değil,
# response'un top-level alanında.
if data.get("currency") != expected_currency:
    sys.exit(1)

# Response toplamını kontrol et.
if Decimal(str(data.get("totalExpense", 0))) != expected_expense:
    sys.exit(1)

days = data.get("days", [])

# İlgili günün expense değerini kontrol et.
found = any(
    isinstance(item, dict)
    and item.get("day") == expected_day
    and Decimal(str(item.get("totalExpense", 0))) == expected_expense
    for item in days
)

sys.exit(0 if found else 1)
PY
}

notification_exists() {
  local json="$1"
  local budget_id="$2"

  JSON_INPUT="$json" python3 - "$budget_id" <<'PY'
import json
import os
import sys

data = json.loads(os.environ["JSON_INPUT"])
budget_id = int(sys.argv[1])

found = any(
    item.get("type") == "BUDGET_EXCEEDED"
    and item.get("resourceType") == "BUDGET"
    and item.get("resourceId") == budget_id
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

transfer_page_contains() {
  local json="$1"
  local transfer_id="$2"
  local from_account_id="$3"
  local to_account_id="$4"
  local expected_size="$5"

  JSON_INPUT="$json" python3 - \
    "$transfer_id" \
    "$from_account_id" \
    "$to_account_id" \
    "$expected_size" <<'PY'
import json
import os
import sys

data = json.loads(os.environ["JSON_INPUT"])

transfer_id = int(sys.argv[1])
from_account_id = int(sys.argv[2])
to_account_id = int(sys.argv[3])
expected_size = int(sys.argv[4])

content = data.get("content", [])

transfer_found = any(
    item.get("id") == transfer_id
    and item.get("fromAccountId") == from_account_id
    and item.get("toAccountId") == to_account_id
    and str(item.get("amount")) in ("300.0", "300.00")
    for item in content
)

pagination_valid = (
    data.get("page") == 0
    and data.get("size") == expected_size
    and data.get("totalElements", 0) >= 1
)

sys.exit(
    0 if transfer_found and pagination_valid else 1
)
PY
}

json_collection_does_not_contain_id() {
  local json="$1"
  local unexpected_id="$2"

  JSON_INPUT="$json" python3 - "$unexpected_id" <<'PY'
import json
import os
import sys

data = json.loads(os.environ["JSON_INPUT"])
unexpected_id = int(sys.argv[1])

if isinstance(data, list):
    content = data
elif isinstance(data, dict):
    content = data.get("content", [])
else:
    sys.exit(1)

contains_id = any(
    item.get("id") == unexpected_id
    for item in content
    if isinstance(item, dict)
)

sys.exit(1 if contains_id else 0)
PY
}

json_collection_contains_id() {
  local json="$1"
  local expected_id="$2"

  JSON_INPUT="$json" python3 - "$expected_id" <<'PY'
import json
import os
import sys

data = json.loads(os.environ["JSON_INPUT"])
expected_id = int(sys.argv[1])

if isinstance(data, list):
    content = data
elif isinstance(data, dict):
    content = data.get("content", [])
else:
    sys.exit(1)

contains_id = any(
    isinstance(item, dict)
    and item.get("id") == expected_id
    for item in content
)

sys.exit(0 if contains_id else 1)
PY
}

recurring_transaction_exists() {
  local json="$1"
  local expected_description="$2"
  local expected_amount="$3"

  JSON_INPUT="$json" python3 - \
    "$expected_description" \
    "$expected_amount" <<'PY'
from decimal import Decimal
import json
import os
import sys

data = json.loads(os.environ["JSON_INPUT"])
expected_description = sys.argv[1]
expected_amount = Decimal(sys.argv[2])

content = data.get("content", [])

found = any(
    isinstance(item, dict)
    and item.get("description") == expected_description
    and item.get("type") == "EXPENSE"
    and Decimal(str(item.get("amount"))) == expected_amount
    for item in content
)

sys.exit(0 if found else 1)
PY
}

execution_history_has_success() {
  local json="$1"
  local expected_scheduled_date="$2"

  JSON_INPUT="$json" python3 - \
    "$expected_scheduled_date" <<'PY'
import json
import os
import sys

data = json.loads(os.environ["JSON_INPUT"])
expected_scheduled_date = sys.argv[1]

found = any(
    isinstance(item, dict)
    and item.get("scheduledDate") == expected_scheduled_date
    and item.get("status") == "SUCCESS"
    and item.get("generatedTransactionId") is not None
    and item.get("errorMessage") is None
    and item.get("completedAt") is not None
    for item in data
)

sys.exit(0 if found else 1)
PY
}

execution_history_has_terminal_failure() {
  local json="$1"
  local expected_scheduled_date="$2"

  JSON_INPUT="$json" python3 - \
    "$expected_scheduled_date" <<'PY'
import json
import os
import sys

data = json.loads(os.environ["JSON_INPUT"])
expected_scheduled_date = sys.argv[1]

if not isinstance(data, list):
    sys.exit(1)

matches = [
    item
    for item in data
    if isinstance(item, dict)
    and item.get("scheduledDate") == expected_scheduled_date
]

valid = (
    len(matches) == 1
    and matches[0].get("status") == "FAILED"
    and matches[0].get("attemptCount") == 3
    and matches[0].get("nextRetryAt") is None
    and matches[0].get("generatedTransactionId") is None
    and isinstance(matches[0].get("errorMessage"), str)
    and len(matches[0].get("errorMessage").strip()) > 0
    and matches[0].get("completedAt") is not None
)

sys.exit(0 if valid else 1)
PY
}

require_command curl
require_command python3

YEAR="$(date -u +%Y)"
MONTH_PADDED="$(date -u +%m)"
TODAY="$(date -u +%Y-%m-%d)"
MONTH="$((10#$MONTH_PADDED))"
DAY="$((10#$(date -u +%d)))"
TRANSACTION_DATE="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
RUN_SUFFIX="${GITHUB_RUN_ID:-local}-$(date +%s)-${RANDOM}"
TRANSFER_IDEMPOTENCY_KEY="transfer-${RUN_SUFFIX}"
FAILED_TRANSFER_IDEMPOTENCY_KEY="failed-transfer-${RUN_SUFFIX}"

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

TRANSFER_REQUEST_BODY="{
  \"fromAccountId\": ${ACCOUNT_ID},
  \"toAccountId\": ${DESTINATION_ACCOUNT_ID},
  \"amount\": 300.00,
  \"description\": \"SmartWallet CI account transfer\"
}"

request POST \
  "/api/transfers" \
  201 \
  "$TRANSFER_REQUEST_BODY" \
  "$PRIMARY_TOKEN" \
  "$TRANSFER_IDEMPOTENCY_KEY"

TRANSFER_ID="$(json_get "$HTTP_BODY" "id")"
TRANSFER_FROM_ACCOUNT_ID="$(json_get "$HTTP_BODY" "fromAccountId")"
TRANSFER_TO_ACCOUNT_ID="$(json_get "$HTTP_BODY" "toAccountId")"

[[ -n "$TRANSFER_ID" ]] \
  || fail "Transfer id is empty"

[[ "$TRANSFER_FROM_ACCOUNT_ID" == "$ACCOUNT_ID" ]] \
  || fail "Transfer source account is incorrect: ${HTTP_BODY}"

[[ "$TRANSFER_TO_ACCOUNT_ID" == "$DESTINATION_ACCOUNT_ID" ]] \
  || fail "Transfer destination account is incorrect: ${HTTP_BODY}"

json_decimal_equals "$HTTP_BODY" "amount" "300.00" \
  || fail "Transfer amount is incorrect: ${HTTP_BODY}"

echo "Transfer id: ${TRANSFER_ID}"

log "Retrying transfer with the same idempotency key"

request POST \
  "/api/transfers" \
  201 \
  "$TRANSFER_REQUEST_BODY" \
  "$PRIMARY_TOKEN" \
  "$TRANSFER_IDEMPOTENCY_KEY"

REPLAYED_TRANSFER_ID="$(json_get "$HTTP_BODY" "id")"

[[ "$REPLAYED_TRANSFER_ID" == "$TRANSFER_ID" ]] \
  || fail "Idempotent retry returned a different transfer: ${HTTP_BODY}"

echo "Idempotent retry returned the original transfer"

log "Verifying that idempotent retry did not change balances twice"

request GET \
  "/api/accounts/${ACCOUNT_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_decimal_equals "$HTTP_BODY" "balance" "550.00" \
  || fail "Source balance changed during idempotent retry: ${HTTP_BODY}"

request GET \
  "/api/accounts/${DESTINATION_ACCOUNT_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_decimal_equals "$HTTP_BODY" "balance" "500.00" \
  || fail "Destination balance changed during idempotent retry: ${HTTP_BODY}"

echo "Idempotent retry did not change account balances twice"

log "Verifying idempotency conflict for different transfer data"

request POST \
  "/api/transfers" \
  409 \
  "{
    \"fromAccountId\": ${ACCOUNT_ID},
    \"toAccountId\": ${DESTINATION_ACCOUNT_ID},
    \"amount\": 250.00,
    \"description\": \"SmartWallet CI account transfer\"
  }" \
  "$PRIMARY_TOKEN" \
  "$TRANSFER_IDEMPOTENCY_KEY"

echo "Different transfer data with the same key was correctly rejected"

log "Verifying idempotency conflict did not modify balances"

request GET \
  "/api/accounts/${ACCOUNT_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_decimal_equals "$HTTP_BODY" "balance" "550.00" \
  || fail "Source balance changed after idempotency conflict: ${HTTP_BODY}"

request GET \
  "/api/accounts/${DESTINATION_ACCOUNT_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_decimal_equals "$HTTP_BODY" "balance" "500.00" \
  || fail "Destination balance changed after idempotency conflict: ${HTTP_BODY}"

echo "Idempotency conflict did not modify balances"

log "Creating a failed transfer with a reusable idempotency key"

request POST \
  "/api/transfers" \
  409 \
  "{
    \"fromAccountId\": ${ACCOUNT_ID},
    \"toAccountId\": ${DESTINATION_ACCOUNT_ID},
    \"amount\": 10000.00,
    \"description\": \"Expected insufficient balance\"
  }" \
  "$PRIMARY_TOKEN" \
  "$FAILED_TRANSFER_IDEMPOTENCY_KEY"

echo "Insufficient balance transfer correctly rejected"

log "Retrying the failed operation with valid transfer data"

request POST \
  "/api/transfers" \
  201 \
  "{
    \"fromAccountId\": ${ACCOUNT_ID},
    \"toAccountId\": ${DESTINATION_ACCOUNT_ID},
    \"amount\": 50.00,
    \"description\": \"Recovered transfer\"
  }" \
  "$PRIMARY_TOKEN" \
  "$FAILED_TRANSFER_IDEMPOTENCY_KEY"

RECOVERED_TRANSFER_ID="$(json_get "$HTTP_BODY" "id")"

[[ -n "$RECOVERED_TRANSFER_ID" ]] \
  || fail "Recovered transfer id is empty"

[[ "$RECOVERED_TRANSFER_ID" != "$TRANSFER_ID" ]] \
  || fail "Recovered operation should create a new transfer"

echo "Failed operation's idempotency key was successfully reused"

log "Verifying balances after recovered transfer"

request GET \
  "/api/accounts/${ACCOUNT_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_decimal_equals "$HTTP_BODY" "balance" "500.00" \
  || fail "Unexpected source balance after recovered transfer: ${HTTP_BODY}"

request GET \
  "/api/accounts/${DESTINATION_ACCOUNT_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_decimal_equals "$HTTP_BODY" "balance" "550.00" \
  || fail "Unexpected destination balance after recovered transfer: ${HTTP_BODY}"

echo "Recovered transfer balances are correct"

log "Verifying transfer history filtering and pagination"

request GET \
  "/api/transfers?accountId=${ACCOUNT_ID}&page=0&size=10" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

transfer_page_contains \
  "$HTTP_BODY" \
  "$TRANSFER_ID" \
  "$ACCOUNT_ID" \
  "$DESTINATION_ACCOUNT_ID" \
  "10" \
  || fail "Transfer history response is incorrect: ${HTTP_BODY}"

echo "Transfer history filtering and pagination are correct"

log "Verifying destination account transfer filtering"

request GET \
  "/api/transfers?accountId=${DESTINATION_ACCOUNT_ID}&page=0&size=10" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

transfer_page_contains \
  "$HTTP_BODY" \
  "$TRANSFER_ID" \
  "$ACCOUNT_ID" \
  "$DESTINATION_ACCOUNT_ID" \
  "10" \
  || fail "Destination account transfer filter is incorrect: ${HTTP_BODY}"

echo "Destination account transfer filtering is correct"

log "Verifying invalid transfer date range rejection"

request GET \
  "/api/transfers?startDate=2026-08-01T00:00:00Z&endDate=2026-07-01T00:00:00Z&page=0&size=10" \
  400 \
  "" \
  "$PRIMARY_TOKEN"

echo "Invalid transfer date range correctly rejected"

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

  if notification_exists "$HTTP_BODY" "$BUDGET_ID"; then
    NOTIFICATION_READY=true
    break
  fi

  sleep "$POLL_DELAY_SECONDS"
done

[[ "$NOTIFICATION_READY" == "true" ]] \
  || fail "BUDGET_EXCEEDED notification was not created in time"

log "Verifying that analytics currency is required"
request GET \
  "/api/analytics/monthly?year=${YEAR}&month=${MONTH}" \
  400 \
  "" \
  "$PRIMARY_TOKEN"
echo "Missing analytics currency correctly rejected"

log "Creating USD account for currency-aware analytics"
request POST "/api/accounts" 201 "{
  \"name\": \"E2E USD Account ${RUN_SUFFIX}\",
  \"type\": \"SAVINGS\",
  \"currency\": \"USD\",
  \"initialBalance\": 500.00
}" "$PRIMARY_TOKEN"
USD_ACCOUNT_ID="$(json_get "$HTTP_BODY" "id")"
[[ -n "$USD_ACCOUNT_ID" ]] || fail "USD account id is empty"

log "Creating USD expense category"
request POST "/api/categories" 201 "{
  \"name\": \"E2E USD Expense ${RUN_SUFFIX}\",
  \"type\": \"EXPENSE\"
}" "$PRIMARY_TOKEN"
USD_CATEGORY_ID="$(json_get "$HTTP_BODY" "id")"
[[ -n "$USD_CATEGORY_ID" ]] || fail "USD category id is empty"

log "Creating USD expense"
request POST "/api/transactions" 201 "{
  \"accountId\": ${USD_ACCOUNT_ID},
  \"categoryId\": ${USD_CATEGORY_ID},
  \"type\": \"EXPENSE\",
  \"amount\": 40.00,
  \"description\": \"SmartWallet CI USD expense\",
  \"transactionDate\": \"${TRANSACTION_DATE}\"
}" "$PRIMARY_TOKEN"
USD_TRANSACTION_ID="$(json_get "$HTTP_BODY" "id")"
[[ -n "$USD_TRANSACTION_ID" ]] || fail "USD transaction id is empty"

log "Waiting for TRY analytics projection"
TRY_ANALYTICS_READY=false
for ((attempt = 1; attempt <= POLL_ATTEMPTS; attempt++)); do
  request GET \
    "/api/analytics/monthly?year=${YEAR}&month=${MONTH}&currency=TRY" \
    200 \
    "" \
    "$PRIMARY_TOKEN"

  total_expense="$(json_get "$HTTP_BODY" "totalExpense")"
  transaction_count="$(json_get "$HTTP_BODY" "transactionCount")"
  response_currency="$(json_get "$HTTP_BODY" "currency")"
  echo "TRY analytics poll ${attempt}/${POLL_ATTEMPTS}: currency=${response_currency}, expense=${total_expense}, count=${transaction_count}"

  if monthly_analytics_matches "$HTTP_BODY" "TRY" "150.00" "1"; then
    TRY_ANALYTICS_READY=true
    break
  fi

  sleep "$POLL_DELAY_SECONDS"
done

[[ "$TRY_ANALYTICS_READY" == "true" ]] \
  || fail "TRY analytics projection was not updated in time"

log "Waiting for USD analytics projection"
USD_ANALYTICS_READY=false
for ((attempt = 1; attempt <= POLL_ATTEMPTS; attempt++)); do
  request GET \
    "/api/analytics/monthly?year=${YEAR}&month=${MONTH}&currency=USD" \
    200 \
    "" \
    "$PRIMARY_TOKEN"

  total_expense="$(json_get "$HTTP_BODY" "totalExpense")"
  transaction_count="$(json_get "$HTTP_BODY" "transactionCount")"
  response_currency="$(json_get "$HTTP_BODY" "currency")"
  echo "USD analytics poll ${attempt}/${POLL_ATTEMPTS}: currency=${response_currency}, expense=${total_expense}, count=${transaction_count}"

  if monthly_analytics_matches "$HTTP_BODY" "USD" "40.00" "1"; then
    USD_ANALYTICS_READY=true
    break
  fi

  sleep "$POLL_DELAY_SECONDS"
done

[[ "$USD_ANALYTICS_READY" == "true" ]] \
  || fail "USD analytics projection was not updated in time"

log "Verifying EUR analytics stays isolated"
request GET \
  "/api/analytics/monthly?year=${YEAR}&month=${MONTH}&currency=EUR" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

monthly_analytics_matches "$HTTP_BODY" "EUR" "0.00" "0" \
  || fail "EUR analytics unexpectedly contains another currency: ${HTTP_BODY}"

echo "Analytics currencies are isolated"

log "Verifying TRY category analytics"
request GET \
  "/api/analytics/monthly/categories?year=${YEAR}&month=${MONTH}&currency=TRY" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

monthly_category_analytics_matches \
  "$HTTP_BODY" \
  "TRY" \
  "$CATEGORY_ID" \
  "150.00" \
  || fail "TRY category analytics is incorrect: ${HTTP_BODY}"

log "Verifying USD category analytics"
request GET \
  "/api/analytics/monthly/categories?year=${YEAR}&month=${MONTH}&currency=USD" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

monthly_category_analytics_matches \
  "$HTTP_BODY" \
  "USD" \
  "$USD_CATEGORY_ID" \
  "40.00" \
  || fail "USD category analytics is incorrect: ${HTTP_BODY}"

log "Verifying TRY monthly trend"
request GET \
  "/api/analytics/monthly-trend?months=6&currency=TRY" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

monthly_trend_matches \
  "$HTTP_BODY" \
  "TRY" \
  "$YEAR" \
  "$MONTH" \
  "150.00" \
  || fail "TRY monthly trend is incorrect: ${HTTP_BODY}"

log "Verifying USD monthly trend"
request GET \
  "/api/analytics/monthly-trend?months=6&currency=USD" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

monthly_trend_matches \
  "$HTTP_BODY" \
  "USD" \
  "$YEAR" \
  "$MONTH" \
  "40.00" \
  || fail "USD monthly trend is incorrect: ${HTTP_BODY}"

log "Verifying TRY daily cash flow"
request GET \
  "/api/analytics/daily-expense?year=${YEAR}&month=${MONTH}&currency=TRY" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

daily_cash_flow_matches \
  "$HTTP_BODY" \
  "TRY" \
  "$DAY" \
  "150.00" \
  || fail "TRY daily cash flow is incorrect: ${HTTP_BODY}"

log "Verifying USD daily cash flow"
request GET \
  "/api/analytics/daily-expense?year=${YEAR}&month=${MONTH}&currency=USD" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

daily_cash_flow_matches \
  "$HTTP_BODY" \
  "USD" \
  "$DAY" \
  "40.00" \
  || fail "USD daily cash flow is incorrect: ${HTTP_BODY}"

echo "Currency-aware analytics endpoints are correct"

log "Verifying that a non-zero balance account cannot be archived"

request DELETE \
  "/api/accounts/${ACCOUNT_ID}" \
  409 \
  "" \
  "$PRIMARY_TOKEN"

echo "Non-zero balance account archive correctly rejected"

request GET \
  "/api/accounts/${ACCOUNT_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

echo "Rejected archive left the account active"

log "Creating an empty account for archive testing"

request POST \
  "/api/accounts" \
  201 \
  "{
    \"name\": \"E2E Archived Account ${RUN_SUFFIX}\",
    \"type\": \"CASH\",
    \"currency\": \"TRY\",
    \"initialBalance\": 0.00
  }" \
  "$PRIMARY_TOKEN"

ARCHIVED_ACCOUNT_ID="$(json_get "$HTTP_BODY" "id")"

[[ -n "$ARCHIVED_ACCOUNT_ID" ]] \
  || fail "Archive test account id is empty"

echo "Archive test account id: ${ARCHIVED_ACCOUNT_ID}"

log "Archiving the zero-balance account"

request DELETE \
  "/api/accounts/${ARCHIVED_ACCOUNT_ID}" \
  204 \
  "" \
  "$PRIMARY_TOKEN"

echo "Zero-balance account archived successfully"

log "Verifying that the account appears in archived accounts"

request GET \
  "/api/accounts/archived" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_collection_contains_id \
  "$HTTP_BODY" \
  "$ARCHIVED_ACCOUNT_ID" \
  || fail "Archived account was not found in archived account list: ${HTTP_BODY}"

echo "Archived account appears in archived account list"


log "Verifying that the archived account is hidden from account list"

request GET \
  "/api/accounts" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_collection_does_not_contain_id \
  "$HTTP_BODY" \
  "$ARCHIVED_ACCOUNT_ID" \
  || fail "Archived account is still visible in account list: ${HTTP_BODY}"

echo "Archived account is hidden from account list"

log "Verifying that transfers to an archived account are rejected"

request POST \
  "/api/transfers" \
  404 \
  "{
    \"fromAccountId\": ${ACCOUNT_ID},
    \"toAccountId\": ${ARCHIVED_ACCOUNT_ID},
    \"amount\": 10.00,
    \"description\": \"Archived account transfer test\"
  }" \
  "$PRIMARY_TOKEN" \
  "archived-account-transfer-${RUN_SUFFIX}"

echo "Transfer to archived account correctly rejected"

log "Restoring the archived account"

request PATCH \
  "/api/accounts/${ARCHIVED_ACCOUNT_ID}/restore" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

RESTORED_ACCOUNT_ID="$(json_get "$HTTP_BODY" "id")"

[[ "$RESTORED_ACCOUNT_ID" == "$ARCHIVED_ACCOUNT_ID" ]] \
  || fail "Restored account id is incorrect: ${HTTP_BODY}"

echo "Archived account restored successfully"


log "Verifying repeated account restore"

request PATCH \
  "/api/accounts/${ARCHIVED_ACCOUNT_ID}/restore" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

REPEATED_RESTORE_ACCOUNT_ID="$(json_get "$HTTP_BODY" "id")"

[[ "$REPEATED_RESTORE_ACCOUNT_ID" == "$ARCHIVED_ACCOUNT_ID" ]] \
  || fail "Repeated restore returned an incorrect account: ${HTTP_BODY}"

echo "Repeated account restore completed without duplication"

log "Verifying that the restored account disappeared from archived accounts"

request GET \
  "/api/accounts/archived" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_collection_does_not_contain_id \
  "$HTTP_BODY" \
  "$ARCHIVED_ACCOUNT_ID" \
  || fail "Restored account is still visible in archived account list: ${HTTP_BODY}"

echo "Restored account disappeared from archived account list"

log "Verifying that the restored account appears in active accounts"

request GET \
  "/api/accounts" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_collection_contains_id \
  "$HTTP_BODY" \
  "$ARCHIVED_ACCOUNT_ID" \
  || fail "Restored account was not found in active account list: ${HTTP_BODY}"

echo "Restored account appears in active account list"

request GET \
  "/api/accounts/${ARCHIVED_ACCOUNT_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

[[ "$(json_get "$HTTP_BODY" "id")" == "$ARCHIVED_ACCOUNT_ID" ]] \
  || fail "Restored account GET response is incorrect: ${HTTP_BODY}"

echo "Restored account is accessible"

log "Transferring money to the restored account"

request POST \
  "/api/transfers" \
  201 \
  "{
    \"fromAccountId\": ${ACCOUNT_ID},
    \"toAccountId\": ${ARCHIVED_ACCOUNT_ID},
    \"amount\": 10.00,
    \"description\": \"Restored account transfer test\"
  }" \
  "$PRIMARY_TOKEN" \
  "restored-account-transfer-${RUN_SUFFIX}"

RESTORED_ACCOUNT_TRANSFER_ID="$(json_get "$HTTP_BODY" "id")"

[[ -n "$RESTORED_ACCOUNT_TRANSFER_ID" ]] \
  || fail "Restored account transfer id is empty"

[[ "$(json_get "$HTTP_BODY" "toAccountId")" == "$ARCHIVED_ACCOUNT_ID" ]] \
  || fail "Restored account was not the transfer destination: ${HTTP_BODY}"

echo "Transfer to restored account completed successfully"

log "Verifying restored account balance"

request GET \
  "/api/accounts/${ARCHIVED_ACCOUNT_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_decimal_equals \
  "$HTTP_BODY" \
  "balance" \
  "10.00" \
  || fail "Restored account balance is incorrect: ${HTTP_BODY}"

echo "Restored account balance is correct"


request GET \
  "/api/accounts/${ACCOUNT_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

SOURCE_BALANCE_BEFORE_ARCHIVED_TRANSFER="$(
  json_get "$HTTP_BODY" "balance"
)"



log "Verifying rejected archived-account transfer did not change balance"

request GET \
  "/api/accounts/${ACCOUNT_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_decimal_equals \
  "$HTTP_BODY" \
  "balance" \
  "$SOURCE_BALANCE_BEFORE_ARCHIVED_TRANSFER" \
  || fail "Source balance changed after rejected archived-account transfer: ${HTTP_BODY}"

echo "Rejected archived-account transfer did not change balance"

log "Preparing recurring transaction execution test"

request GET \
  "/api/accounts/${ACCOUNT_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

BALANCE_BEFORE_RECURRING="$(
  json_get "$HTTP_BODY" "balance"
)"

EXPECTED_BALANCE_AFTER_RECURRING="$(
  python3 - "$BALANCE_BEFORE_RECURRING" <<'PY'
from decimal import Decimal
import sys

balance = Decimal(sys.argv[1])
result = balance - Decimal("25.00")

print(f"{result:.2f}")
PY
)"

RECURRING_DESCRIPTION="E2E recurring expense ${RUN_SUFFIX}"

echo "Balance before recurring transaction: ${BALANCE_BEFORE_RECURRING}"

log "Creating a due recurring transaction"

request POST \
  "/api/recurring-transactions" \
  201 \
  "{
    \"accountId\": ${ACCOUNT_ID},
    \"categoryId\": ${CATEGORY_ID},
    \"type\": \"EXPENSE\",
    \"amount\": 25.00,
    \"description\": \"${RECURRING_DESCRIPTION}\",
    \"frequency\": \"MONTHLY\",
    \"startDate\": \"${TODAY}\",
    \"endDate\": null
  }" \
  "$PRIMARY_TOKEN"

RECURRING_TRANSACTION_ID="$(
  json_get "$HTTP_BODY" "id"
)"

RECURRING_STATUS="$(
  json_get "$HTTP_BODY" "status"
)"

[[ -n "$RECURRING_TRANSACTION_ID" ]] \
  || fail "Recurring transaction id is empty"

[[ "$RECURRING_STATUS" == "ACTIVE" ]] \
  || fail "New recurring transaction should be ACTIVE: ${HTTP_BODY}"

echo "Recurring transaction id: ${RECURRING_TRANSACTION_ID}"

log "Verifying recurring transaction list"

request GET \
  "/api/recurring-transactions" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_collection_contains_id \
  "$HTTP_BODY" \
  "$RECURRING_TRANSACTION_ID" \
  || fail "Recurring transaction was not found in list: ${HTTP_BODY}"

echo "Recurring transaction appears in list"

log "Verifying recurring transaction ownership"

request GET \
  "/api/recurring-transactions/${RECURRING_TRANSACTION_ID}" \
  404 \
  "" \
  "$SECONDARY_TOKEN"

echo "Foreign recurring transaction access correctly rejected"

log "Waiting for recurring transaction scheduler"

RECURRING_EXECUTED=false

for ((attempt = 1; attempt <= POLL_ATTEMPTS; attempt++)); do
  request GET \
    "/api/recurring-transactions/${RECURRING_TRANSACTION_ID}" \
    200 \
    "" \
    "$PRIMARY_TOKEN"

  last_execution_date="$(
    json_get "$HTTP_BODY" "lastExecutionDate"
  )"

  next_execution_date="$(
    json_get "$HTTP_BODY" "nextExecutionDate"
  )"

  echo "Recurring poll ${attempt}/${POLL_ATTEMPTS}: last=${last_execution_date}, next=${next_execution_date}"

  if [[ "$last_execution_date" == "$TODAY" ]]; then
    RECURRING_EXECUTED=true
    break
  fi

  sleep "$POLL_DELAY_SECONDS"
done

[[ "$RECURRING_EXECUTED" == "true" ]] \
  || fail "Recurring transaction was not executed in time"

log "Verifying recurring transaction execution history"

request GET \
  "/api/recurring-transactions/${RECURRING_TRANSACTION_ID}/executions" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

execution_history_has_success \
  "$HTTP_BODY" \
  "$TODAY" \
  || fail "Successful execution history was not found: ${HTTP_BODY}"

echo "Successful recurring execution history was returned"

log "Verifying recurring execution history ownership"

request GET \
  "/api/recurring-transactions/${RECURRING_TRANSACTION_ID}/executions" \
  404 \
  "" \
  "$SECONDARY_TOKEN"

echo "Foreign recurring execution history access correctly rejected"

log "Waiting for recurring transaction scheduler"

RECURRING_EXECUTED=false

for ((attempt = 1; attempt <= POLL_ATTEMPTS; attempt++)); do
  request GET \
    "/api/recurring-transactions/${RECURRING_TRANSACTION_ID}" \
    200 \
    "" \
    "$PRIMARY_TOKEN"

  last_execution_date="$(
    json_get "$HTTP_BODY" "lastExecutionDate"
  )"

  next_execution_date="$(
    json_get "$HTTP_BODY" "nextExecutionDate"
  )"

  echo "Recurring poll ${attempt}/${POLL_ATTEMPTS}: last=${last_execution_date}, next=${next_execution_date}"

  if [[ "$last_execution_date" == "$TODAY" ]]; then
    RECURRING_EXECUTED=true
    break
  fi

  sleep "$POLL_DELAY_SECONDS"
done

[[ "$RECURRING_EXECUTED" == "true" ]] \
  || fail "Recurring transaction was not executed in time"

log "Verifying recurring transaction balance effect"

request GET \
  "/api/accounts/${ACCOUNT_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_decimal_equals \
  "$HTTP_BODY" \
  "balance" \
  "$EXPECTED_BALANCE_AFTER_RECURRING" \
  || fail "Recurring transaction balance is incorrect: ${HTTP_BODY}"

echo "Recurring transaction updated account balance correctly"


log "Verifying generated financial transaction"

request GET \
  "/api/transactions?accountId=${ACCOUNT_ID}&categoryId=${CATEGORY_ID}&type=EXPENSE&page=0&size=50" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

recurring_transaction_exists \
  "$HTTP_BODY" \
  "$RECURRING_DESCRIPTION" \
  "25.00" \
  || fail "Generated recurring financial transaction was not found: ${HTTP_BODY}"

echo "Recurring plan generated a financial transaction"


log "Verifying recurring transaction was not executed twice"

sleep 5

request GET \
  "/api/accounts/${ACCOUNT_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

json_decimal_equals \
  "$HTTP_BODY" \
  "balance" \
  "$EXPECTED_BALANCE_AFTER_RECURRING" \
  || fail "Recurring transaction was executed more than once: ${HTTP_BODY}"

echo "Recurring transaction was executed only once"


log "Pausing recurring transaction"

request PATCH \
  "/api/recurring-transactions/${RECURRING_TRANSACTION_ID}/pause" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

[[ "$(json_get "$HTTP_BODY" "status")" == "PAUSED" ]] \
  || fail "Recurring transaction was not paused: ${HTTP_BODY}"

echo "Recurring transaction paused successfully"


log "Resuming recurring transaction"

request PATCH \
  "/api/recurring-transactions/${RECURRING_TRANSACTION_ID}/resume" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

[[ "$(json_get "$HTTP_BODY" "status")" == "ACTIVE" ]] \
  || fail "Recurring transaction was not resumed: ${HTTP_BODY}"

echo "Recurring transaction resumed successfully"


log "Cancelling recurring transaction"

request DELETE \
  "/api/recurring-transactions/${RECURRING_TRANSACTION_ID}" \
  204 \
  "" \
  "$PRIMARY_TOKEN"

request GET \
  "/api/recurring-transactions/${RECURRING_TRANSACTION_ID}" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

[[ "$(json_get "$HTTP_BODY" "status")" == "CANCELLED" ]] \
  || fail "Recurring transaction was not cancelled: ${HTTP_BODY}"

echo "Recurring transaction cancelled successfully"


log "Verifying cancelled recurring transaction cannot be resumed"

request PATCH \
  "/api/recurring-transactions/${RECURRING_TRANSACTION_ID}/resume" \
  409 \
  "" \
  "$PRIMARY_TOKEN"

echo "Cancelled recurring transaction resume correctly rejected"

log "Creating an account for recurring retry testing"

request POST \
  "/api/accounts" \
  201 \
  "{
    \"name\": \"E2E Recurring Retry Account ${RUN_SUFFIX}\",
    \"type\": \"CASH\",
    \"currency\": \"TRY\",
    \"initialBalance\": 0.00
  }" \
  "$PRIMARY_TOKEN"

RETRY_ACCOUNT_ID="$(
  json_get "$HTTP_BODY" "id"
)"

[[ -n "$RETRY_ACCOUNT_ID" ]] \
  || fail "Recurring retry account id is empty"

echo "Recurring retry account id: ${RETRY_ACCOUNT_ID}"


log "Creating recurring transaction for retry testing"

RETRY_DESCRIPTION="E2E recurring retry ${RUN_SUFFIX}"

request POST \
  "/api/recurring-transactions" \
  201 \
  "{
    \"accountId\": ${RETRY_ACCOUNT_ID},
    \"categoryId\": ${CATEGORY_ID},
    \"type\": \"EXPENSE\",
    \"amount\": 25.00,
    \"description\": \"${RETRY_DESCRIPTION}\",
    \"frequency\": \"MONTHLY\",
    \"startDate\": \"${TODAY}\",
    \"endDate\": null
  }" \
  "$PRIMARY_TOKEN"

RETRY_RECURRING_ID="$(
  json_get "$HTTP_BODY" "id"
)"

[[ -n "$RETRY_RECURRING_ID" ]] \
  || fail "Recurring retry plan id is empty"

echo "Recurring retry plan id: ${RETRY_RECURRING_ID}"


log "Pausing recurring transaction before fault setup"

request PATCH \
  "/api/recurring-transactions/${RETRY_RECURRING_ID}/pause" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

[[ "$(json_get "$HTTP_BODY" "status")" == "PAUSED" ]] \
  || fail "Recurring retry plan was not paused: ${HTTP_BODY}"


log "Archiving recurring transaction account"

request DELETE \
  "/api/accounts/${RETRY_ACCOUNT_ID}" \
  204 \
  "" \
  "$PRIMARY_TOKEN"

echo "Recurring retry account archived"

log "Resuming recurring transaction to trigger retry policy"

request PATCH \
  "/api/recurring-transactions/${RETRY_RECURRING_ID}/resume" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

[[ "$(json_get "$HTTP_BODY" "status")" == "ACTIVE" ]] \
  || fail "Recurring retry plan was not resumed: ${HTTP_BODY}"

log "Waiting for recurring retry policy"

RETRY_POLICY_COMPLETED=false

for ((attempt = 1; attempt <= 30; attempt++)); do
  request GET \
    "/api/recurring-transactions/${RETRY_RECURRING_ID}" \
    200 \
    "" \
    "$PRIMARY_TOKEN"

  retry_plan_status="$(
    json_get "$HTTP_BODY" "status"
  )"

  echo "Retry policy poll ${attempt}/30: status=${retry_plan_status}"

  if [[ "$retry_plan_status" == "PAUSED" ]]; then
    RETRY_POLICY_COMPLETED=true
    break
  fi

  sleep 1
done

[[ "$RETRY_POLICY_COMPLETED" == "true" ]] \
  || fail "Recurring transaction was not paused after failed retries"

log "Verifying final recurring retry execution history"

request GET \
  "/api/recurring-transactions/${RETRY_RECURRING_ID}/executions" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

execution_history_has_terminal_failure \
  "$HTTP_BODY" \
  "$TODAY" \
  || fail "Recurring retry history is incorrect: ${HTTP_BODY}"

echo "Recurring retry history contains three failed attempts"

log "Verifying failed recurring plan generated no transaction"

request GET \
  "/api/transactions?accountId=${RETRY_ACCOUNT_ID}&type=EXPENSE&page=0&size=50" \
  200 \
  "" \
  "$PRIMARY_TOKEN"

if recurring_transaction_exists \
  "$HTTP_BODY" \
  "$RETRY_DESCRIPTION" \
  "25.00"; then

  fail "Failed recurring plan unexpectedly generated a transaction"
fi

echo "Failed recurring plan generated no financial transaction"


log "End-to-end flow passed"
echo "Primary user id : ${PRIMARY_USER_ID}"
echo "Account id      : ${ACCOUNT_ID}"
echo "Category id     : ${CATEGORY_ID}"
echo "Budget id       : ${BUDGET_ID}"
echo "Transaction id  : ${TRANSACTION_ID}"
echo "USD account id  : ${USD_ACCOUNT_ID:-not-created}"
echo "USD category id : ${USD_CATEGORY_ID:-not-created}"
echo "USD transaction : ${USD_TRANSACTION_ID:-not-created}"