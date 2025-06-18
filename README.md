# Mailchimp to MailerLite Migrator

[![CI](https://github.com/your-username/mailchimp-to-mailerlite-migrator/actions/workflows/ci.yml/badge.svg)](https://github.com/your-username/mailchimp-to-mailerlite-migrator/actions/workflows/ci.yml)
[![Code Quality](https://github.com/your-username/mailchimp-to-mailerlite-migrator/actions/workflows/quality.yml/badge.svg)](https://github.com/your-username/mailchimp-to-mailerlite-migrator/actions/workflows/quality.yml)
[![Docker](https://github.com/your-username/mailchimp-to-mailerlite-migrator/actions/workflows/docker.yml/badge.svg)](https://github.com/your-username/mailchimp-to-mailerlite-migrator/actions/workflows/docker.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A comprehensive Spring Boot application for seamlessly migrating your email marketing data from Mailchimp to MailerLite. This tool handles subscribers, tags/groups, e-commerce data, and provides guidance for campaign migration.

## 🚀 Features

- **Complete Data Migration**: Migrate subscribers, tags, e-commerce shops, products, and categories
- **Real-time Progress Tracking**: WebSocket-powered live updates during migration
- **Intuitive Web Interface**: Clean, responsive UI built with Thymeleaf and TailwindCSS
- **Pre-Migration Analysis**: Analyze your Mailchimp data before starting migration
- **Error Handling & Retry Logic**: Robust error handling with retry mechanisms
- **Rate Limit Compliance**: Built-in rate limiting for both APIs
- **Batch Processing**: Efficient batch processing for large datasets
- **Campaign Migration Guidance**: Step-by-step instructions for recreating campaigns

## 📋 Prerequisites

- Java 21 or later
- Maven 3.6 or later
- Mailchimp API key
- MailerLite API token

## 🛠️ Installation

### Option 1: Run from Source

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/mailchimp-to-mailerlite-migrator.git
   cd mailchimp-to-mailerlite-migrator
   ```

2. **Build the application**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application**
   Open your browser and navigate to `http://localhost:8080`

### Option 2: Docker

1. **Using Docker Compose (Recommended)**
   ```bash
   git clone https://github.com/your-username/mailchimp-to-mailerlite-migrator.git
   cd mailchimp-to-mailerlite-migrator
   docker-compose up
   ```

2. **Using Docker directly**
   ```bash
   docker run -p 8080:8080 ghcr.io/your-username/mailchimp-to-mailerlite-migrator:latest
   ```

3. **Building Docker image locally**
   ```bash
   docker build -t mailchimp-migrator .
   docker run -p 8080:8080 mailchimp-migrator
   ```

## 📖 Usage

### Step 1: Configure API Keys

1. Navigate to `http://localhost:8080`
2. Enter your Mailchimp API key and MailerLite API token
3. Click "Configure & Proceed to Dashboard"

#### Getting API Keys

**Mailchimp API Key:**
1. Log into your Mailchimp account
2. Go to Account → Extras → API keys
3. Generate a new API key
4. Format: `your-key-datacenter` (e.g., `abc123def456-us6`)

**MailerLite API Token:**
1. Log into your MailerLite account
2. Go to Integrations → MailerLite API
3. Generate a new token

### Step 2: Validate Connections

1. Click "Test Connections" to verify your API credentials
2. Run "Analyze Data" to see migration statistics
3. Review the pre-migration analysis

### Step 3: Start Migration

1. Go to the Dashboard
2. Click "Start Migration"
3. Monitor real-time progress via the dashboard
4. Review any errors in the error log

### Step 4: Post-Migration Setup

After migration completes:

1. **Review Migrated Data**: Check your MailerLite account for all subscribers and groups
2. **Set Up Automations**: Create welcome emails and purchase-based sequences
3. **Update Forms**: Replace Mailchimp forms with MailerLite forms on your website
4. **Schedule Campaigns**: Set up your newsletter campaign schedule

## 🔧 Configuration

The application can be configured via `application.yml`:

```yaml
migration:
  batch-size: 500  # Number of subscribers to process in each batch
  retry:
    max-attempts: 3  # Maximum retry attempts for failed operations
    backoff-delay: 1000  # Delay between retries (ms)
  rate-limit:
    mailchimp:
      connections: 10  # Max concurrent connections
      timeout: 120000  # Request timeout (ms)
    mailerlite:
      requests-per-minute: 120  # Rate limit compliance
```

## 📊 Migration Process

The migration follows these phases:

1. **Initialization**: Validate connections and prepare for migration
2. **Tag/Group Migration**: Convert Mailchimp tags to MailerLite groups
3. **E-commerce Setup**: Migrate shops, categories, and products
4. **Subscriber Migration**: Transfer all subscribers with their data
5. **Campaign Guidance**: Provide instructions for manual campaign recreation

## 🧪 Testing

The project includes comprehensive tests with WireMock for API integration testing.

```bash
# Run all tests
mvn test

# Run tests with coverage
mvn clean verify

# Run specific test class
mvn test -Dtest=MailchimpServiceIntegrationTest

# Run integration tests only
mvn failsafe:integration-test
```

## 🔄 CI/CD Pipeline

The project includes a comprehensive CI/CD pipeline with GitHub Actions:

### Workflows

1. **CI Workflow** (`.github/workflows/ci.yml`)
   - Runs on every push and pull request
   - Tests against Java 21 and 22
   - Executes `./mvnw verify`
   - Generates test reports and coverage
   - Uploads build artifacts

2. **Code Quality** (`.github/workflows/quality.yml`)
   - Code formatting checks with Spotless
   - SonarCloud analysis (if configured)
   - Codecov integration
   - PR coverage comments

3. **Security Scan** (`.github/workflows/ci.yml`)
   - OWASP dependency vulnerability scanning
   - Security report generation

4. **Docker Build** (`.github/workflows/docker.yml`)
   - Multi-platform Docker builds (AMD64, ARM64)
   - Publishes to GitHub Container Registry
   - Automatic tagging based on branches/tags

5. **Release** (`.github/workflows/release.yml`)
   - Triggered on GitHub releases
   - Creates release artifacts (JAR, TAR.GZ, ZIP)
   - Automatic version management

### Status Badges

Add these badges to your repository README:

```markdown
[![CI](https://github.com/your-username/mailchimp-to-mailerlite-migrator/actions/workflows/ci.yml/badge.svg)](https://github.com/your-username/mailchimp-to-mailerlite-migrator/actions/workflows/ci.yml)
[![Code Quality](https://github.com/your-username/mailchimp-to-mailerlite-migrator/actions/workflows/quality.yml/badge.svg)](https://github.com/your-username/mailchimp-to-mailerlite-migrator/actions/workflows/quality.yml)
[![Docker](https://github.com/your-username/mailchimp-to-mailerlite-migrator/actions/workflows/docker.yml/badge.svg)](https://github.com/your-username/mailchimp-to-mailerlite-migrator/actions/workflows/docker.yml)
```

## 🏗️ Architecture

```
src/main/java/digital/pragmatech/
├── config/                 # Configuration classes
├── controller/             # REST controllers and web endpoints
├── service/
│   ├── mailchimp/         # Mailchimp API integration
│   ├── mailerlite/        # MailerLite API integration
│   └── migration/         # Migration orchestration and progress tracking
├── model/
│   ├── common/            # Shared domain models
│   ├── mailchimp/         # Mailchimp-specific DTOs
│   └── mailerlite/        # MailerLite-specific DTOs
├── dto/                   # Request/Response DTOs
└── exception/             # Custom exceptions
```

## 🔒 Security Considerations

- API keys are stored in memory only during runtime
- HTTPS is recommended for production deployment
- No sensitive data is logged
- API keys are not persisted to disk

## 📈 Performance

- Batch processing for efficient data transfer
- Rate limiting to comply with API restrictions
- Concurrent processing where possible
- Progress tracking with minimal overhead

## 🐛 Troubleshooting

### Common Issues

**Connection Failed**
- Verify API keys are correct and have necessary permissions
- Check network connectivity
- Ensure API endpoints are accessible

**Migration Stuck**
- Check error logs for specific issues
- Verify rate limits aren't being exceeded
- Try pausing and resuming the migration

**Missing Data**
- Some data might not be available via API
- Check Mailchimp export options for complete data
- Verify MailerLite import limits

### Debug Mode

Enable debug logging by adding to `application.yml`:

```yaml
logging:
  level:
    digital.pragmatech: DEBUG
    org.springframework.web.client.RestClient: DEBUG
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow Spring Boot best practices
- Write tests for new features
- Update documentation for significant changes
- Use conventional commit messages

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Mailchimp API Documentation](https://mailchimp.com/developer/marketing/api/)
- [MailerLite API Documentation](https://developers.mailerlite.com/)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [TailwindCSS](https://tailwindcss.com/)
- [WireMock](https://wiremock.org/)

## 📞 Support

If you encounter any issues or have questions:

1. Check the [troubleshooting section](#-troubleshooting)
2. Search existing [GitHub issues](https://github.com/your-username/mailchimp-to-mailerlite-migrator/issues)
3. Create a new issue with detailed information

## 🗺️ Roadmap

- [ ] Docker containerization
- [ ] Database persistence for migration history
- [ ] Advanced filtering options
- [ ] Webhook support for real-time updates
- [ ] CLI version for headless operations
- [ ] Support for additional email providers

---

**⚠️ Important Notice**: Always test migrations with a small subset of data first. While this tool is designed to be safe and reliable, it's recommended to backup your data before migration.