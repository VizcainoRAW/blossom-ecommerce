# API reference

The base URL is `/api`. Through Docker that is `http://localhost:3000/api` (via nginx) or `http://localhost:8080/api` (the API directly). An interactive version lives at `/swagger-ui/index.html` on the API port.

All bodies are JSON. Send `Content-Type: application/json`.

## Authentication

Log in or register to get a JWT, then send it on every protected request:

```
Authorization: Bearer <accessToken>
```

Tokens are HS256-signed and last 1 hour. Claims:

| Claim | Meaning |
|---|---|
| `sub`, `user_id` | Account id (UUID) |
| `account_type` | `CUSTOMER` or `EMPLOYEE`. This decides what the caller may do. |
| `jti` | Token id, used to revoke the token on logout |
| `iat`, `exp` | Issued-at and expiry |

### Access rules

| | Anonymous | Customer | Employee |
|---|:-:|:-:|:-:|
| Register, log in | ✅ | ✅ | ✅ |
| Log out | | ✅ | ✅ |
| Read products and categories | ✅ | ✅ | ✅ |
| Create, edit or delete products and categories | | | ✅ |
| Place an order | | ✅ | |
| List or read orders | | own only | all |
| Cancel an order | | own, while `PENDING` | `PENDING` or `PAID` |
| Change order status | | | ✅ |

Calls without a valid token get `401`. Calls with a token that lacks the needed role get `403`.

## Errors

Every error uses the RFC 9457 *problem detail* format:

```json
{
  "title": "Conflict",
  "status": 409,
  "detail": "Not enough stock for TEE-B-L",
  "instance": "/api/orders"
}
```

Validation errors also include an `errors` map with one message per field:

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid request content.",
  "errors": {
    "shippingAddress": "must not be blank",
    "items[0].quantity": "must be greater than or equal to 1"
  }
}
```

| Status | When |
|---|---|
| `400` | Invalid body or query parameter, or a business rule violated (e.g. unknown category, duplicate SKU in one request) |
| `401` | Missing, expired, revoked or invalid token, or wrong login credentials |
| `403` | Logged in, but the account type isn't allowed |
| `404` | Resource doesn't exist, is hidden, or belongs to another customer |
| `409` | Conflicts with current state: duplicate email, category name or SKU, not enough stock, a disallowed status change, deleting a category that is in use |

## Pagination

List endpoints that page return:

```json
{ "items": [ ... ], "page": 0, "size": 20, "totalItems": 42 }
```

`page` starts at 0. `size` defaults to 20 and is capped at 100.

---

## Auth

### `POST /api/auth/register`

Creates a **customer** account and logs it in.

```json
{
  "firstName": "Ana",
  "lastName": "Diaz",
  "identifier": "ana@mail.com",
  "password": "secret123",
  "documentType": "CC",
  "documentNumber": "1004"
}
```

| Field | Rules |
|---|---|
| `firstName`, `lastName` | Required |
| `identifier` | Required. The login name (normally an email). Stored trimmed and lowercased, and must be unique across all accounts. |
| `password` | 8 to 72 characters |
| `documentType` | Optional: `CC`, `TI`, `NIT`, `CE`, `PASSPORT`. Must be sent together with `documentNumber`. |
| `documentNumber` | Optional. Must be sent together with `documentType`. |

**`201 Created`**
```json
{ "accessToken": "eyJhbGciOi...", "tokenType": "Bearer", "expiresIn": 3600 }
```
Errors: `400` (validation), `409` (identifier already registered).

### `POST /api/auth/login`

Works for customers and employees. The identifier is matched case-insensitively.

```json
{ "identifier": "ana@mail.com", "password": "secret123" }
```

**`200 OK`** returns the same body as register.
**`401`** returns `"Invalid identifier or password"` for every failure: unknown account, wrong password, or disabled account. This means the endpoint can't be used to find out which emails are registered.

### `POST /api/auth/logout` 🔒

Revokes the current token. **`204 No Content`.** Any later request with the same token gets `401`.

---

## Categories

### `GET /api/categories`

All categories, sorted by name. Not paged.

```json
[ { "id": 5, "name": "Accessories" }, { "id": 1, "name": "Shirts" } ]
```

### `GET /api/categories/{id}`

**`200`** `{ "id": 1, "name": "Shirts" }` · **`404`** if it doesn't exist.

### `POST /api/categories` 🔒 employee

```json
{ "name": "Hats" }
```
The name is required, at most 150 characters, trimmed, and unique ignoring case. **`201`** returns the category. **`409`** if the name is taken.

### `PUT /api/categories/{id}` 🔒 employee

Renames the category. Same body and rules as create. **`200`**, **`404`** or **`409`**.

### `DELETE /api/categories/{id}` 🔒 employee

**`204`.** Returns **`409`** `"Category 1 still has products"` while any product uses it, hidden products included.

---

## Products

A product has one or more **variants**: each color/size combination with its own SKU, price and stock. Prices live on variants.

### Product object

```json
{
  "id": 1,
  "categoryId": 1,
  "name": "Linen Shirt",
  "description": "Light and breathable",
  "imageUrls": ["https://cdn.example.com/linen-1.jpg"],
  "variants": [
    { "id": 1, "color": "White", "size": "M", "sku": "LS-W-M", "price": 49.90, "stock": 10 },
    { "id": 2, "color": "White", "size": "L", "sku": "LS-W-L", "price": 59.90, "stock": 5 }
  ]
}
```

### `GET /api/products`

Searches active products. Every filter is optional, and filters combine with AND.

| Query param | Effect |
|---|---|
| `name` | Name contains this text, ignoring case. `%` and `_` are matched literally. |
| `categoryId` | Exact category |
| `minPrice`, `maxPrice` | Matches if **any** variant's price is in the range (inclusive) |
| `page`, `size` | Pagination (see above) |

Results are sorted by id. Returns a page of product objects.

```
GET /api/products?name=shirt&categoryId=1&minPrice=40&maxPrice=60&page=0&size=20
```

### `GET /api/products/{id}`

**`200`** returns the product object. **`404`** if it doesn't exist or has been deleted.

### `POST /api/products` 🔒 employee

```json
{
  "categoryId": 1,
  "name": "Linen Shirt",
  "description": "Light and breathable",
  "imageUrls": ["https://cdn.example.com/linen-1.jpg"],
  "variants": [
    { "color": "White", "size": "M", "sku": "LS-W-M", "price": 49.90, "stock": 10 }
  ]
}
```

| Field | Rules |
|---|---|
| `categoryId` | Required, and the category must exist |
| `name` | Required, at most 255 characters |
| `description`, `imageUrls` | Optional |
| `variants` | At least one. `color` ≤ 50 characters, `size` ≤ 20, `sku` ≤ 100, `price` ≥ 0 with up to 2 decimals, `stock` ≥ 0. Within one product, SKUs must be unique and each color/size combination may appear once (ignoring case). |

**`201`** returns the product object. Errors: **`400`** (validation, unknown category, duplicates inside the request) or **`409`** (SKU already used by another product).

### `PUT /api/products/{id}` 🔒 employee

Replaces the product, using the same body as create. Variants are **matched to existing ones by SKU**:

- **SKU already on the product:** that variant is updated and keeps its `id`.
- **New SKU:** a new variant is created.
- **Existing SKU left out:** that variant is deleted. This returns `409` if an order references it.

**`200`** returns the updated product. **`404`** if the product doesn't exist or is deleted.

### `DELETE /api/products/{id}` 🔒 employee

**Soft delete.** The product disappears from search and `GET`, and can no longer be ordered, but past orders still reference it. **`204`**, or **`404`** if already deleted.

---

## Orders

### Order object

```json
{
  "id": 12,
  "customerId": "4f3d94ae-b77e-4953-b4be-0046b8699528",
  "status": "PENDING",
  "total": 81.47,
  "shippingAddress": "Calle 1 #2-3, Bogotá",
  "createdAt": "2026-11-15T22:29:06.665Z",
  "items": [
    {
      "variantId": 1, "productName": "Tee", "sku": "TEE-B-M", "color": "Black", "size": "M",
      "quantity": 3, "unitPrice": 19.99, "subtotal": 59.97
    }
  ]
}
```

Item name, SKU, color, size and `unitPrice` are copied from the product when the order is placed, so an order keeps showing what was bought even after the product changes.

### Status lifecycle

```
PENDING ──► PAID ──► SHIPPED
   │          │
   └──► CANCELLED ◄──┘
```

Every other change returns `409`. Cancelling returns the items to stock.

### `POST /api/orders` 🔒 customer

```json
{
  "shippingAddress": "Calle 1 #2-3, Bogotá",
  "items": [
    { "variantId": 1, "quantity": 3 },
    { "variantId": 2, "quantity": 1 }
  ]
}
```

- **Prices** come from the database at checkout. The client never sends prices.
- **Stock** is taken in the same transaction. If any item is short, the whole order fails and no stock changes.
- Each `variantId` may appear only once, and `quantity` must be at least 1.

**`201`** returns the order object, with status `PENDING`. Errors:
- **`400`**: validation, an unknown variant, a variant of a deleted product, or a duplicate variant.
- **`409`**: `"Not enough stock for <SKU>"`.

### `GET /api/orders` 🔒

Newest first, paged.

| Query param | Customer | Employee |
|---|---|---|
| `status` | Filters their orders | Filters all orders |
| `customerId` | Ignored: always their own orders | Filters by customer |
| `page`, `size` | Pagination | Pagination |

### `GET /api/orders/{id}` 🔒

**`200`** returns the order object. Customers get **`404`** for orders that aren't theirs, so they can't probe which order ids exist.

### `POST /api/orders/{id}/cancel` 🔒

Customers can cancel their own `PENDING` orders. Employees can also cancel `PAID` ones. Stock is returned. **`200`** returns the updated order. **`409`** if the order can't be cancelled in its current status.

### `PATCH /api/orders/{id}/status` 🔒 employee

```json
{ "status": "PAID" }
```
Accepts `PAID`, `SHIPPED` or `CANCELLED`, following the lifecycle above. Setting `CANCELLED` returns the stock. **`200`**, **`400`** (unknown status value), **`404`** or **`409`** (change not allowed).

---

## Example session (curl)

```bash
API=http://localhost:8080/api

# Employee creates a product
ADMIN=$(curl -s -X POST $API/auth/login -H 'Content-Type: application/json' \
  -d '{"identifier":"admin@blossom.dev","password":"admin123"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
curl -s -X POST $API/products -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"categoryId":1,"name":"Tee","variants":[{"color":"Black","size":"M","sku":"TEE-B-M","price":19.99,"stock":10}]}'

# Customer signs up and orders it
TOKEN=$(curl -s -X POST $API/auth/register -H 'Content-Type: application/json' \
  -d '{"firstName":"Ana","lastName":"Diaz","identifier":"ana@mail.com","password":"secret123"}' \
  | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
curl -s -X POST $API/orders -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"shippingAddress":"Calle 1 #2-3","items":[{"variantId":1,"quantity":2}]}'

# Employee marks it paid
curl -s -X PATCH $API/orders/1/status -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"status":"PAID"}'
```
