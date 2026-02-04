# FluxPay

A payment ledger API with idempotent transaction processing, double-entry bookkeeping, and optimistic concurrency control.

## Architecture

```
React SPA (port 3000) → Spring Boot REST API (port 8080) → PostgreSQL (port 5432)
```

**Backend:** Java 17, Spring Boot 3.2, Spring Data JPA, Spring Security (JWT), Flyway, Maven

**Frontend:** TypeScript, React 18, Vite, TanStack Query, React Hook Form + Zod, Tailwind CSS

**Database:** PostgreSQL 15 with double-entry bookkeeping schema

## Quick Start

### Docker Compose (recommended)

```bash
docker compose up
```

Services: `http://localhost:3000` (frontend), `http://localhost:8080` (API), `localhost:5432` (database)

### Local Development

```bash
# Start database
docker compose up db

# Start backend (requires Java 17)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Start frontend
cd frontend && npm install && npm run dev
```

## API Endpoints

### Auth
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login |
| POST | `/api/auth/refresh` | Refresh JWT |

### Accounts
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/accounts` | Create account |
| GET | `/api/accounts` | List user's accounts |
| GET | `/api/accounts/{id}` | Get account by ID |
| PATCH | `/api/accounts/{id}/status` | Update account status |

### Transactions
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/transactions/deposit` | Deposit funds |
| POST | `/api/transactions/withdraw` | Withdraw funds |
| POST | `/api/transactions/transfer` | Transfer between accounts |
| GET | `/api/transactions?accountId=X` | List transactions (paginated) |
| GET | `/api/transactions/{id}` | Get transaction by ID |

## Key Design Decisions

- **BigDecimal** for all monetary amounts (never `double`/`float`)
- **Idempotent transactions** via client-generated UUID idempotency keys
- **Double-entry bookkeeping** — every transfer creates DEBIT + CREDIT records sharing a correlation ID
- **Optimistic locking** with `@Version` — retries up to 3 times on conflict
- **JWT auth** — access tokens (15min) + refresh tokens (7 days), stored in memory

## Testing

```bash
# Backend unit tests
mvn test

# Backend unit + integration tests (requires Docker for Testcontainers)
mvn verify

# Frontend tests
cd frontend && npm test
```

## Project Structure

```
├── src/main/java/com/payflow/
│   ├── config/          # Security, JWT filter, CORS
│   ├── controller/      # REST controllers
│   ├── service/         # Business logic
│   ├── repository/      # Spring Data JPA
│   ├── model/           # JPA entities + enums
│   ├── dto/             # Request/response DTOs
│   ├── exception/       # Domain exceptions + handler
│   └── util/            # JWT utility
├── src/main/resources/
│   └── db/migration/    # Flyway SQL migrations
├── frontend/
│   └── src/
│       ├── api/         # Axios API client
│       ├── components/  # Reusable React components
│       ├── context/     # Auth context
│       ├── pages/       # Route pages
│       └── types/       # TypeScript types
└── docker-compose.yml
```
