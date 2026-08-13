# SmartWallet Domain Rules

This document records business invariants already represented by the current SmartWallet code and database schema. New code should preserve these rules unless a deliberate product/domain change is being made.

## 1. Identity and ownership

- The authenticated user ID comes from the JWT subject.
- User-owned Finance resources must be queried using both resource ID and authenticated `userId` where applicable.
- A user must not be able to operate on another user's account, category, transaction, transfer, recurring transaction, budget, notification, or analytics data.
- New users are created with role `USER` and `enabled = true`.
- Registration normalizes email by trimming and converting it to lowercase.
- Email is unique.

## 2. Authentication

- Password length is 8-64 characters at registration.
- Passwords are stored as BCrypt hashes.
- Access tokens are JWT Bearer tokens signed by Auth Service.
- JWT issuer is `smartwallet-auth-service`.
- Access-token TTL is 15 minutes.
- Refresh-token TTL is 7 days.
- Raw refresh tokens are not persisted; Auth stores a SHA-256 hash.
- Refreshing rotates the token: the current refresh token is revoked and a new one is issued.
- Logout revokes the supplied valid refresh token.

## 3. Accounts

Supported account types:

- `CHECKING`
- `SAVINGS`
- `CASH`
- `CREDIT_CARD`

Supported currencies:

- `TRY`
- `USD`
- `EUR`

Rules:

- Account name is required and at most 100 characters.
- Initial balance cannot be negative.
- A new account is `ACTIVE`.
- Standard account reads return active accounts.
- Archived accounts are available through the archived-account API.
- An active account can be archived only when its balance is exactly zero.
- Archiving an already archived account is idempotent.
- Restoring an already active account is idempotent.
- Financial operations that require an account use active accounts.

Account deletion in the API is therefore a soft archive operation, not a row deletion.

## 4. Categories

Category types:

- `INCOME`
- `EXPENSE`

Rules:

- Name is required and at most 100 characters.
- Category names are trimmed before persistence.
- The service rejects a duplicate category for the same user, case-insensitive name, and type.
- The database also has a unique `(user_id, name, type)` constraint.
- A transaction category must belong to the authenticated user.
- A transaction's category type must match the transaction type.
- A recurring transaction's category type must match its recurring transaction type.
- A budget can be created only for an `EXPENSE` category owned by the user.

## 5. Financial transactions

Transaction types:

- `INCOME`
- `EXPENSE`

Rules:

- Account ID is required and must identify an active account owned by the user.
- Category ID is required and must identify a category owned by the user.
- Amount must be greater than zero.
- Description is optional and limited to 255 characters; blank/whitespace descriptions are normalized to `null`.
- If `transactionDate` is absent during creation, the current instant is used.
- `INCOME` increases account balance.
- `EXPENSE` decreases account balance.
- The current implementation does not impose a non-negative balance rule for ordinary `EXPENSE` transactions; do not invent one without a product decision.
- Creating a transaction and applying its balance effect occur atomically.

### Updating a transaction

An update may change account, category, type, amount, description, or transaction date.

The service must:

1. lock the owned transaction;
2. determine old and new account state;
3. validate the new category/type combination;
4. reverse the original balance effect;
5. apply the new balance effect;
6. save the updated transaction;
7. enqueue an `UPDATED` transaction event in the same local transaction.

### Deleting a transaction

Deleting a transaction must reverse its balance effect before removing it and must enqueue a `DELETED` transaction event.

### Transaction filtering

- Default page: `0`.
- Default page size: `20`.
- Maximum page size: `100`.
- `startDate` must be before or equal to `endDate`.
- Results are ordered newest first by transaction date, then ID.

## 6. Account transfers

Rules:

- Source and destination account IDs are required and positive.
- Source and destination must be different.
- Both accounts must belong to the authenticated user and be active.
- Transfer amount must be greater than zero.
- Both accounts must use the same currency.
- Source account must have sufficient balance.
- Source debit, destination credit, and transfer persistence are one atomic transaction.
- Description is optional and at most 255 characters.
- If `transferredAt` is absent, the current instant is used.

### Transfer idempotency

`POST /api/transfers` requires an `Idempotency-Key` header.

- The key is normalized and associated with the authenticated user.
- A canonical request fingerprint is stored with the transfer.
- Retrying the same logical request with the same key returns the existing transfer.
- Reusing the same key for a different request is an idempotency conflict.
- The database enforces one `(user_id, idempotency_key)` pair.

### Transfer filtering

- Default page: `0`.
- Default page size: `20`.
- Maximum page size: `100`.
- `startDate` must be before or equal to `endDate`.
- Results are ordered newest first by transfer date, then ID.

## 7. Recurring transactions

Supported frequencies:

- `WEEKLY`
- `MONTHLY`

Schedule statuses:

- `ACTIVE`
- `PAUSED`
- `CANCELLED`

Execution statuses:

- `PROCESSING`
- `SUCCESS`
- `FAILED`

Creation rules:

- Account and category IDs are required and positive.
- Account must belong to the user and be active.
- Category must belong to the user.
- Category type must match recurring transaction type.
- Amount must be at least `0.01` and have at most two decimal places.
- Start date cannot be in the past.
- End date is optional but cannot precede the start date.
- New schedules start as `ACTIVE`.
- Initial `nextExecutionDate` equals `startDate`.

State-change rules:

- Cancelling an already cancelled schedule is idempotent.
- A cancelled schedule cannot be paused.
- A cancelled schedule cannot be resumed.
- Pausing an already paused schedule returns its current state.
- Resuming an already active schedule returns its current state.
- When resuming, missed dates are advanced by the recurrence frequency until the next execution date is today or in the future.
- A schedule cannot be resumed when that computed next execution date is past its configured end date.

Execution rules:

- Only `ACTIVE` schedules are executed.
- A due execution uses the schedule's `nextExecutionDate` as its `scheduledDate`.
- If the scheduled date is beyond the end date, the schedule is cancelled.
- Each `(recurring_transaction_id, scheduled_date)` may have only one execution-history row.
- A successful execution creates a normal Finance transaction, so normal transaction balance and outbox rules apply.
- After success, `lastExecutionDate` is updated and `nextExecutionDate` advances by one week or one month.
- If the newly calculated next execution date is beyond the end date, the schedule is cancelled.

Failure/retry rules:

- A failed execution is recorded separately from the rolled-back execution transaction.
- Failure count is incremented.
- Before maximum attempts are reached, `nextRetryAt` is set from configured retry delays.
- An execution must not retry before `nextRetryAt`.
- When maximum attempts are reached, the recurring schedule becomes `PAUSED`.
- Permanent failure creates `RecurringTransactionFailedEvent` in the Finance outbox.

Current default configuration:

- max attempts: `3`;
- first retry delay: `PT1M`;
- second retry delay: `PT5M`;
- scheduler delay: `60000 ms`.

The root Docker Compose intentionally overrides these timing values to much shorter values for local E2E execution.

## 8. Budgets

Rules:

- Budget category must exist, belong to the user, and be of type `EXPENSE`.
- Limit amount must be greater than zero.
- Year must be at least `2000`.
- Month must be `1-12`.
- Only one budget may exist for a `(user, category, year, month)`.
- A new budget starts with `spentAmount = 0` and status `ACTIVE`.
- Budget status values are `ACTIVE` and `EXCEEDED`.
- Spent amount is derived from transaction events rather than direct writes from Finance.
- Duplicate transaction events must not apply their effect twice.
- A `BudgetExceededEvent` is emitted when a budget transitions from a non-exceeded state into `EXCEEDED`.

## 9. Analytics

- Analytics is derived from transaction events, not the Finance database.
- Each transaction projection is keyed by Finance transaction ID.
- Projection transaction type must be `INCOME` or `EXPENSE`.
- Projection amount must be positive.
- Duplicate transaction events must not be processed twice.
- Report requests are always scoped by authenticated user ID.
- Monthly analytics accepts year >= 2000 and month 1-12.
- Monthly trend accepts at least 1 month; the controller currently declares a maximum validation value of 120 while its message says 12. Treat the actual annotation (`120`) as current runtime behavior until deliberately corrected.

## 10. Notifications

Current notification types:

- `BUDGET_EXCEEDED`
- `RECURRING_TRANSACTION_FAILED`

Rules:

- Notifications belong to a user.
- Event-driven notification creation is idempotent through unique `source_event_id`.
- Notification resources are represented by generic `resource_type` and `resource_id` fields.
- Listing supports `unreadOnly`, pagination, and maximum page size 100.
- Reading one notification must be scoped to the authenticated user.
- Mark-all-read affects only the authenticated user's notifications.

## 11. Event consistency rules

- A Finance transaction state change and its Finance outbox event must commit together.
- A Budget state transition and its Budget outbox event must commit together.
- Consumers must be safe under repeated delivery.
- Eventual consistency is expected for Budget, Analytics, and Notification after a Finance write.
- Temporary lag in those derived services must not cause Finance to roll back a valid financial transaction.
