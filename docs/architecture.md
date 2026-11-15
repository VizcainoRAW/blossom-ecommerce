# Architecture

## System overview

```
                ┌──────────────────────── Docker network ────────────────────────┐
 Browser ──► :3000  react (nginx)                                                 │
                │   ├─ /            → built React SPA (index.html fallback)        │
                │   └─ /api/*       → proxy ──► spring :8080 ──► postgres :5432    │
                └────────────────────────────────────────────────────────────────┘
 Swagger/curl ─► :8080 spring (also published for direct API access)
```

- **react:** a static build served by nginx. API calls are relative (`/api/...`), so in Docker nginx forwards them to Spring, and in development the Vite dev server does. Either way the browser sees one origin, so the API has no CORS setup.
- **spring:** a stateless REST API. Logins are JWTs; there are no server sessions.
- **postgres:** the schema is created by `deployment/database/01_init.sql`. Hibernate never changes the schema (`ddl-auto=none`).

## Backend: hexagonal architecture

The API follows *ports and adapters*. Dependencies point inward: the domain knows nothing about Spring, JPA or HTTP.

```
infrastructure/web          ─┐
infrastructure/persistence   ├──► application/port ──► domain
infrastructure/security     ─┘
```

| Layer | Package | Contains | Depends on |
|---|---|---|---|
| Domain | `domain.model`, `domain.exception` | Entities with their rules (`Customer`, `Product`, `Order`...), value objects, domain exceptions | nothing |
| Application | `application.port.in` | Use cases: `RegisterCustomerUseCase`, `LoginUseCase`, `ManageProductsUseCase`, `ManageCategoriesUseCase`, `ManageOrdersUseCase` | domain, out-ports |
| | `application.port.out` | Interfaces the use cases need: `CustomerRepository`, `ProductRepository`, `InventoryPort`, `TransactionPort`, `PasswordEncoderPort`... | domain |
| Infrastructure | `infrastructure.web` | REST controllers, request/response DTOs, `GlobalExceptionHandler` | application |
| | `infrastructure.persistence` | JPA entities, Spring Data repositories, adapters that implement the out-ports | application, JPA |
| | `infrastructure.security` | `SecurityConfig`, JWT issuing and checking, BCrypt adapter, logout list | application, Spring Security |
| | `infrastructure.config` | `UseCaseConfig`, which wires the plain-Java use cases as Spring beans | everything |

What this buys:

- **Use cases and domain are plain Java,** so they're unit-tested with Mockito and need no Spring context.
- **JPA entities are kept apart from domain models.** Adapters map between them, so persistence details like lazy loading and generated ids don't leak into the business rules.
- **Transactions cross layers through `TransactionPort`.** Placing an order touches stock and orders together, so the use case wraps that work in `transactions.inTransaction(...)` without depending on Spring. The Spring adapter uses `TransactionTemplate`.

### Request flow example: placing an order

1. `OrderController` validates the DTO, reads the customer id from the JWT `sub` claim, and calls `ManageOrdersUseCase.place(...)`.
2. Inside one transaction, the use case:
   1. loads the requested variants that belong to active products (`InventoryPort.findOrderable`);
   2. builds `OrderItem`s with a snapshot of name, SKU, color, size and current price;
   3. calls `Order.place(...)`, which checks the rules (address present, at least one item, no duplicate variant) and computes the total;
   4. takes stock for each item with an atomic `UPDATE ... SET stock = stock - :q WHERE id = :id AND stock >= :q`, in variant-id order;
   5. saves the order.
3. Any failure (missing variant, not enough stock) throws, and the whole transaction rolls back, stock included.
4. `GlobalExceptionHandler` turns domain exceptions into problem-detail responses.

## Business rules

### Accounts and security

- **Two account types share one `accounts` table:** `CUSTOMER` and `EMPLOYEE`, with details in `customers` or `employees` (JPA joined inheritance, using `account_type` to tell them apart).
- **Identifiers** (emails) are stored trimmed and lowercased, and are unique across both types.
- **Passwords** are hashed with BCrypt.
- **The JWT carries `sub`/`user_id` and `account_type`.** Spring Security maps `account_type` to `ROLE_CUSTOMER` or `ROLE_EMPLOYEE`, and the URL rules in `SecurityConfig` check those roles.
- **Logout** stores the token's `jti` in `RevokedTokenStore` until the token would have expired; token checking rejects listed ids. This list is in memory (see limitations).
- **Login failures all return the same message,** so the endpoint can't be used to find out which emails are registered.

### Catalog

- **Products have variants** (color + size + SKU + price + stock). Search by price matches a product if any of its variants is in range.
- **Deleting a product is a soft delete** (`active = false`). Order lines reference variants, so the rows must stay. Hidden products can't be read, searched or ordered.
- **Updates match variants by SKU,** so variant ids, which orders reference, survive edits. The `sku` and `(product_id, color, size)` unique constraints are `DEFERRABLE INITIALLY DEFERRED`, so one save can replace or swap variants without clashing with its own old rows partway through.
- **Categories** have unique names, ignoring case. A category can only be deleted while no product (hidden ones included) uses it.

### Orders

- **Prices are never taken from the client.** The server reads them at checkout and copies them, with name, SKU, color and size, onto the order line.
- **Stock can't oversell.** The stock decrease is a single conditional `UPDATE`, so two concurrent checkouts can't both take the last unit; an integration test checks this with parallel requests. Variant rows are updated in id order, so two checkouts can't deadlock on each other.
- **Status changes follow** `PENDING → PAID → SHIPPED`, and cancel is allowed from `PENDING` or `PAID` (`OrderStatus.canTransitionTo`).
- **Cancelling returns the stock.** Cancel and status changes lock the order row (`SELECT ... FOR UPDATE`), so two simultaneous cancels can't return the stock twice.
- **Customers only ever see their own orders.** Someone else's order returns 404, not 403, so order ids can't be probed.

## Data model

```
roles 1───* employees *───1 accounts 1───1 customers 1───* orders 1───* order_items *───1 product_variants *───1 products *───1 categories
```

| Table | Key columns | Notes |
|---|---|---|
| `accounts` | `id UUID`, `identifier` (unique), `password_hash`, `account_type`, `active` | Shared login data |
| `customers` | `account_id` → accounts, `first_name`, `last_name`, `document_type`, `document_number` | |
| `employees` | `account_id` → accounts, `role_id` → roles, names, document | |
| `roles` | `id`, `name` | Seeded with `ADMIN`; not yet used for permissions |
| `categories` | `id`, `name` (unique) | |
| `products` | `id`, `category_id`, `name`, `description`, `image_urls TEXT[]`, `active` | Index on `LOWER(name)` and on `category_id` |
| `product_variants` | `id`, `product_id`, `color`, `size`, `sku` (unique, deferred), `price`, `stock` | `CHECK (price >= 0)`, `CHECK (stock >= 0)`, unique `(product_id, color, size)` (deferred) |
| `orders` | `id`, `customer_id`, `status`, `total`, `shipping_address`, `create_at`, `update_at` | `CHECK` on status values; index on `customer_id` |
| `order_items` | PK `(order_id, variant_id)`, `product_name`, `sku`, `color`, `size`, `quantity`, `unit_price` | Snapshot taken at purchase |

The `CHECK (stock >= 0)` constraint is a second safety net behind the conditional update. Even buggy code can't push stock negative.

## Error handling

`GlobalExceptionHandler` maps exceptions to HTTP status codes:

| Exception | Status |
|---|---|
| Bean validation (`@Valid`) | 400, with an `errors` map per field |
| `InvalidCredentialsException` | 401 |
| `ProductNotFoundException`, `CategoryNotFoundException`, `OrderNotFoundException` | 404 |
| `IdentifierAlreadyRegisteredException`, `CategoryConflictException`, `OrderConflictException` | 409 |
| `DataIntegrityViolationException` (unique or foreign-key violation from the database) | 409 |
| Any other `DomainException` | 400 |

## Frontend

| Area | Files | Notes |
|---|---|---|
| Routing | `App.tsx` | `RequireAuth` guards customer routes, and employee routes with `employee` |
| API client | `api.ts` | Adds the bearer token, turns problem details into `Error`s, and logs the user out on 401 |
| Session | `auth.tsx`, `jwt.ts` | Token in `localStorage`; reads `account_type` and expiry from the JWT |
| Cart | `cart.ts` (pure logic, unit-tested), `cartContext.tsx` | Kept in `localStorage`; the server re-checks price and stock at checkout |
| Pages | `pages/` | Home, Products (search and filters), ProductDetail, Cart, Checkout, Orders, OrderDetail (cancel and status buttons), Login, Register, ManageProducts, ProductForm |

## Testing strategy

| Level | Tooling | Scope |
|---|---|---|
| Domain unit | JUnit 5, AssertJ | Every rule in the domain models |
| Use case unit | Mockito | Use case logic with fake ports (e.g. the stock order, rollback paths, who can see what) |
| Integration | `@SpringBootTest`, MockMvc, Testcontainers PostgreSQL 18 | Every endpoint end to end, including security, validation, SQL behavior and a concurrent checkout race |

The integration database is built from the same `01_init.sql` and `02_dev_seed.sql` as production, so tests catch schema drift. JaCoCo fails `./gradlew check` below 80% line or instruction coverage.

## Known limitations and next steps

| Limitation | Suggested fix |
|---|---|
| The logout list is in memory: lost on restart, not shared between instances | Store revoked `jti`s in Redis or a table with a TTL |
| Saving a product sets stock to a fixed number and can overwrite a concurrent sale | Add an "adjust stock by ±N" endpoint and stop accepting stock in product updates |
| No payment provider; `PAID` is set by hand | Payment integration with webhooks |
| No sign-up or management screens for employees, and `roles` isn't used for permissions | Employee admin endpoints and role-based permissions |
| No category management screen | Add a page using the existing category endpoints |
| Schema changes need `down -v` | Add migrations (Flyway or Liquibase) |
