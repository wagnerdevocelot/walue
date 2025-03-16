# Walue - Portfolio Evaluation Service

[![CI/CD Pipeline](https://github.com/yourusername/walue/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/yourusername/walue/actions/workflows/ci-cd.yml)
[![Architectural Fitness](https://img.shields.io/badge/Architectural%20Fitness-Checked-brightgreen.svg)](https://github.com/yourusername/walue/blob/main/src/walue/infra/fitness.clj)

A Clojure web service that implements a portfolio evaluation system using Domain-Driven Design (DDD) and Hexagonal Architecture.

## Overview

This service evaluates investment portfolios based on custom criteria. It receives a portfolio of assets and a list of criteria, evaluates each asset against the criteria, and returns a ranked portfolio with scores for each asset.

## Architecture

The project follows Hexagonal Architecture (also known as Ports and Adapters) and Domain-Driven Design principles:

```
                                    +---------------------+
                                    |                     |
                     +------------->|  HTTP Adapter       |
                     |              |  (http_adapter.clj) |
                     |              |                     |
                     |              +----------+----------+
                     |                         |
                     |                         | uses
                     |                         v
+-------------------++                +--------+---------+                 +-----------------+
|                    |                |                  |                 |                 |
|  Client Request    +--------------->+  Evaluation Port +---------------->+  Domain Logic   |
|  (JSON)            |                |  (port)          |                 |  (evaluation.clj)|
|                    |                |                  |                 |                 |
+--------------------+                +--------+---------+                 +-----------------+
                     |                         |
                     |                         | supported by
                     |                         v
                     |              +----------+----------+
                     |              |                     |
                     +------------->|  Infrastructure     |
                                    |  (logging, metrics) |
                                    |                     |
                                    +---------------------+
```

- **Domain (Core)**: Contains the business logic for portfolio evaluation.
- **Ports**: Define interfaces that the domain uses to interact with the outside world.
- **Adapters**: Implement the interfaces defined by the ports, connecting the domain to external systems (HTTP, databases, etc.).
- **Infrastructure**: Provides technical capabilities like logging and metrics.

### Architectural Fitness Functions

This project includes automated architectural fitness functions that enforce the following rules:

1. **Layer Dependency Rules**:
   - Domain can only depend on Clojure stdlib
   - Port can only depend on Domain
   - Adapter can depend on Domain and Port
   - Infrastructure should be self-contained
   - Core can depend on all layers

2. **Domain Purity**: Ensures domain code has no external dependencies beyond Clojure stdlib

3. **Circular Dependency Prevention**: Detects and prevents circular dependencies between components

4. **Interface Isolation**: Ensures all ports define protocols (interfaces)

5. **Adapter Implementation**: Verifies that adapters implement at least one port

These checks run automatically in the CI/CD pipeline and locally via:

```bash
./bin/check-architecture.sh
```

The fitness functions prevent architectural degradation over time as new changes are introduced.

## API

### Evaluate Portfolio

**Endpoint**: `POST /api/evaluate`

**Request Body Example**:

```json
{
  "portfolio": [
    {
      "ticker": "PETR4",
      "pl": 8.5,
      "tag_along": 80,
      "corrupcao": false
    },
    {
      "ticker": "ITUB4",
      "pl": 12.3,
      "tag_along": 100,
      "corrupcao": false
    }
  ],
  "criterios": [
    {
      "nome": "PL < 10",
      "tipo": "numerico",
      "campo": "pl",
      "operador": "<",
      "valor": 10,
      "peso": 1.5
    },
    {
      "nome": "Tag Along 100%",
      "tipo": "booleano",
      "campo": "tag_along",
      "operador": "==",
      "valor": 100,
      "peso": 2.0
    },
    {
      "nome": "Sem escândalos de corrupção",
      "tipo": "booleano",
      "campo": "corrupcao",
      "operador": "==",
      "valor": false,
      "peso": 3.0
    }
  ]
}
```

**Response Example**:

```json
{
  "resultado": [
    {
      "ticker": "ITUB4",
      "score": 5.0
    },
    {
      "ticker": "PETR4",
      "score": 4.5
    }
  ]
}
```

### Health Check

**Endpoint**: `GET /health`

**Response**:

```json
{
  "status": "UP"
}
```

## Development

### Prerequisites

- Java JDK 11 or higher
- Leiningen

### Setup

1. Clone the repository
2. Install dependencies:

```
lein deps
```

### Running Tests

```
lein test
```

### Running the Application

```
lein run
```

Or to specify a port:

```
lein run 8000
```

### Building

To build an uberjar:

```
lein uberjar
```

## Docker

Build the Docker image:

```
docker build -t walue .
```

Run the container:

```
docker run -p 8080:8080 walue
```

## CI/CD

The project includes a GitHub Actions workflow for continuous integration and deployment. It:

1. Runs tests on every push and pull request
2. Builds and publishes a Docker image when changes are pushed to the main branch

### CI/CD Setup

To configure CI/CD for this project:

1. **GitHub Secrets**: Add the following secrets to your GitHub repository:
   - `DOCKERHUB_USERNAME`: Your DockerHub username
   - `DOCKERHUB_TOKEN`: Your DockerHub access token (create one in DockerHub account settings)

2. **Custom Docker Image Tag**: Edit the `.github/workflows/ci-cd.yml` file to update the Docker image tags:
   ```yaml
   tags: |
     yourusername/walue:latest
     yourusername/walue:${{ github.sha }}
   ```
   Replace `yourusername` with your actual DockerHub username.

3. **Branch Protection Rules**: Consider adding branch protection rules for the `main` branch in GitHub:
   - Require pull request reviews before merging
   - Require status checks to pass before merging
   - Require up-to-date branches before merging

### Deployment Instructions

#### Deploying with Docker

1. **Pull the latest image**:
   ```bash
   docker pull yourusername/walue:latest
   ```

2. **Run the container**:
   ```bash
   docker run -d -p 8080:8080 --name walue yourusername/walue:latest
   ```

3. **Check container logs**:
   ```bash
   docker logs -f walue
   ```

#### Deploying to Kubernetes

1. **Create a Kubernetes deployment file** (`k8s-deployment.yaml`):
   ```yaml
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     name: walue
     labels:
       app: walue
   spec:
     replicas: 2
     selector:
       matchLabels:
         app: walue
     template:
       metadata:
         labels:
           app: walue
       spec:
         containers:
         - name: walue
           image: yourusername/walue:latest
           ports:
           - containerPort: 8080
           resources:
             limits:
               cpu: "0.5"
               memory: "512Mi"
             requests:
               cpu: "0.2"
               memory: "256Mi"
           livenessProbe:
             httpGet:
               path: /health
               port: 8080
             initialDelaySeconds: 30
             periodSeconds: 10
           readinessProbe:
             httpGet:
               path: /health
               port: 8080
             initialDelaySeconds: 5
             periodSeconds: 5
   ---
   apiVersion: v1
   kind: Service
   metadata:
     name: walue-service
   spec:
     selector:
       app: walue
     ports:
     - port: 80
       targetPort: 8080
     type: ClusterIP
   ```

2. **Apply the deployment**:
   ```bash
   kubectl apply -f k8s-deployment.yaml
   ```

3. **Check deployment status**:
   ```bash
   kubectl get deployments
   kubectl get pods
   kubectl get services
   ```

#### Environment Variables

The application supports the following environment variables:

- `PORT`: The port to run the server on (default: 8080)
- `LOG_LEVEL`: The log level to use (default: info)

You can set these when running the Docker container:

```bash
docker run -d -p 8080:8080 -e PORT=8080 -e LOG_LEVEL=debug --name walue yourusername/walue:latest
```

Or in your Kubernetes deployment YAML:

```yaml
env:
- name: PORT
  value: "8080"
- name: LOG_LEVEL
  value: "info"
```