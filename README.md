# Event-Driven Order System

A portfolio project demonstrating a **Saga-orchestrated, event-driven microservices architecture** for an e-commerce order flow, built with Java 25, Spring Boot 4 and Apache Kafka.

Four independently deployable Spring Boot services collaborate **only through Kafka events** — no service ever calls another synchronously over HTTP — to drive an order through payment authorization and inventory reservation, with automatic **compensation (rollback)** when a step downstream fails.

```
Order Created ──▶ Payment Requested ──▶ Payment Approved ──▶ Inventory Reserved ──▶ Order Confirmed ──▶ Notification Sent
                         │                                          │
                         ▼ (rejected)                                ▼ (rejected)
                    Order Cancelled                          Payment Compensated (refund) ──▶ Order Cancelled
```

## Architecture

```
                         ┌─────────────────┐
                         │   order-service   │  (Saga Orchestrator)
                         │   REST :8081      │
                         └─────────┬─────────┘
                                   │ Kafka topics
         ┌─────────────────────────┼─────────────────────────┐
         ▼                         ▼                         ▼
┌──────────────────┐     ┌──────────────────┐      ┌──────────────────────┐
│  payment-service   │     │ inventory-service  │      │ notification-service  │
│  REST :8082        │     │ REST :8083         │      │ REST :8084            │
└──────────────────┘     └──────────────────┘      └──────────────────────┘
         │                         │                         │
         └───────────┬─────────────┴─────────────┬───────────┘
                      ▼                           ▼
              ┌──────────────┐            ┌──────────────┐
              │ Apache Kafka  │            │ Redis         │ (idempotency store)
              └──────────────┘            └──────────────┘
                      │
              ┌──────────────┐
              │ PostgreSQL    │ (one logical database per service)
              └──────────────┘
```

Each service owns its own database, its own event DTOs (a deliberate anti-corruption-layer boundary — no shared library), and its own bounded context, following **Domain-Driven Design**: `domain` (entities, invariants) → `application` (use cases / saga logic) → `infrastructure` (persistence, messaging, web, config).

### Saga: order-service is the orchestrator

`order-service` owns the `Order` aggregate and drives the saga forward or backward by publishing command-style events and reacting to reply events. It never blocks on a synchronous call — every hop is an async Kafka round trip, and every transition is recorded in an auditable `saga_log` table for traceability.

| # | Topic | Producer | Consumer | Purpose |
|---|-------|----------|----------|---------|
| 1 | `order.created.events` | order-service | order-service | Decouples the REST write path from saga execution |
| 2 | `payment.requested.events` | order-service | payment-service | Ask payment-service to authorize the charge |
| 3 | `payment.processed.events` | payment-service | order-service | `APPROVED` or `REJECTED` |
| 4 | `inventory.reservation.requested.events` | order-service | inventory-service | Ask inventory-service to reserve stock |
| 5 | `inventory.reservation.processed.events` | inventory-service | order-service | `RESERVED` or `REJECTED` |
| 6 | `payment.compensation.requested.events` | order-service | payment-service | Compensating transaction: refund |
| 7 | `payment.compensated.events` | payment-service | order-service | Confirms refund, saga ends `CANCELLED` |
| 8 | `order.status.events` | order-service | notification-service | Terminal outcome: `CONFIRMED` or `CANCELLED` |

Every consumed event carries `eventId`, `correlationId` (= `orderId`) and `occurredAt`. Every listener is:
- **Idempotent** — a Redis `SETNX` dedup key (`idempotency:<service>:<eventId>`, 24h TTL) guarantees at-most-once processing even under Kafka's at-least-once redelivery.
- **Resilient** — a `DefaultErrorHandler` retries 3× with a 1s fixed backoff, then routes the poison message to a `<topic>.DLT` dead-letter topic instead of blocking the partition.
- **Traceable** — an `X-Correlation-Id` propagates from the inbound HTTP request through every Kafka hop into SLF4J's MDC, so a single order's entire saga can be grepped from the logs of all four services by one id.

## Tech stack

- **Java 25**, **Spring Boot 4.0.8** (Web, Validation, Data JPA, Data Redis, Kafka, Actuator)
- **Apache Kafka** (KRaft mode, no Zookeeper) — event backbone
- **PostgreSQL 16** — one database per service (`orders_db`, `payments_db`, `inventory_db`, `notifications_db`)
- **Redis 7** — idempotency/dedup store
- **Flyway** — versioned schema migrations (`ddl-auto: validate`, never `update`)
- **springdoc-openapi** — live Swagger UI per service
- **JUnit 5 + Mockito** — unit tests
- **Testcontainers** (PostgreSQL + Kafka) — real-infrastructure integration tests
- **Podman / Docker Compose** — local orchestration
- **GitHub Actions** — CI (build + test per service, compose build validation)

## Services

| Service | Port | Responsibility |
|---|---|---|
| `order-service` | `18110` (host) → `8081` | Order aggregate, Saga orchestration, saga audit log |
| `payment-service` | `18111` (host) → `8082` | Payment authorization (>R$5000.00 is auto-rejected for demo purposes) and refunds |
| `inventory-service` | `18112` (host) → `8083` | Product stock, all-or-nothing reservation |
| `notification-service` | `18113` (host) → `8084` | Simulated customer notification (email/SMS) on saga completion |

Infrastructure: PostgreSQL `15500`, Redis `16400`, Kafka `19100`.

Each service exposes:
- `GET /actuator/health` — liveness/readiness
- `GET /swagger-ui.html` — interactive OpenAPI docs
- Full REST CRUD with Bean Validation and a consistent error envelope (`400` validation, `404` not found, `409` illegal state transition)

## Running locally (Podman)

```bash
cd event-driven-order-system
./scripts/start.sh      # builds every image and starts the whole stack
# ...
./scripts/stop.sh       # tears everything down (containers + volumes)
```

`start.sh` auto-generates `secrets/postgres_password.txt` on first run (git-ignored). Works with Docker Compose too — it auto-detects whichever engine is on `PATH`.

### Try the saga end-to-end

Inventory seeds 5 demo products on startup, e.g. `11111111-1111-1111-1111-111111111111` (`SKU-WIRELESS-MOUSE`). Create an order for it and watch it flow through payment → inventory → confirmation:

```bash
curl -s -X POST http://localhost:18110/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "22222222-2222-2222-2222-222222222222",
    "items": [
      { "productId": "11111111-1111-1111-1111-111111111111", "productName": "Wireless Mouse", "quantity": 2, "unitPrice": 49.90 }
    ]
  }'

# Poll the order until the saga settles:
curl -s http://localhost:18110/api/orders/{id} | jq .status
curl -s http://localhost:18110/api/orders/{id}/saga-log | jq .

# The resulting notification:
curl -s "http://localhost:18113/api/notifications?orderId={id}" | jq .
```

An order over R$5000.00, or for a product with insufficient stock, demonstrates the compensation path (payment gets refunded, order ends `CANCELLED`).

## Testing

Each service ships unit tests (Mockito), `@WebMvcTest` controller-slice tests covering every validation/error branch, and a Testcontainers-backed integration test that exercises a real Kafka + PostgreSQL round trip.

```bash
cd order-service && ./mvnw verify   # repeat per service
```

## CI/CD

`.github/workflows/ci.yml` runs `mvn verify` for all four services on every push/PR to `master`, then validates that every Dockerfile actually builds.

## Design notes & deliberate trade-offs

- **Orchestration over pure choreography** for the saga: `order-service` holds the state machine explicitly (`saga_log` audit trail), which is easier to reason about and debug than events chained implicitly across services — a conscious trade-off for a portfolio project meant to demonstrate the pattern clearly.
- **No shared event library**: each service defines its own event DTOs mirroring the same JSON contract. This is a deliberate DDD anti-corruption-layer choice — a schema change in one service can never force a synchronized deploy of another.
- **No transactional outbox**: events are published directly via `KafkaTemplate` inside the same request/listener, not through an outbox table + relay. Documented here as the natural next step for strict dual-write consistency, intentionally out of scope for this demo.
- **Database per service, single PostgreSQL container**: physically colocated for local-dev simplicity, logically fully isolated (separate database + role per service, no cross-schema access).
