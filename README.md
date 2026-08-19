# orderflow-backend
E-commerce microservices backend — Spring Boot, PostgreSQL, Redis, RabbitMQ


## Day 8 - Observability & Distributed Tracing
- Configured Micrometer Tracing & OpenTelemetry Zipkin Exporter
- Verified trace waterfall visualization across api-gateway and backend services

# OrderFlow Microservices Backend

A scalable, event-driven e-commerce backend built with Spring Boot 3, Spring Cloud Gateway, PostgreSQL, RabbitMQ, and Zipkin.

---

## 🛠 Tech Stack

* **Language & Framework:** Java 17+, Spring Boot 3.x
* **API Gateway:** Spring Cloud Gateway
* **Database & Persistence:** PostgreSQL, Spring Data JPA, Hibernate
* **Asynchronous Messaging:** RabbitMQ (AMQP)
* **Observability & Tracing:** Micrometer Tracing, OpenTelemetry, Zipkin
* **Containerization:** Docker Desktop

---

## 🏗 Architecture Overview

```text
[Client / Postman]
       │
       ▼
 [API Gateway : 8080]
   ├── /api/users/**    ──► [User Service    : 8081]
   ├── /api/products/** ──► [Product Service : 8082]
   ├── /api/orders/**   ──► [Order Service   : 8083]
   └── /api/payments/** ──► [Payment Service : 8084]