# 🚀 OrderFlow

`JAVA 17` `SPRING BOOT 3` `POSTGRESQL` `RABBITMQ` `REDIS` `DOCKER`

**A distributed e-commerce order processing backend, built one microservice at a time.**

OrderFlow simulates what happens behind the scenes when a real e-commerce platform processes an order — beyond simple CRUD, it tackles the real problems of distributed systems: service-to-service communication, duplicate payment prevention, event-driven notifications, and independent data ownership per service.

This project is being built as a structured **30-day daily commit challenge**, with each day adding a new distributed-systems concept on top of the last.

---

## 📖 Table of Contents

1. [Why This Exists](#-why-this-exists)
2. [System Architecture](#️-system-architecture)
3. [Tech Stack](#-tech-stack)
4. [Services](#-services)
5. [Core Features](#-core-features)
6. [API Reference](#-api-reference)
7. [Running Locally](#-running-locally)
8. [Build Plan / Roadmap](#️-build-plan--roadmap)
9. [Notable Engineering Decisions](#-notable-engineering-decisions)

---

## 💡 Why This Exists

Most beginner backend projects are a single Spring Boot app with one database — the "happy path," where nothing ever talks to anything else. OrderFlow was built to answer a harder question: **what does it actually take for independent services to work together reliably?**

If Product Service is temporarily unreachable, Order Service needs to handle that. If a customer's order needs to notify them, that shouldn't block or slow down the checkout request itself. If stock is insufficient, the system needs to record *why* the order failed, not just reject it silently. These are the exact problems real e-commerce backends solve every day — this project builds them from the ground up.

---

## 🏗️ System Architecture

```
                          ┌───────────────────┐
                          │   User Service      │  (8081)
                          │   + PostgreSQL       │  userdb
                          │   JWT Auth            │
                          └───────────────────┘

┌───────────────────┐   REST (sync)    ┌───────────────────┐
│   Order Service     │ ───────────────> │  Product Service    │
│   (8083)             │ <─────────────── │  (8082)               │
│   + PostgreSQL       │  check + reduce   │  + PostgreSQL         │
│   orderdb             │      stock         │  productdb            │
└───────────────────┘                    └───────────────────┘
          │
          │  publishes "order.placed" event
          ▼
┌───────────────────┐
│     RabbitMQ          │
│  (topic exchange)     │
└───────────────────┘
          │
          ▼
┌───────────────────┐
│ Notification Consumer │
└───────────────────┘

┌───────────────────┐
│   Payment Service     │  (idempotency-key protected)
│   + PostgreSQL         │
└───────────────────┘
```

Each service is an **independently runnable Spring Boot Maven project**, on its own port, with its **own PostgreSQL database** — following the database-per-service pattern rather than one shared schema.

---

## 🛠️ Tech Stack

| Layer | Technology | Purpose |
|---|---|---|
| Language / Framework | Java 17 & Spring Boot 3 | Core application framework and dependency injection |
| Database | PostgreSQL (one instance per service) | Isolated relational storage enforcing service boundaries |
| Messaging | RabbitMQ | Async event-driven communication decoupling order placement from notifications |
| HTTP Client | RestTemplate + Apache HttpClient5 | Synchronous service-to-service REST calls (with full PATCH support) |
| Security | JWT (planned rollout across services) | Stateless authentication |
| Caching | Redis *(planned)* | Fast product lookups |
| Rate Limiting | Redis *(planned)* | Abuse protection on order creation |
| Containerization | Docker | Running RabbitMQ and future service containerization |
| Build Tool | Maven | Dependency management per independent module |
| API Testing | Postman | Manual endpoint verification |

---

## 🧩 Services

| Service | Port | Database | Status |
|---|---|---|---|
| **User Service** | 8081 | `userdb` | 🔄 Model, repository, JWT util scaffolded — auth endpoints pending |
| **Product Service** | 8082 | `productdb` | ✅ Complete — full CRUD + stock management |
| **Order Service** | 8083 | `orderdb` | ✅ Complete — full CRUD + service-to-service stock check/reduction |
| **Notification Consumer** | — | — | 🔄 In progress — RabbitMQ event consumer |
| **Payment Service** | TBD | TBD | ⬜ Planned — idempotency-key protected mock payments |

---

## 🔥 Core Features

- **Service-to-Service REST Communication:** Order Service calls Product Service synchronously via `RestTemplate` to check stock availability and reserve inventory before confirming an order.
- **Failed-Order Audit Trail:** If stock is insufficient, the order is still persisted with `status: "FAILED"` rather than silently rejected — preserving a record of what happened.
- **PATCH-Compatible HTTP Client:** Java's default `HttpURLConnection` doesn't support PATCH — Order Service's `RestTemplate` is configured with Apache HttpClient5 to support it.
- **Reserved-Keyword-Safe Schema:** The `Order` entity is explicitly mapped to a table named `orders` (not `order`), since `ORDER` is a reserved SQL keyword.
- **Event-Driven Notifications *(in progress)*:** Order placement publishes an event to a RabbitMQ topic exchange, decoupling notification delivery from the checkout request itself.
- **Idempotency Key Enforcement *(planned)*:** Payment requests will carry a unique client-generated key so duplicate network retries return the original payment result instead of double-charging.

---

## 🔌 API Reference

### Product Service — `localhost:8082`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/products` | Create a new product |
| `GET` | `/api/products` | Get all products |
| `GET` | `/api/products/{id}` | Get product by ID |
| `PUT` | `/api/products/{id}` | Update a product |
| `DELETE` | `/api/products/{id}` | Delete a product |
| `PATCH` | `/api/products/{id}/reduce-stock?quantity=X` | Reduce stock quantity |

### Order Service — `localhost:8083`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/orders` | Place a new order (checks + reduces stock via Product Service) |
| `GET` | `/api/orders` | Get all orders |
| `GET` | `/api/orders/{id}` | Get order by ID |

**Example — placing an order:**

```json
POST http://localhost:8083/api/orders
Content-Type: application/json

{
  "productId": 1,
  "quantity": 5
}
```

**Response:**
```json
{
  "id": 1,
  "productId": 1,
  "quantity": 5,
  "status": "CONFIRMED",
  "totalPrice": 3995.0,
  "createdAt": "2026-08-12T23:56:39.6090529",
  "updatedAt": "2026-08-12T23:56:39.6090529"
}
```

---

## 🚀 Running Locally

### Prerequisites
- Java 17+
- Maven (or the included `mvnw` wrapper)
- PostgreSQL running locally on port 5432
- Docker Desktop (for RabbitMQ)
- Postman (for API testing)

### 1. Create the databases

```sql
CREATE DATABASE userdb;
CREATE DATABASE productdb;
CREATE DATABASE orderdb;
```

### 2. Start RabbitMQ

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```
Management UI: `http://localhost:15672` (login: `guest` / `guest`)

### 3. Run each service (in separate terminals)

```bash
cd user-service && ./mvnw spring-boot:run
```
```bash
cd product-service && ./mvnw spring-boot:run
```
```bash
cd order-service && ./mvnw spring-boot:run
```

All three must run **simultaneously** for service-to-service calls to succeed.

---

## 🗺️ Build Plan / Roadmap

| Day | Focus | What gets built | Status |
|---|---|---|---|
| 1 | Foundation + User Service | Multi-service repo structure, User model/repo, JWT util, PostgreSQL | ✅ |
| 2 | Product Service | Product catalog CRUD, stock tracking, dedicated database | ✅ |
| 3 | Order Service + service-to-service calls | Order creation calling Product Service over REST to check/reserve stock | ✅ |
| 4 | Event-Driven Notification | RabbitMQ topic exchange, "order placed" event publisher + consumer | 🔄 |
| 5 | Payment Service + Idempotency | Mock payment processing with idempotency-key duplicate protection | ⬜ |
| 6 | Caching + Rate Limiting | Redis caching on product lookups, rate limiting on order creation | ⬜ |

---

## 🧠 Notable Engineering Decisions

- **`orders` table naming:** `ORDER` is a reserved SQL keyword, so the entity is explicitly mapped with `@Table(name = "orders")` to avoid query conflicts.
- **RestTemplate + PATCH:** Java's default `HttpURLConnection` doesn't support the HTTP `PATCH` method. Order Service's `RestTemplate` bean is configured with Apache HttpClient5 (`HttpComponentsClientHttpRequestFactory`) instead of the default factory, to support the PATCH call to Product Service's stock-reduction endpoint.
- **Database-per-service:** Each service owns its own PostgreSQL database (`userdb`, `productdb`, `orderdb`) rather than sharing one — enforcing service boundaries the way independent microservices would in production.
- **Failed orders are still recorded:** If stock is insufficient, the order is still saved with `status: "FAILED"` rather than silently rejected — preserving an audit trail instead of losing the attempt.

---

## 👤 Author

Built by [Monika Suresh](https://github.com/monikasuresh5) — B.Tech CSE student, SRM Institute of Science and Technology, as a hands-on microservices portfolio project for backend/Java placement prep.

## 📄 License

This project is for educational and portfolio purposes.