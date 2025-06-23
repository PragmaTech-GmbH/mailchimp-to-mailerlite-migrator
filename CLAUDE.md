# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5.0 application for migrating email marketing data from Mailchimp to MailerLite. The application uses Java 21, Maven for build management, and provides a web UI built with Thymeleaf and HTMX.

## Essential Commands

### Build and Test
```bash
# Build the project (includes running tests)
./mvnw clean install

# Run all tests
./mvnw test

# Run tests with coverage and verification
./mvnw verify

# Run specific test class
./mvnw test -Dtest=MailchimpServiceIntegrationTest

# Run only integration tests
./mvnw failsafe:integration-test

# Run code formatting check
./mvnw spotless:check

# Apply code formatting
./mvnw spotless:apply

# Run security vulnerability scan
./mvnw dependency-check:check
```

### Development
```bash
# Run the application in development mode with hot reload
./run.sh

# Run the application without devtools (production-like)
./mvnw spring-boot:run

# Run with Docker Compose
docker compose up

# Build Docker image
docker build -t mailchimp-migrator .
```

### Development Features
The `./run.sh` script enables several development-friendly features:

- **Hot Reload**: Code changes automatically restart the application
- **LiveReload**: Browser refreshes when static files (CSS, JS, templates) change
- **Template Hot Reload**: Thymeleaf templates reload without restart
- **Extended Logging**: More detailed logging for debugging
- **Faster Migration Settings**: Reduced batch sizes and timeouts for testing
- **Development Profile**: Loads `application-dev.yml` configuration

**LiveReload Browser Extension**: For automatic browser refresh, install the LiveReload browser extension and enable it on localhost:8080.

### Environment Variables Support
The application supports pre-configuring API keys via environment variables:
- Create a `.env` file based on `.env.example`
- Set `MAILCHIMP_API_KEY` and `MAILERLITE_API_TOKEN`
- Run `./run.sh` to automatically load these variables

## Architecture Overview

### Core Components

1. **API Clients** (`service/mailchimp/MailchimpApiClient.java`, `service/mailerlite/MailerLiteApiClient.java`)
   - Handle direct API communication with external services
   - Implement rate limiting and retry logic
   - Use Spring's RestClient for HTTP operations

2. **Service Layer** (`service/mailchimp/MailchimpService.java`, `service/mailerlite/MailerLiteService.java`)
   - Business logic for data transformation
   - Batch processing implementation
   - Error handling and recovery

3. **Migration Orchestrator** (`service/migration/MigrationOrchestrator.java`)
   - Coordinates the entire migration process
   - Manages migration phases: initialization, tag/group migration, e-commerce setup, subscriber migration
   - Handles progress tracking via WebSocket

4. **WebSocket Integration** (`config/WebSocketConfig.java`, `service/migration/MigrationProgressTracker.java`)
   - Real-time progress updates to the web UI
   - Uses STOMP protocol over WebSocket
   - Broadcasts migration status, progress, and errors

### Key Design Patterns

- **Rate Limiting**: Configured via `application.yml` properties under `migration.rate-limit`
- **Batch Processing**: Configurable batch sizes (default: 500) to handle large datasets efficiently
- **Retry Logic**: Exponential backoff with configurable max attempts
- **Progress Tracking**: Event-driven updates via WebSocket for real-time UI updates

### Testing Strategy

- **Unit Tests**: Standard JUnit tests for individual components
- **Web Layer Tests**: Use `@WebMvcTest` with HtmlUnit for controller testing
- **Integration Tests**: Use WireMock to mock external API calls
- **Test Separation**: 
  - Unit tests: `*Test.java`, `*Tests.java` (run by Surefire)
  - Integration tests: `*IntegrationTest.java`, `*IT.java` (run by Failsafe)

### Controller Testing with HtmlUnit

The project includes comprehensive controller tests using `@WebMvcTest` and HtmlUnit:
- **Form validation testing**: Tests HTML form submissions and validations
- **JavaScript-enabled testing**: Tests dynamic UI interactions
- **Graceful degradation**: Tests functionality without JavaScript
- **Error handling**: Tests various error scenarios and user feedback

### Configuration

The application uses YAML configuration with the following key sections:
- `migration.*`: Migration-specific settings (batch size, retry, rate limits)
- `api.*`: External API endpoints
- `management.*`: Spring Boot Actuator endpoints for monitoring

### Code Style

- Google Java Format is enforced via Spotless Maven plugin
- Lombok is used to reduce boilerplate code
- Follow Spring Boot conventions for dependency injection and configuration