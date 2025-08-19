# GDP Data Serving with Quarkus

A multi-module Quarkus application that provides both REST and GraphQL APIs for data serving, with OIDC authentication support.

## Project Structure

```
gdp-dataserving-quarkus/
├── pom.xml                 # Parent POM
├── server/                 # Server module
│   ├── pom.xml
│   └── src/main/java/com/gdp/dataserving/
│       ├── model/          # Data models
│       ├── service/        # Business logic
│       ├── rest/           # REST API endpoints
│       └── graphql/        # GraphQL API
└── client/                 # Client module
    ├── pom.xml
    └── src/main/java/com/gdp/dataserving/client/
        ├── model/          # Client-side models
        ├── rest/           # REST client
        ├── graphql/        # GraphQL client
        └── service/        # Client services
```

## Features

### Server Module
- **REST API**: JAX-RS based REST endpoints
- **GraphQL API**: SmallRye GraphQL implementation
- **OIDC Authentication**: Secure endpoints with role-based access control
- **Data Management**: CRUD operations for data items
- **Search & Filter**: Search by name/description, filter by category/status

### Client Module
- **REST Client**: MicroProfile REST Client for consuming REST APIs
- **GraphQL Client**: SmallRye GraphQL Client for consuming GraphQL APIs
- **OIDC Client**: Automatic token management and authentication
- **Demo Endpoints**: Example usage of both REST and GraphQL clients

## Technology Stack

- **Java**: 17 (OpenJDK 17.0.16)
- **Quarkus**: 3.2.12.Final
- **SmallRye GraphQL**: For GraphQL support
- **MicroProfile**: REST Client, JWT, OpenAPI
- **OIDC**: Authentication and authorization
- **Jackson**: JSON serialization
- **AWS SDK**: v1 (1.12.788) for S3 operations
- **Hibernate ORM**: with Panache for data persistence
- **Flyway**: for database migrations

## Configuration

### Server Configuration (server/src/main/resources/application.yml)

```yaml
quarkus:
  http:
    port: 8082  # Server runs on port 8082
  smallrye-graphql:
    ui:
      enable: true
      path: /graphql-ui
  oidc:
    auth-server-url: ${OIDC_AUTH_SERVER_URL:http://localhost:8080/realms/quarkus}
    client-id: ${OIDC_CLIENT_ID:backend-service}
    credentials:
      secret: ${OIDC_CLIENT_SECRET:secret}
```

### Client Configuration (client/src/main/resources/application.yml)

```yaml
quarkus:
  rest-client:
    "com.sc.gdp.client.DataServiceClient":
      url: http://localhost:8082  # Points to server on port 8082
      scope: javax.inject.Singleton
  oidc-client:
    auth-server-url: ${OIDC_AUTH_SERVER_URL:http://localhost:8080/realms/quarkus}
    client-id: ${OIDC_CLIENT_ID:frontend-service}
    credentials:
      secret: ${OIDC_CLIENT_SECRET:secret}
```

## Building and Running

### Prerequisites
- Java 17 (OpenJDK 17.0.16 or later)
- Maven 3.8.1+
- Docker (optional, for containerized deployment)
- Keycloak server (for OIDC authentication)

### Development Mode

To run the application in development mode with hot reload:

```bash
# Start the server (runs on port 8082)
cd server
mvn quarkus:dev

# In another terminal, start the client (runs on port 8080)
cd client  
mvn quarkus:dev
```

The server will be available at:
- REST API: http://localhost:8082
- GraphQL Playground: http://localhost:8082/graphql-ui
- Quarkus Dev UI: http://localhost:8082/q/dev-ui (development tools and extensions)
- Health Check: http://localhost:8082/q/health

The client demo will be available at:
- Client Demo: http://localhost:8080

### Build the Project

```bash
# Build all modules
mvn clean compile

# Package all modules
mvn clean package
```

## API Endpoints

### REST API

The server provides the following REST endpoints (available at http://localhost:8082):

- `GET /api/data` - Get all data items
- `GET /api/data/{id}` - Get data item by ID  
- `POST /api/data` - Create new data item
- `PUT /api/data/{id}` - Update data item
- `DELETE /api/data/{id}` - Delete data item
- `GET /api/s3/presigned-url/download?key={s3Key}` - Generate S3 download presigned URL
- `POST /api/s3/presigned-url/upload` - Generate S3 upload presigned URL
- `GET /q/health` - Health check endpoint
- `GET /q/metrics` - Metrics endpoint
- `GET /q/dev-ui` - Quarkus Dev UI (development mode only)

### GraphQL API

The GraphQL endpoint is available at `http://localhost:8082/graphql` with GraphQL UI at `http://localhost:8082/graphql-ui`.

Example queries:

#### Sample Queries

```graphql
# Get all data items
query {
  allDataItems {
    id
    name
    description
    value
    category
    status
  }
}

# Get data item by ID
query {
  dataItem(id: 1) {
    id
    name
    description
  }
}

# Search data items
query {
  searchDataItems(searchTerm: "sample") {
    id
    name
    description
  }
}
```

#### Sample Mutations

```graphql
# Create data item
mutation {
  createDataItem(input: {
    name: "New Item"
    description: "A new data item"
    value: "some value"
    category: "test"
  }) {
    id
    name
    createdAt
  }
}

# Update data item
mutation {
  updateDataItem(id: 1, input: {
    name: "Updated Item"
    description: "Updated description"
  }) {
    id
    name
    updatedAt
  }
}
```

### S3 API

The S3 API provides endpoints for generating presigned URLs for secure file upload and download operations.

#### Generate Download Presigned URL

```bash
GET /api/s3/presigned-url/download?key=path/to/file.jpg
```

Response:
```json
{
  "url": "https://s3.amazonaws.com/bucket/path/to/file.jpg?X-Amz-Algorithm=...",
  "operation": "download",
  "key": "path/to/file.jpg",
  "expiresInMinutes": 60
}
```

#### Generate Upload Presigned URL

```bash
POST /api/s3/presigned-url/upload
Content-Type: application/json

{
  "key": "uploads/document.pdf",
  "contentType": "application/pdf"
}
```

Response:
```json
{
  "url": "https://s3.amazonaws.com/bucket/uploads/document.pdf?X-Amz-Algorithm=...",
  "operation": "upload",
  "key": "uploads/document.pdf",
  "contentType": "application/pdf",
  "expiresInMinutes": 60
}
```

**Note**: Both endpoints require authentication and appropriate user roles (`user` or `admin`).

### Client Demo API

The client provides demo endpoints (available at http://localhost:8080):

- `GET /client-demo/test` - Test connectivity to server
- `GET /client-demo/rest/items` - Get all items via REST
- `GET /client-demo/graphql/items` - Get all items via GraphQL
- `POST /client-demo/rest/items` - Create item via REST
- `POST /client-demo/graphql/items` - Create item via GraphQL

## Authentication

The application uses OIDC for authentication. You need to:

1. Set up a Keycloak server
2. Create a realm and clients
3. Configure users with appropriate roles (`user`, `admin`)
4. Update the configuration files with your Keycloak details

### Role-based Access Control

- **user** role: Can read data items
- **admin** role: Can create, update, and delete data items

## Development

### Running in Development Mode

Both modules support Quarkus dev mode with hot reload:

```bash
# Terminal 1 - Server
cd server
mvn quarkus:dev

# Terminal 2 - Client
cd client
mvn quarkus:dev
```

### Testing

```bash
# Run tests for all modules
mvn test

# Run tests for specific module
cd server
mvn test
```

## Docker Support

Quarkus provides built-in Docker support:

```bash
# Build native image
mvn package -Pnative -Dquarkus.native.container-build=true

# Build Docker image
docker build -f src/main/docker/Dockerfile.native -t gdp-dataserving .
```

## Monitoring and Health Checks

Quarkus provides built-in health checks and metrics:

- Health: `http://localhost:8080/q/health`
- Metrics: `http://localhost:8080/q/metrics`
- OpenAPI: `http://localhost:8080/q/openapi`
- Swagger UI: `http://localhost:8080/q/swagger-ui`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.