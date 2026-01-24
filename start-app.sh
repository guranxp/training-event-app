#!/bin/bash
set -e  # Exit on error

echo "=== Starting Training Event Application ==="

# Check if Colima is running, start if not
if ! command -v colima &> /dev/null; then
    echo "Error: Colima is not installed. Please install it with: brew install colima"
    exit 1
fi

if ! colima status 2>/dev/null | grep -q "running"; then
    echo "Starting Colima..."
    colima start --memory 4 --disk 20 || {
        echo "Error: Failed to start Colima. Please ensure Docker Desktop is properly installed and running."
        exit 1
    }
    echo "Colima started successfully"
else
    echo "✓ Colima is already running"
fi

# Clean up any existing container if it's in a bad state
if docker ps -a --filter "name=training-postgres" --filter "status=exited" | grep -q "training-postgres"; then
    echo "Cleaning up existing container..."
    docker-compose down -v
fi

# Start PostgreSQL container
echo -n "Starting PostgreSQL container... "
if ! docker ps | grep -q "training-postgres"; then
    if ! docker-compose up -d; then
        echo -e "\nError: Failed to start PostgreSQL container. Check if port 5432 is available."
        echo "You can check container logs with: docker-compose logs"
        exit 1
    fi
    echo "Done"
    
    # Wait for PostgreSQL to be ready with timeout
    echo -n "Waiting for PostgreSQL to be ready..."
    for i in {1..30}; do
        if docker-compose exec -T postgres pg_isready -U training_user -d training_event_db &>/dev/null; then
            echo -e "\n✓ PostgreSQL is ready!"
            break
        fi
        if [ $i -eq 30 ]; then
            echo -e "\nError: Timed out waiting for PostgreSQL. Check container logs with: docker-compose logs"
            exit 1
        fi
        sleep 1
        echo -n "."
    done
else
    echo "✓ PostgreSQL container is already running"
fi

# Start the Spring Boot application
echo -e "\nStarting application... (Press Ctrl+C to stop)"
./mvnw spring-boot:run
