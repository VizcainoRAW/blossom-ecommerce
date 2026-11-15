# Blossom E-commerce

A clothing store with a Spring Boot REST API and a React storefront. Customers browse the catalog, keep a cart, check out and track their orders. Employees manage products and categories and move orders through fulfilment.

| Part | Stack | Folder |
|---|---|---|
| API | Java 25, Spring Boot 4.1, Spring Security (JWT), Spring Data JPA | [`spring-service/`](spring-service) |
| Web app | React 19, TypeScript, Vite, Tailwind CSS, React Router | [`react-sevice/`](react-sevice) |
| Database | PostgreSQL 18 | [`deployment/database/`](deployment/database) |
| Infrastructure | Docker Compose, nginx | [`deployment/`](deployment) |

More documentation:

- [API reference](docs/api.md): every endpoint, its request and response, and who may call it.
- [Architecture](docs/architecture.md): code layout, business rules, data model and design decisions.

---

## Quick start (Docker)

You only need Docker.

```bash
cp deployment/.env.example deployment/.env    # then edit the values
docker compose -f deployment/compose.yaml up -d --build
```

| What | URL |
|---|---|
| Storefront | http://localhost:3000 |
| API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| pgAdmin | http://localhost:6060 (`admin@admin.com` / `admin`) |

The first build takes a few minutes because it downloads Gradle, npm packages and base images. Later builds use the cache.

### Dev login

The dev seed ([`02_dev_seed.sql`](deployment/database/02_dev_seed.sql)) creates one employee and five categories (Shirts, Pants, Dresses, Shoes, Accessories):

| Role | Email | Password |
|---|---|---|
| Employee | `admin@blossom.dev` | `admin123` |

Customers sign up through the app or `POST /api/auth/register`.

> ⚠️ The seed is for development only. Don't load `02_dev_seed.sql` in production, and change the admin password in any shared environment.

### Services

| Service | Image / build | Host port | Notes |
|---|---|---|---|
| `postgres` | `postgres:18-alpine` | 5432 | Runs `deployment/database/*.sql` on first start only |
| `spring` | `spring-service/Dockerfile` | `SPRING_PORT` (8080) | Waits for Postgres to be healthy |
| `react` | `react-sevice/Dockerfile` | `WEB_PORT` (3000) | nginx serves the built app and proxies `/api` to `spring` |
| `pgadmin` | `dpage/pgadmin4` | 6060 | Optional DB admin UI |

The browser only talks to the `react` container. nginx forwards `/api/*` to Spring over the internal Docker network, so the API needs no CORS configuration.

### Common commands

```bash
docker compose -f deployment/compose.yaml up -d --build      # build and start everything
docker compose -f deployment/compose.yaml logs -f spring     # follow API logs
docker compose -f deployment/compose.yaml up -d --build spring   # rebuild only the API after code changes
docker compose -f deployment/compose.yaml down               # stop, keep data
docker compose -f deployment/compose.yaml down -v            # stop and DELETE the database
```

**Schema changes:** Postgres only runs the init scripts when its volume is empty. After editing `01_init.sql` or `02_dev_seed.sql`, recreate the volume with `down -v` and then `up -d`. This deletes all data.

---

## Configuration

All settings live in `deployment/.env`. Compose reads it automatically. See [`.env.example`](deployment/.env.example).

| Variable | Required | Description |
|---|---|---|
| `POSTGRES_DB` | yes | Database name |
| `POSTGRES_USER` | yes | Database user |
| `POSTGRES_PASSWORD` | yes | Database password |
| `JWT_SECRET` | yes | HS256 signing key, **at least 32 characters**. The API refuses to start with a shorter one. Generate with `openssl rand -base64 48`. |
| `SPRING_PORT` | no | Host port for the API (default `8080`) |
| `WEB_PORT` | no | Host port for the storefront (default `3000`) |
| `DB_URL` | no | JDBC URL. Compose sets it to the `postgres` container; when running locally it defaults to `jdbc:postgresql://localhost:5432/${POSTGRES_DB}`. |

Tokens expire after 1 hour (`security.jwt.expiration` in [`application.properties`](spring-service/src/main/resources/application.properties)).

> ⚠️ `deployment/.env` holds real secrets. Keep it out of version control and commit only `.env.example`.

---

## Local development

Run Postgres in Docker and the API and web app on your machine, so you get hot reload.

**Requirements:** JDK 25, Node 24, Docker.

```bash
# 1. Database only
docker compose -f deployment/compose.yaml up -d postgres

# 2. API on http://localhost:8080 (export the same variables as deployment/.env)
cd spring-service
export POSTGRES_DB=... POSTGRES_USER=... POSTGRES_PASSWORD=... JWT_SECRET=...
./gradlew bootRun

# 3. Web app on http://localhost:5173 (Vite proxies /api to localhost:8080)
cd react-sevice
npm install
npm run dev
```

On Windows PowerShell, set the variables with `$env:POSTGRES_DB = "..."` and so on, and run `.\gradlew.bat bootRun`.

---

## Testing

### API (`spring-service`)

```bash
cd spring-service
./gradlew check     # all tests plus the 80% coverage gate
./gradlew test      # tests plus the coverage report only
```

- **Docker must be running.** Integration tests start a throwaway PostgreSQL 18 with [Testcontainers](https://testcontainers.com), built from the same `deployment/database` scripts as production.
- **Coverage report:** `spring-service/build/reports/jacoco/test/html/index.html`. The build fails if line or instruction coverage drops below **80%**.

| Kind | Where | What it covers |
|---|---|---|
| Unit | `src/test/.../domain`, `.../application` | Domain rules (validation, totals, order status changes) and use cases, with Mockito standing in for the ports |
| Integration | `src/test/.../infrastructure/web` | Every endpoint through **MockMvc** against real Postgres: security rules, validation errors, search, stock and rollbacks, concurrent checkouts |

### Web app (`react-sevice`)

```bash
cd react-sevice
node --test scripts/*.test.ts   # cart, product and validation logic
npm run lint
npm run build                   # type-check and production build
```

---

## Project structure

```
blossom-ecommerce/
├── deployment/
│   ├── compose.yaml             # postgres, spring, react, pgadmin
│   ├── .env.example             # copy to .env
│   └── database/
│       ├── 01_init.sql          # schema
│       └── 02_dev_seed.sql      # dev admin + categories
├── docs/
│   ├── api.md
│   └── architecture.md
├── spring-service/              # REST API (hexagonal architecture)
│   ├── Dockerfile
│   └── src/main/java/com/blossom/ecommerce/
│       ├── domain/              # entities, rules, exceptions (no framework code)
│       ├── application/port/    # use cases (in) and the interfaces they need (out)
│       └── infrastructure/      # web, persistence, security, Spring config
└── react-sevice/                # storefront SPA
    ├── Dockerfile
    ├── nginx.conf               # serves the SPA, proxies /api
    └── src/
        ├── pages/               # one component per route
        ├── components/
        ├── api.ts               # fetch wrapper + API types
        ├── auth.tsx, jwt.ts     # session from the JWT
        └── cart.ts, cartContext.tsx
```

## Features

**Customers**
- Sign up, log in and log out with JWTs.
- Browse and search products by name, category and price range.
- Keep a cart in the browser.
- Check out. Prices and stock are re-checked on the server.
- View their own orders and cancel ones that are still pending.

**Employees**
- Create, edit and hide products with color/size variants, prices and stock.
- Manage categories (API only, no screen yet).
- See every order, filter by customer or status, and move orders `PENDING → PAID → SHIPPED` or cancel them.

## Known limitations

- **Logout is held in memory.** Logged-out tokens become valid again if the API restarts, and aren't shared between instances. Moving the list to Redis or the database fixes this.
- **Saving a product sets stock to a fixed number.** An employee saving a product at the same moment a customer checks out can overwrite that sale. A separate "adjust stock by ±N" operation would fix it.
- **No payment integration.** An employee marks orders `PAID` by hand.
- **No sign-up for employees.** Employees are created directly in the database (see the seed).
