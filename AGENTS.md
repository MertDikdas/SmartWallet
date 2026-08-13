# SmartWallet Agent Instructions

This file defines the working rules for AI coding agents and human contributors modifying SmartWallet. Read this file together with `README.md`, `docs/ARCHITECTURE.md`, and `docs/DOMAIN_RULES.md` before making architectural or domain changes.

## 1. Project snapshot

SmartWallet is a personal-finance application with a Java/Spring Boot microservice backend and a React/TypeScript web client.

Backend components:

| Component | Port | Responsibility |
| --- | ---: | --- |
| `api-gateway` | 8080 | External API entry point, routing, JWT validation |
| `auth-service` | 8081 | Registration, login, refresh-token rotation, user identity |
| `finance-service` | 8082 | Accounts, categories, transactions, transfers, recurring transactions |
| `budget-service` | 8083 | Monthly category budgets and spending state |
| `notification-service` | 8084 | User-facing notifications |
| `analytics-service` | 8085 | Read-optimized transaction projections and reports |

Infrastructure:

- PostgreSQL 16, one database per stateful service
- RabbitMQ for asynchronous domain events
- Transactional Outbox Pattern in Finance and Budget services
- Flyway for schema migrations
- RSA-signed JWT access tokens
- Docker Compose for the local backend stack
- React 19 + TypeScript + Vite for `smartwallet-web`

## 2. Read before changing code

Before implementing a task:

1. Identify which service owns the data and business rule.
2. Read the relevant controller, service, repository, entity, DTO, migrations, and tests.
3. Check `docs/DOMAIN_RULES.md` for invariants.
4. Check `docs/ARCHITECTURE.md` before introducing service-to-service coupling.
5. Check existing Flyway migrations before changing persistence.
6. Prefer extending existing patterns over introducing a new framework or abstraction.

Do not infer a cross-service contract from a database table. Service APIs and domain events are the integration boundary.

## 3. Service-boundary rules

- A service may write only to its own database.
- Do not join or query tables belonging to another service.
- Finance is the source of truth for accounts, categories, transactions, transfers, balances, and recurring transactions.
- Budget, Analytics, and Notification are derived capabilities and may be eventually consistent.
- Do not move account balances into a separate service unless the consistency model is deliberately redesigned.
- Do not make Analytics or Notification part of a Finance database transaction.
- Budget may synchronously validate category ownership/type through the Finance HTTP API, but it must not access the Finance database.

## 4. Backend coding conventions

### Java and Spring

- Target Java 21.
- Keep Spring Boot aligned with the versions already used by the service; currently the backend uses Spring Boot `4.1.0`.
- Controllers should handle HTTP concerns only: authentication principal, validation, path/query/header extraction, status codes, and response mapping.
- Put business rules and transaction boundaries in services.
- Put persistence queries and locking behavior in repositories.
- Use DTOs at API boundaries; do not expose JPA entities directly.
- Existing request DTOs are Java records. Continue that style unless a concrete requirement makes a class necessary.
- Use constructor injection. Lombok `@RequiredArgsConstructor` is the established pattern.
- Use `@Transactional(readOnly = true)` for read-only service methods where appropriate.
- Do not add broad exception swallowing. Preserve domain exceptions and the existing API error handling pattern.

### Money

- Use `BigDecimal` for monetary values.
- Never use `double` or `float` for balances, transfer amounts, transaction amounts, budget limits, or spent amounts.
- Preserve currency checks before account-to-account transfers.
- A change that mutates a transaction must also correctly reverse and reapply its balance effect.

### Ownership

- User ownership is derived from the JWT subject and represented as a `Long userId` in domain services.
- Every user-owned read/write must remain scoped to that user.
- Never trust a user ID supplied in a request body when the authenticated subject is available.

## 5. Finance consistency and concurrency rules

Finance operations are a strict-consistency boundary.

- Transaction creation/update/deletion and the related account balance changes must remain in one local database transaction.
- Transfers must debit the source account, credit the destination account, and persist the transfer atomically.
- Preserve pessimistic locking where it protects balances or concurrent modifications.
- When multiple accounts must be locked, preserve the current deterministic locking approach in `AccountTransferService` / transaction update logic rather than replacing it with unordered reads.
- Never publish a Finance domain event directly before the database transaction commits. Enqueue it in the Finance outbox within the same transaction.

### Transfer idempotency

`POST /api/transfers` requires `Idempotency-Key`.

- Preserve the unique `(user_id, idempotency_key)` database constraint.
- The same key with the same canonical request may return the original result.
- Reusing a key for a different request must remain a conflict.
- Do not weaken the request fingerprint check.

## 6. Recurring-transaction rules

Recurring transactions are owned by Finance.

- Frequencies currently supported: `WEEKLY`, `MONTHLY`.
- Statuses: `ACTIVE`, `PAUSED`, `CANCELLED`.
- Execution statuses: `PROCESSING`, `SUCCESS`, `FAILED`.
- The pair `(recurring_transaction_id, scheduled_date)` is unique. Do not remove this protection.
- Execution uses `REQUIRES_NEW` transactions so one recurring item does not share the scheduler transaction.
- Failed execution history is recorded in a separate `REQUIRES_NEW` transaction.
- Retry delays and maximum attempts are configuration-driven.
- After the maximum failed attempts, the recurring transaction is paused and a `RecurringTransactionFailedEvent` is written to the Finance outbox.
- Resume logic must not reactivate a cancelled schedule or a schedule whose end date has already passed.

## 7. Messaging and outbox rules

### Finance -> Budget / Analytics

Finance emits `TransactionChangedEvent` through its outbox. Consumers must continue handling created, updated, and deleted transaction effects idempotently.

### Budget -> Notification

When a budget transitions into `EXCEEDED`, Budget creates `BudgetExceededEvent` through its own outbox. Do not publish this event in a way that can be lost after the budget database transaction commits.

### Finance -> Notification

Permanent recurring-transaction failure is emitted as `RecurringTransactionFailedEvent` through the Finance outbox.

### Consumer idempotency

- Budget and Analytics keep processed transaction-event IDs.
- Notification has a unique `source_event_id` for notification creation.
- RabbitMQ redelivery must not cause duplicate financial projections, budget adjustments, or notifications.

Do not remove consumer idempotency because “RabbitMQ already delivered once.” At-least-once delivery means duplicates are possible.

## 8. Database and Flyway rules

- `spring.jpa.hibernate.ddl-auto` is `validate`; Flyway owns schema evolution.
- Never edit an already-applied migration to make a new change.
- Add a new sequential migration in the affected service.
- Keep database constraints for invariants that can be enforced at the database layer.
- Preserve foreign-key deletion behavior unless the domain model is intentionally changing.
- Add indexes when a new access pattern needs them; do not add speculative indexes without a query/use case.
- Never share a schema between services.

## 9. Security rules

- Access tokens are RSA-signed JWTs issued only by Auth Service.
- The private RSA key belongs only to Auth Service.
- Gateway and protected services receive only the public key for verification.
- JWT issuer must remain `smartwallet-auth-service` unless all issuers/verifiers are migrated together.
- Access-token TTL is currently 15 minutes.
- Refresh-token TTL is currently 7 days.
- Refresh tokens are stored server-side as SHA-256 hashes and rotated on refresh.
- User passwords are BCrypt hashes.
- Never log passwords, raw refresh tokens, private keys, authorization headers, or secrets.
- Do not commit generated production secrets or private keys.

Public API routes are limited to the existing auth operations and health/error endpoints. New public routes require an explicit security decision.

## 10. Frontend rules

The web client is in `smartwallet-web`.

- Use React + TypeScript.
- Keep network calls in `src/api/` rather than embedding fetch logic throughout pages.
- Reuse `apiRequest` for authenticated JSON API calls so automatic token refresh remains centralized.
- Current auth session storage uses `sessionStorage`; do not silently switch persistence strategy as part of unrelated work.
- The Vite dev server proxies `/api` to `http://localhost:8080`.
- Protected pages should use the existing auth/route protection rather than duplicating access checks.
- Keep API types close to the API modules until a shared type layer becomes clearly necessary.
- Run both TypeScript build and ESLint after frontend changes.

## 11. Commands to use

### Full backend stack

```bash
./scripts/generate-jwt-keys.sh
docker compose up --build -d
docker compose ps
```

### E2E

```bash
bash scripts/e2e-test.sh
```

### One backend service

```bash
cd finance-service
./mvnw clean verify
```

Use the corresponding Maven Wrapper in the service being changed.

### Frontend

```bash
cd smartwallet-web
npm ci
npm run lint
npm run build
npm run dev
```

## 12. Testing expectations

For backend changes:

- Add or update a focused unit/service test for the changed rule.
- Add repository/integration coverage when locking, queries, database constraints, or event-processing semantics change.
- Run `./mvnw clean verify` in each affected service.
- For cross-service behavior, run the Docker stack and `scripts/e2e-test.sh`.

For frontend changes:

- Run `npm run lint`.
- Run `npm run build`.
- Manually verify the affected route against the gateway when the change depends on backend behavior.

## 13. Definition of done

A change is complete when:

- it respects service ownership and domain invariants;
- API input is validated;
- authenticated ownership is enforced;
- database changes use a new Flyway migration;
- concurrency/idempotency protections are preserved where relevant;
- event-driven changes remain safe for redelivery;
- tests for the affected behavior pass;
- frontend lint/build pass when frontend code changes;
- documentation is updated if the architecture, API, configuration, or domain rules changed.

## 14. Changes agents must not make casually

Do not do any of the following as an incidental refactor:

- merge service databases;
- replace RabbitMQ/outbox flows with direct synchronous writes;
- expose a service database to another service;
- remove pessimistic locks, unique constraints, or idempotency checks;
- change JWT issuer/key ownership;
- edit historical Flyway migration files;
- change monetary types away from `BigDecimal`;
- put passwords/tokens/keys in source control;
- add a new dependency when the existing stack already solves the problem;
- rewrite unrelated modules while completing a scoped task.
