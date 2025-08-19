# Changelog

All notable changes to the GDP DataServing Quarkus project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- CHANGELOG.md to track version history and breaking changes
- Comprehensive documentation updates for Java 17 migration
- Health check and metrics endpoints documentation
- Quarkus Dev UI for additional development tools and extension management

### Changed
- **BREAKING**: Upgraded Java runtime from Java 11 to Java 17 (OpenJDK 17.0.16)
- **BREAKING**: Updated server port from 8080 to 8082 to avoid conflicts
- Updated client port from 8081 to 8080 for consistency
- Updated Maven compiler source and target to Java 17
- Enhanced README.md with current technology stack and configuration
- Updated client configuration to point to correct server port (8082)

### Technical Details
- Java version: 17 (OpenJDK 17.0.16)
- Quarkus version: 3.2.12.Final
- AWS SDK: v1 (1.12.788) for S3 operations
- Server runs on port 8082
- Client runs on port 8080
- GraphQL UI available at http://localhost:8082/graphql-ui

## [1.1.0] - S3 Library Migration

### Changed
- **BREAKING**: Migrated S3 library from AWS SDK v2 to AWS SDK v1 (1.12.788)
- Updated S3 service implementation to use AWS SDK v1 patterns
- Changed dependency from `software.amazon.awssdk:s3` to `com.amazonaws:aws-java-sdk-s3`
- Updated method signatures and return types in S3Service
- Modified logging configuration for AWS SDK v1

### Migration Notes
- `HeadObjectResponse` is now `ObjectMetadata`
- `ResponseInputStream<GetObjectResponse>` is now `S3Object`
- Metadata access methods changed (e.g., `contentType()` → `getContentType()`)
- Update logging configuration from `software.amazon.awssdk` to `com.amazonaws`

### Added
- Comprehensive S3 library documentation with migration guide
- Version history and troubleshooting sections in S3 README
- Future enhancement roadmap for S3 library

## [1.0.0] - Initial Release

### Added
- Quarkus-based microservices architecture with server and client modules
- REST API endpoints for data operations
- GraphQL API with interactive UI
- OIDC authentication and authorization
- MicroProfile integration (REST Client, JWT, OpenAPI)
- S3 integration for file operations
- Hibernate ORM with Panache for data persistence
- Flyway for database migrations
- Docker support for containerized deployment
- Health checks and metrics endpoints
- Development mode with hot reload support

### Features
- Multi-module Maven project structure
- Server module with REST and GraphQL APIs
- Client module with demo endpoints
- S3 common library for file operations
- Comprehensive configuration management
- Security with OIDC integration
- Database integration with migration support