#!/bin/bash

# Ultra-quiet Maven build script
# Usage: ./mvn-quiet.sh [maven-args]

echo "Running Maven build silently..."

# Run Maven with maximum quiet settings and redirect all output
mvn "$@" \
  --quiet \
  --batch-mode \
  -Dorg.slf4j.simpleLogger.defaultLogLevel=off \
  -Dorg.slf4j.simpleLogger.log.com.guranxp=off \
  -Dorg.slf4j.simpleLogger.log.org.springframework=off \
  -Dorg.slf4j.simpleLogger.log.org.hibernate=off \
  -Dmaven.test.jvmArgs="-Dorg.slf4j.simpleLogger.defaultLogLevel=off" \
  -Dmaven.javadoc.skip=true \
  -DskipITs=false \
  > /dev/null 2>&1

EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ Build completed successfully"
else
    echo "❌ Build failed with exit code: $EXIT_CODE"
fi

exit $EXIT_CODE
