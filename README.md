# micro-portfolio

A small **investments-style API** in Kotlin/Ktor: JWT auth, Postgres holdings with `BigDecimal`, and an **event-driven order flow** on Kafka. Built as a learning project to show production-shaped backend patterns (not a real broker).

```
POST /orders  →  202 + PENDING row  →  Kafka OrderPlaced  →  consumer fills holdings
GET /portfolio  may lag by a moment  (eventual consistency)
```

## Why these choices

- **Kafka instead of updating holdings in the HTTP handler.** The job of `POST /orders` is to *accept* the command. A consumer applies it. That is the same split as “distributed microservices + event-driven architecture.” If Kafka is down after the insert, the API returns **503** (fail closed). Dual-write (DB then Kafka) is an acknowledged shortcut; an outbox would be the next production step.
- **`BigDecimal` / Postgres `NUMERIC(19,8)`.** Binary floats (`0.1 + 0.2`) are not acceptable for quantities or prices. JSON sends decimals as **strings**.
- **Fake quotes** (BTC 65000, ETH 3500, AAPL 180). No market-data feed; price is snapshotted onto the event so the fill matches what we accepted.
- **Same process, real Kafka.** HTTP publishes; an in-app consumer group (`micro-portfolio-holdings`) updates `portfolios`. Not two microservices, still a real broker.

## Architecture

```
Client
  │  JWT
  ▼
Ktor API ──► Postgres (users, orders PENDING, portfolios)
  │
  └── publish OrderPlaced ──► Kafka topic order-placed
                                  │
                                  ▼
                            in-app consumer
                                  │
                                  ▼
                            holdings + order FILLED
```

Correlation: `X-Request-Id` on the HTTP call is copied onto the event and into consumer logs (`requestId=...`).

## Run

**All-in-one (API + Postgres + Kafka):**

```bash
docker compose up --build
# http://localhost:8080/health
```

Stop a local `./gradlew run` first (port 8080). DBeaver: `localhost:5432`, db `portfolio`, user/password `postgres`.

**API on the host** (Postgres + Kafka in Docker only):

```bash
docker compose up -d postgres kafka
./gradlew run
```

Uses `localhost:9092` for Kafka (host listener). See `.env.example`.

```bash
./gradlew test    # no Docker: validation, holding math, auth/health
./scripts/demo.sh # needs a running API + jq
```

## Endpoints

| Method | Path | Auth | Result |
|--------|------|------|--------|
| `GET` | `/health` | – | `{ "status": "UP" }`; echoes `X-Request-Id` |
| `POST` | `/auth/register` | – | **201** `{ id, email }` — BCrypt, unique email |
| `POST` | `/auth/login` | – | **200** `{ token }` |
| `GET` | `/portfolio` | Bearer | holdings for the JWT user |
| `POST` | `/orders` | Bearer | `{ symbol, side, quantity }` → **202** PENDING; consumer fills |

Quotes: `BTC`, `ETH`, `AAPL`. Side: `BUY` / `SELL`. Quantity as a string, e.g. `"0.5"`.

## Layout

```
src/main/kotlin/microportfolio/
├── Application.kt           composition root
├── Routing.kt               HTTP
├── domain/                  tables + applyOrderPlaced
├── orders/                  validateOrder, nextHolding (pure)
├── quotes/                  FakeQuoteService
├── kafka/                   producer, consumer, OrderPlaced
└── plugins/                 Database, Security, Observability
```

## Intentionally skipped

Flyway, a parent `portfolios` table, a separate consumer service, real market data.
