# micro-portfolio

Ktor 3 + Kotlin backend for a small portfolio API. JWT auth, Postgres via Exposed + HikariCP.

## Layout

```
src/main/kotlin/microportfolio/
├── Application.kt        fun Application.module() — the single entry point
├── Routing.kt            HTTP endpoints
├── Serialization.kt      ContentNegotiation + kotlinx.serialization JSON
├── StatusPages.kt        exception -> HTTP response mapping
└── plugins/
    ├── Database.kt       Hikari pool + Exposed connection
    └── Security.kt       JWT verification and token creation
src/main/resources/
├── application.yaml      port, module list, jwt + database config
└── logback.xml           logging
```

Folders under `src/main/kotlin` mirror the `package` declaration at the top of
each file: `src/main/kotlin/microportfolio/plugins/Security.kt` declares
`package microportfolio.plugins`.

## Running

```bash
docker compose up -d      # Postgres (+ Kafka, unused for now)
./gradlew run             # http://localhost:8080
./gradlew test            # tests use src/test/resources/application.yaml, no DB needed
```

## Endpoints

| Method | Path             | Auth   | Status |
|--------|------------------|--------|--------|
| GET    | `/health`        | –      | done |
| POST   | `/auth/register` | –      | stub — no user table, no hashing yet |
| POST   | `/auth/login`    | –      | stub — issues a token for any email |
| GET    | `/portfolio`     | Bearer | stub — echoes the `userId` claim |

```bash
TOKEN=$(curl -s -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"me@example.com","password":"x"}' | jq -r .token)

curl localhost:8080/portfolio -H "Authorization: Bearer $TOKEN"
```

## Configuration

`application.yaml` uses `"$ENV_VAR:default"`, so every value can be overridden
by an environment variable. See `.env.example`.

## Next steps

- Users table (Exposed) + BCrypt password hashing, wired into register/login
- Real portfolio domain: tables, repository, routes
- Migrations (Flyway) instead of `SchemaUtils.create`
- Add `exposed-java-time` when you need timestamp columns (left out until it's used)
