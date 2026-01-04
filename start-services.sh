#!/bin/bash

echo "Starting Docker Compose services..."
docker compose up -d

echo "Waiting for services to be healthy..."
echo "Waiting for Elasticsearch..."
until docker exec elasticsearch curl -s http://localhost:9200/_cluster/health > /dev/null 2>&1; do
    echo "  Elasticsearch is not ready yet..."
    sleep 2
done
echo "✓ Elasticsearch is ready"

echo "Waiting for Ollama..."
until docker exec ollama ollama list > /dev/null 2>&1; do
    echo "  Ollama is not ready yet..."
    sleep 2
done
echo "✓ Ollama is ready"

echo "Waiting for model download..."
docker logs -f ollama-pull 2>&1 | grep -q "pulled successfully" && echo "✓ Model downloaded"

echo ""
echo "All services are ready! You can now start your Spring Boot application."
