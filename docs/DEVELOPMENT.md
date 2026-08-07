# SmartWallet Development Guide

## 1. Prerequisites

Backend/local stack:

- Java 21 if running services outside Docker
- Docker with Docker Compose
- OpenSSL
- Bash
- `curl`
- Python 3 for the current E2E script

Frontend:

- Node.js/npm compatible with the versions required by the checked-in frontend dependencies
- `npm ci` should be used when reproducing the lockfile exactly

## 2. Repository layout

```text
smartwallet/
├── api-gateway/
├── auth-service/
├── finance-service/
├── budget-service/
├── notification-service/
├── analytics-service/
├── smartwallet-web/
├── scripts/
│   ├── generate-jwt-keys.sh
│   └── e2e-test.sh
├── .github/workflows/
├── docs/
└── docker-compose.yml
```

Each backend service is an independent Maven/Spring Boot project with its own Maven Wrapper and Dockerfile.

## 3. Generate local JWT keys

From the repository root:

```bash
chmod +x scripts/generate-jwt-keys.sh
./scripts/generate-jwt-keys.sh
```

The root Compose setup expects generated key material under the repository's secrets path. Auth receives the private and public key; Gateway and protected services receive the public key only.

Do not commit production private keys.

## 4. Start the backend stack

```bash
docker compose up --build -d
```

Check health:

```bash
docker compose ps
```

Useful URLs:

- Gateway: `http://localhost:8080`
- Auth health: `http://localhost:8081/actuator/health`
- Finance health: `http://localhost:8082/actuator/health`
- Budget health: `http://localhost:8083/actuator/health`
- Notification health: `http://localhost:8084/actuator/health`
- Analytics health: `http://localhost:8085/actuator/health`
- RabbitMQ Management: `http://localhost:15672`

Local RabbitMQ credentials from Compose:

```text
username: smartwallet
password: smartwallet
```

## 5. Stop/reset the backend stack

Stop containers:

```bash
docker compose down
```

Stop and remove local PostgreSQL/RabbitMQ volumes:

```bash
docker compose down -v
```

Use `-v` only when you intentionally want to reset local data.

## 6. Run a backend service directly

The service defaults are configured for local host ports, so infrastructure can be run with Docker while a service is run from the IDE/Maven.

Example:

```bash
cd finance-service
./mvnw spring-boot:run
```

Finance defaults to:

- PostgreSQL: `localhost:5434/smartwallet_finance`
- RabbitMQ: `localhost:5672`
- service port: `8082`

Equivalent defaults exist in each service's `application.yaml`.

When running a service outside Docker, make sure the JWT key location points to a readable public/private key as appropriate.

## 7. Build and test backend services

Run all tests for one service:

```bash
cd finance-service
./mvnw clean verify
```

Repeat for each affected service.

Current CI tests these modules independently:

- `auth-service`
- `finance-service`
- `budget-service`
- `notification-service`
- `analytics-service`
- `api-gateway`

## 8. Run end-to-end tests

With the backend stack running:

```bash
chmod +x scripts/e2e-test.sh
./scripts/e2e-test.sh
```

The E2E flow is designed to exercise behavior through API Gateway and wait for asynchronous event-driven effects where needed.

Root Docker Compose deliberately shortens recurring scheduler/retry timing so recurring failure/retry scenarios can complete during E2E tests.

## 9. Frontend development

Install dependencies:

```bash
cd smartwallet-web
npm ci
```

Start Vite:

```bash
npm run dev
```

Vite normally serves the app on:

```text
http://localhost:5173
```

The frontend dev server proxies:

```text
/api/* -> http://localhost:8080
```

Therefore the backend Gateway should be running when testing real API flows.

### Frontend quality checks

```bash
npm run lint
npm run build
```

`npm run build` runs TypeScript project build and Vite production build.

## 10. Database development

### Database ports

| Database | Host port | Database name |
| --- | ---: | --- |
| Auth | 5433 | `smartwallet_auth` |
| Finance | 5434 | `smartwallet_finance` |
| Budget | 5435 | `smartwallet_budget` |
| Notification | 5436 | `smartwallet_notification` |
| Analytics | 5437 | `smartwallet_analytics` |

Local username/password in Compose are both `smartwallet`.

### Flyway workflow

When schema changes are required:

1. Do not edit an old migration that may already have been applied.
2. Add a new migration under the affected service:

```text
src/main/resources/db/migration/
```

3. Continue the service's migration number sequence.
4. Put constraints/indexes needed by the new domain behavior into the migration.
5. Run that service's tests.
6. Start the service against a clean local database at least once when practical.

Hibernate uses `ddl-auto: validate`, so entity/schema mismatch should fail rather than silently modifying the schema.

## 11. RabbitMQ development

Services read RabbitMQ connection settings from environment variables, with local defaults in `application.yaml`.

Finance and Budget have publisher confirms/returns enabled and use outbox publishers. When debugging event-driven behavior, inspect:

1. the producer business row;
2. the producer `outbox_events` row/status;
3. RabbitMQ bindings/queues;
4. the consumer processed-event table or notification `source_event_id`;
5. the derived domain row.

Do not test an eventual-consistency flow by checking the consumer immediately after the producer HTTP response without allowing for asynchronous delivery.

## 12. Configuration reference

### Common JWT

Protected services use:

```text
JWT_PUBLIC_KEY_LOCATION
```

Auth also uses:

```text
JWT_PRIVATE_KEY_LOCATION
```

### Databases

```text
AUTH_DB_URL
AUTH_DB_USERNAME
AUTH_DB_PASSWORD

FINANCE_DB_URL
FINANCE_DB_USERNAME
FINANCE_DB_PASSWORD

BUDGET_DB_URL
BUDGET_DB_USERNAME
BUDGET_DB_PASSWORD

NOTIFICATION_DB_URL
NOTIFICATION_DB_USERNAME
NOTIFICATION_DB_PASSWORD

ANALYTICS_DB_URL
ANALYTICS_DB_USERNAME
ANALYTICS_DB_PASSWORD
```

### RabbitMQ

```text
RABBITMQ_HOST
RABBITMQ_PORT
RABBITMQ_USERNAME
RABBITMQ_PASSWORD
```

### Service URLs

Gateway uses service URL environment variables such as:

```text
AUTH_SERVICE_URL
FINANCE_SERVICE_URL
BUDGET_SERVICE_URL
NOTIFICATION_SERVICE_URL
ANALYTICS_SERVICE_URL
```

Budget uses:

```text
FINANCE_SERVICE_URL
```

for Finance category validation.

### Recurring transactions

```text
RECURRING_TRANSACTION_SCHEDULER_DELAY_MS
RECURRING_RETRY_MAX_ATTEMPTS
RECURRING_RETRY_FIRST_DELAY
RECURRING_RETRY_SECOND_DELAY
```

Application defaults are:

```text
scheduler delay: 60000 ms
max attempts:    3
first delay:     PT1M
second delay:    PT5M
```

## 13. CI and container publishing

`.github/workflows/ci.yml` currently:

1. runs `clean verify` for all six backend modules;
2. validates Docker Compose;
3. builds Docker images;
4. starts the complete stack;
5. checks health endpoints;
6. runs `scripts/e2e-test.sh`;
7. dumps logs on failure.

`.github/workflows/publish-images.yml` publishes backend images to GitHub Container Registry on pushes to `main` and on manual dispatch.

Each service gets:

- a `latest` tag;
- a `sha-<12-char-commit>` tag.

## 14. Common development checks

### A backend service fails at startup

Check:

- database container health;
- RabbitMQ health for event-driven services;
- JWT key path/permissions;
- Flyway migration state/checksum;
- the service's configured port is free.

### Finance operation returns 401

Check:

- the request went through `/api` Gateway route;
- `Authorization: Bearer <access-token>` is present;
- token issuer matches `smartwallet-auth-service`;
- public key matches the private key used by Auth;
- access token has not expired.

### Frontend gets 401 after some time

The frontend `apiRequest` attempts one token refresh on a 401 when an access token exists. If refresh fails, the auth session is cleared and the user is redirected to the login/home route.

### Async result does not appear immediately

Inspect outbox status and RabbitMQ/consumer processing. Budget, Analytics, and Notifications are intentionally eventually consistent.
