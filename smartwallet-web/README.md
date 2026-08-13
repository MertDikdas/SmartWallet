# SmartWallet Web

Responsive React + TypeScript frontend for the SmartWallet microservice application.

## Included flows

- Landing, registration, login, refresh-token handling, and logout
- Dashboard overview with balances, monthly analytics, category spending, and cash-flow trend
- Account creation and multi-currency account cards
- Income/expense transactions and category creation
- Monthly category budgets with progress and exceeded states
- Idempotent transfers between accounts with matching currencies
- Weekly/monthly recurring transactions with pause and resume actions
- Notification center for exceeded budgets and failed recurring transactions
- Responsive sidebar, mobile layouts, loading states, empty states, and API errors

## Requirements

- Node.js version compatible with the versions in `package.json`
- SmartWallet API Gateway running on `http://localhost:8080`

## Run locally

```bash
npm install
npm run dev
```

The Vite development server runs on `http://localhost:5173` and proxies `/api` calls to the API Gateway on port `8080`.

## Environment variable

Copy `.env.example` to `.env` only when the frontend and gateway are hosted on different origins:

```bash
cp .env.example .env
```

Then set:

```env
VITE_API_BASE_URL=https://your-api-gateway.example.com
```

Leave the value empty during normal local development so Vite uses the proxy in `vite.config.ts`.

## Checks

```bash
npm run lint
npm run build
```

## Main structure

```text
src/
├── api/          # Backend API functions and response types
├── auth/         # Token storage, refresh, and protected routes
├── components/   # Reusable icons and modal
├── pages/        # Landing, authentication, and dashboard pages
├── styles/       # Shared authentication styles
└── utils/        # Currency and date formatting helpers
```

## Backend routes used

The frontend is aligned with the existing SmartWallet gateway routes:

- `/api/auth`, `/api/users`
- `/api/accounts`, `/api/categories`, `/api/transactions`
- `/api/transfers`, `/api/recurring-transactions`
- `/api/budgets`, `/api/analytics`, `/api/notifications`
