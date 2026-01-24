#!/bin/bash

# Start Colima if not running
if ! colima status 2>&1 | grep -q "running"; then
    echo "Starting Colima..."
    colima start --memory 4 --disk 20
else
    echo "Colima is already running"
fi

# Start PostgreSQL container if not running
if ! docker ps | grep -q "training-postgres"; then
    echo "Starting PostgreSQL container..."
    docker-compose up -d
    
    # Wait for PostgreSQL to be ready
    echo "Waiting for PostgreSQL to be ready..."
    until docker exec training-postgres pg_isready -U training_user -d training_event_db >/dev/null 2>&1; do
        sleep 1
    done
    echo "PostgreSQL is ready!"
else
    echo "PostgreSQL container is already running"
fi
