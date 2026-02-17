# Booking Service
![Development Status](https://img.shields.io/badge/status-in%20development-yellow)

A Spring Boot 3 service for users to book tickets to events. Events can be created and managed by other users.

This is an enterprise-grade codebase aligned with industry best practices, demonstrating proficiency in backend development.

#### ⚙️ Tech Stack
- Java 17+
- Spring Boot 3
- JSON Web Token
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

### 🛠️Setup

#### Getting Started
1. Clone the repository:

    ```bash
    git clone https://github.com/draff1800/booking-service.git
    cd user-service
    ```

2. Set up environment variables (to create the database):
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

3. Set up environment variables (to connect to the database):
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
    ./gradlew bootRun --args='--spring.profiles.active=local''
    ```

5. When you stop the service, remember to stop the database too:

    ```bash
    docker compose down
    ```