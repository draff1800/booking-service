# Booking Service
![Development Status](https://img.shields.io/badge/status-in%20development-yellow)

A Spring Boot 3 service for users to book tickets to events, as well as create and manage their own.

This is an enterprise-grade codebase aligned with industry best practices, demonstrating proficiency in backend development.

#### ⚙️ Tech Stack
- Java 17+
- Spring Boot 3
- JSON Web Token (JWT)
- PostgreSQL
- Docker
- JUnit & Mockito

#### 📐 Engineering Practices
- Clean Architecture
- Validation & Error Handling
- Response Pagination
- Considered Domain Modelling
- Horizontal Scale Design (E.g. Concurrency Handling)

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
| <kbd>GET /events</kbd> | List Published upcoming Events (Pageable via `?page=x`)  
| <kbd>GET /events/mine</kbd> | List User's Draft & Published Events (Pageable via `?page=x`)
| <kbd>POST /events/:id/ticket-types</kbd> | Register new Ticket Type for specific Event
| <kbd>GET /events/:id/ticket-types</kbd> | List Ticket Types for specific Event

#### Bookings
| Route | Description                                          
|-------|-------------
| <kbd>POST /bookings</kbd> | Register new Booking(s)
| <kbd>GET /bookings/mine</kbd> | List User's Bookings (Pageable via `?page=x`)

#### Users
| Route | Description                                          
|-------|-------------
| <kbd>PATCH /users/me</kbd> | Update User details
| <kbd>GET /me</kbd> | Get User details

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
      - `JWT_SECRET`: String for authorization (At least 32 characters long)
      - `JWT_TIME_TO_LIVE_SECONDS`: Seconds the JWT is valid for

3. Set up `application-local.properties` environment variables:
    - Create a `src/main/resources/application-local.properties`:

      ```bash
      cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
      ```

    - Adjust the following values to match those from `.env`:

        - `example-host` ⬅️ `POSTGRES_HOST`
        - `0000` ⬅️ `POSTGRES_PORT`
        - `example-database-name` ⬅️ `POSTGRES_DB`
        - `example-user` ⬅️ `POSTGRES_USER`
        - `example-password` ⬅️ `POSTGRES_PASSWORD`
        - `example-jwt-secret` ⬅️ `JWT_SECRET`
        - `example-jwt-time-to-live-seconds` ⬅️ `JWT_TIME_TO_LIVE_SECONDS`

#### Local Development
1. Install the following prerequisites:
    - Java 17
    - Docker Desktop

2. Run Docker Desktop

3. Run the database:

    ```bash
    docker compose up -d
    ```

4. Run the service (on [localhost:8080](http://localhost:8080)):

    ```bash
    ./gradlew bootRun --args='--spring.profiles.active=local'
    ```

5. When you stop the service, remember to stop the database too:

    ```bash
    docker compose down
    ```
