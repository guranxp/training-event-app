# Training Event App

A Spring Boot application for managing training events.

## Prerequisites

- Java 21 or later
- Maven 3.8+
- Docker & Docker Compose (for local development)
- Colima (for running Docker on macOS)

## Quick Start

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd training-event-app
   ```

2. **Start the application**
   ```bash
   ./start-app.sh
   ```
   The application will be available at `http://localhost:8080`

## Development

### Starting the Application

```bash
# Make scripts executable (first time only)
chmod +x *.sh

# Start the application (includes database)
./start-app.sh
```

### Database

- **Development**: PostgreSQL in Docker
- **Tests**: H2 in-memory database

#### Database Management

- **Start database only**: `./start-db.sh`
- **Stop database**: `docker-compose down`
- **View logs**: `docker-compose logs -f`
- **Access database**:
  ```bash
  docker-compose exec postgres psql -U training_user -d training_event_db
  ```

### Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/guranxp/trainingeventapp/
│   │       └── TrainingEventAppApplication.java
│   └── resources/
│       ├── application.properties
│       └── application-test.properties
└── test/
    └── java/
        └── com/guranxp/trainingeventapp/
            ├── SimpleUnitTest.java
            └── DatabaseConnectionIT.java
```

## Testing

### Run Unit Tests
```bash
./mvnw test
```

### Run Integration Tests
```bash
./mvnw verify -Dskip.unit.tests=true
```

### Run All Tests
```bash
./mvnw verify
```

## Troubleshooting

### Port 5432 Already in Use
If you get a port conflict, either:
1. Stop the service using port 5432, or
2. Update `docker-compose.yml` to use a different port

### Database Connection Issues
1. Check if the database is running: `docker ps`
2. View logs: `docker-compose logs -f`
3. Reset the database (warning: deletes all data):
   ```bash
   docker-compose down -v
   ./start-db.sh
   ```

## License

MIT
