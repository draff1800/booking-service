# Booking Service
![Development Status](https://img.shields.io/badge/status-in%20development-yellow)

A Spring Boot 3 service for users to book tickets to events, as well as create and manage their own.

This is an enterprise-grade codebase aligned with industry best practices, demonstrating proficiency in backend development.

### ℹ️ Overview
Booking Service allows users to:
- Register and authenticate via JWT (JSON Web Token)
- Create, manage and view Events
- Create and view Ticket Types for Events, including capacity
- Create and view Bookings for Tickets, including oversell protection
- Manage and view their profile (E.g. Update handle)
- Perform Admin operations (E.g. Update User role)

#### ⚙️ Tech Stack
- Java 17+
- Spring Boot 3
- Spring Security (JWT)
- PostgreSQL
- Redis
- Docker
- JUnit 5 & Mockito
- Testcontainers (Integration testing)

#### 🧩 Application Architecture
Organised across layers:

`Controller → Service → Repository → Database`

- Controllers handle HTTP requests and DTO (Data Transfer Object) mapping
- Services handle business logic
- Repositories handle persistence logic
- Database holds data

Clear separation of API (Interface), Domain (Core business logic) and Infrastructure (External systems e.g. Database).
Improves testability, scalability and flexibility.

#### 🏗️ System Architecture
Envisaged as part of a wider system, including:
* Load balancer/s paired with stateless API design, enabling horizontal scaling
* Separate data layer, protecting business logic from data-access details

```mermaid
flowchart LR

  Client["Clients<br/>(Postman / Frontends)"]
  LB["Load Balancer"]

  subgraph API["Spring Boot API"]
    direction TB

    Security["JWT Security (Stateless)"]

    subgraph Controllers["Controllers"]
      Auth["Auth"]
      Event["Event"]
      Booking["Booking"]
      User["User"]
      Admin["Admin"]
      Health["Health"]
    end

    Services["Services <br/>(Authorization, validation...)"]
    Repositories["Repositories"]
    Cache["Cache Abstraction"]
  end

  subgraph Infra["Infrastructure"]
    Redis[("Redis Cache")]
    Postgres[(PostgreSQL DB)]
  end

  Client -->|HTTP / JSON| LB
  LB --> Auth
  LB --> Event
  LB --> Booking
  LB --> User
  LB --> Admin
  LB --> Health

  Auth --> Security
  Event --> Security
  Booking --> Security
  User --> Security
  Admin --> Security

  Security --> Services
  Services --> Repositories
  Services --> Cache

  Cache --> Redis
  Repositories --> Postgres

  Auth -->|JWT| Client
```

#### 🗂️ Domain Modelling
Key Entities:

- User
- Event
- TicketType
- Booking
- BookingItem

Design Principles:

- Explicit aggregates (E.g. Booking contains multiple BookingItems, which are accessed through that Booking and follow common rules)
- UUID-based identities
- Domain Models (Business logic) separate from DTOs (API contract)
- Public vs Internal identity separation

#### 👥 Public vs Public Identity
Distinguishes between:

| Type | Field | Purpose                                 
|------|-------|---------
| Display | `displayName` | UI-friendly name
| Public | `handle` | Stable external identifier
| Internal | `id` | Database relations

For example, Event responses expose:

```json
{
  ...
  "organizer": {
    "displayName": "Dylan Rafferty",
    "handle": "dylan-rafferty"
  }
}
```

Internal IDs are not exposed publicly.

#### 🔐 Authentication & Authorization
- JWT-based authentication
- Stateless security (Any server can handle any request)
- Role-based access control via Spring Security

Roles:
- `USER`
- `ADMIN`

For example, `PATCH /admin/users/{id}/role` has:

```java
@PreAuthorize("hasRole('ADMIN')")
```

Only `ADMIN`s can change the role of another User.

#### 💾 Caching
Redis is used as a read-through cache for public, read-heavy endpoints where stale data is acceptable for a short period.

Current usage:
| Endpoint | TTL (Time To Live)                               
|----------|-------------------
| `GET /events` | 45 seconds
| `GET /events/:id` | 5 minutes

Strategy:
- Caches stable, shared reads with high re-use
- Does not cache Booking writes
- Does not cache paths that expose tickets' `capacityRemaining`
- Invalidates cache upon Event Update, Publish or Cancel
- If Redis is unavailable, requests fall through to PostgreSQL

#### ✨ Other Features
1. **Oversell Protection (Concurrency-Safe)**
    Ticket booking uses atomic database updates:

    ```sql
    UPDATE TicketType ticketType
    SET ticketType.capacityRemaining = ticketType.capacityRemaining - :qty
    WHERE ticketType.id = :ticketTypeId and ticketType.capacityRemaining >= :qty
    ```

    Also validates quantities and reserves Ticket Types in consistent order, reducing deadlock risk when concurrent requests contain the same items in different orders.

    * No overselling
    * Safety under high concurrency
    * Enforcement at DB level

2. **Idempotency**
    Critical write endpoints (e.g. `POST /bookings`) support the `Idempotency-Key` header.
    Prevents duplicate operations like double bookings.

3. **Pagination**
    List endpoints (e.g. `GET /events`) suppoort pagination:

    ```
    ?page=0&size=20
    ```

    Ensures scalability with large datasets.

4. **Global Error Handling**
    Central handling via `@RestControllerAdvice`, producing consistent API responses:

    ```json
    {
      "timestamp": "...", 
      "status": 401,
      "error": "UNAUTHORIZED",
      "message": "...",
      "path": "...",
      "traceId": "...",
      "details": null
    }
    ```

5. **N+1 Query Avoidance**
    Bulk-fetch strategy is used to avoid chain-reactions of queries.

    For example:
    * `GET /events` - All organizers are retrieved in 1 call.
    * `GET /bookings/mine` - All Booking Items are retrieved in 1 call.

    Prevents performance degradation at scale.

#### 🧪 Testing
* **Unit:** Verifies business logic in isolation using JUnit and Mockito.
* **Framework Integration:** Verifies Spring-managed behaviour (e.g. caching) using a lightweight Spring test context with mocked repositories.
* **Infrastructure Integration:** Verifies persistence and concurrency behaviour against PostgreSQL using `@Testcontainers`.


---

### 🔌Endpoints
#### Authentication
| Route | Description                                     
|-------|-------------
| <kbd>POST /auth/register</kbd> | Register new User
| <kbd>POST /auth/login</kbd> | Authenticate User via JSON Web Token (JWT)

#### Events
| Route | Description                                          
|-------|-------------
| <kbd>POST /events</kbd> | Register new Event
| <kbd>PATCH /events/:id</kbd> | Update specific Event
| <kbd>POST /events/:id/publish</kbd> | Publish specific Event
| <kbd>POST /events/:id/cancel</kbd> | Cancel specific Event
| <kbd>GET /events</kbd> | List Published upcoming Events (Pageable via `?page=x&size=y`)
| <kbd>GET /events/mine</kbd> | List User's Draft & Published Events (Pageable via `?page=x&size=y`)
| <kbd>GET /events/:id</kbd> | Get specific Event
| <kbd>POST /events/:id/ticket-types</kbd> | Register new Ticket Type for specific Event
| <kbd>GET /events/:id/ticket-types</kbd> | List Ticket Types for specific Event

#### Bookings
| Route | Description                                          
|-------|-------------
| <kbd>POST /bookings</kbd> | Register new Booking(s)
| <kbd>GET /bookings/mine</kbd> | List User's Bookings (Pageable via `?page=x&size=y`)

#### Users
| Route | Description                                          
|-------|-------------
| <kbd>PATCH /users/me</kbd> | Update current User details
| <kbd>GET /users/me</kbd> | Get current User

#### Admin
| Route | Description                              
|-------|-------------
| <kbd>PATCH /admin/users/:id/role</kbd> | Update specific User's role

#### Health
| Route | Description                                          
|-------|-------------
| <kbd>GET /health</kbd> | Get server status

---

### 🛠️Setup

#### Getting Started
1. Clone the repository:

    ```bash
    git clone https://github.com/draff1800/booking-service.git
    cd user-service
    ```

2. Set up `.env` environment variables:
    - Create a `.env`:

      ```bash
      cp .env.example .env
      ```

    - Adjust its values based on your desired configuration:

      - `POSTGRES_DB`: Database name
      - `POSTGRES_USER`: Database User username
      - `POSTGRES_PASSWORD`: Database User password
      - `POSTGRES_HOST`: Database host (E.g. `localhost`)
      - `POSTGRES_PORT`: Database port (E.g. `5432`)
      - `REDIS_ENABLED`: Whether caching is enabled (E.g. `GET /events`)
      - `REDIS_HOST`: Cache host (E.g. `localhost`)
      - `REDIS_PORT`: Cache port (E.g. `6379`)
      - `JWT_SECRET`: String for authorization (At least 32 characters long)
      - `JWT_TIME_TO_LIVE_SECONDS`: Seconds the JWT is valid for
			
3. Load `.env` for use by `application.properties`:

    ```bash
    set -a
    source .env
    set +a
	  ```

#### Local Development
1. Install the following prerequisites:
    - Java 17
    - Docker Desktop

2. Run Docker Desktop

3. Run the database and cache:

    ```bash
    docker compose up -d
    ```

4. Run the service (on [localhost:8080](http://localhost:8080)):

    ```bash
    ./gradlew bootRun
    ```

5. To bootstrap a first Admin:
    * Register a User via `/auth/register`
    * Run `docker ps` to find the database container name
    * Open a psql session in the container:
        ```bash
        docker exec -it <container name> psql -U <POSTGRES_USER value> -d <POSTGRES_DB value>
        ```
    * Run the following SQL:
        ```sql
        UPDATE users 
        SET role = 'ADMIN' 
        WHERE email = <users email>;
        ```
    * Log in via `auth/login` to receive updated JWT with `ADMIN` role

5. When stopping the service, remember to stop the database too:

    ```bash
    docker compose down
    ```

---

### 🌱 Future Improvements
* RabbitMQ integration (Asynchronous processing)
* Resilience improvements (E.g. Rate limiting)
* Spring Actuator integration (Monitoring)
* OpenAPI integration (API documentation)
* Automated code quality checks
* General functionality additions
* Improved test coverage
