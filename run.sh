#!/bin/bash

# Mailchimp to MailerLite Migrator - Run Script
# Usage: ./run.sh

echo "🚀 Starting Mailchimp to MailerLite Migrator..."
echo ""

# Load environment variables from .env file if it exists
if [ -f .env ]; then
    echo "📄 Loading environment variables from .env file..."
    set -a
    source .env
    set +a
    echo "✅ Environment variables loaded"
    
    # Display loaded API keys (masked for security)
    if [ ! -z "$MAILCHIMP_API_KEY" ]; then
        echo "   • MAILCHIMP_API_KEY: ${MAILCHIMP_API_KEY:0:10}...${MAILCHIMP_API_KEY: -4}"
    fi
    if [ ! -z "$MAILERLITE_API_TOKEN" ]; then
        echo "   • MAILERLITE_API_TOKEN: ${MAILERLITE_API_TOKEN:0:10}...${MAILERLITE_API_TOKEN: -4}"
    fi
else
    echo "ℹ️  No .env file found. You can create one to pre-configure API keys:"
    echo "   Create a .env file with:"
    echo "   MAILCHIMP_API_KEY=your-mailchimp-api-key"
    echo "   MAILERLITE_API_TOKEN=your-mailerlite-api-token"
fi

echo ""

# Check if Java 21 is installed
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 21 ]; then
        echo "✅ Java $JAVA_VERSION detected"
    else
        echo "❌ Java 21 or later is required. Current version: $JAVA_VERSION"
        exit 1
    fi
else
    echo "❌ Java not found. Please install Java 21 or later."
    exit 1
fi

# Check if Maven is installed
if command -v ./mvnw &> /dev/null; then
    echo "✅ Maven detected"
else
    echo "❌ Maven not found. Please install Maven 3.6 or later."
    exit 1
fi

echo ""

echo ""
echo "🔥 Starting application in DEVELOPMENT mode on http://localhost:8080"
echo ""
echo "📋 Once the application starts:"
echo "   1. Open http://localhost:8080 in your browser"
if [ -z "$MAILCHIMP_API_KEY" ] || [ -z "$MAILERLITE_API_TOKEN" ]; then
    echo "   2. Enter your Mailchimp API key and MailerLite API token"
else
    echo "   2. Your API keys are pre-configured from .env file"
fi
echo "   3. Follow the migration wizard"
echo ""
echo "🔄 Development Features Enabled:"
echo "   • Hot reload - Code changes will automatically restart the app"
echo "   • LiveReload - Browser will refresh when static files change"
echo "   • Extended classpath monitoring"
echo ""
echo "💡 To stop the application, press Ctrl+C"
echo ""

# Set development profile and enable devtools features
export SPRING_PROFILES_ACTIVE=dev
export SPRING_DEVTOOLS_RESTART_ENABLED=true
export SPRING_DEVTOOLS_LIVERELOAD_ENABLED=true

# Run with spring-boot-devtools optimizations
./mvnw spring-boot:run \
    -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=true -Dspring.devtools.livereload.enabled=true -Dspring.devtools.restart.poll-interval=2s -Dspring.devtools.restart.quiet-period=1s" \
    -Dspring-boot.run.fork=true