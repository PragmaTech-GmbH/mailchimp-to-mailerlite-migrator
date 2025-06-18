#!/bin/bash

# Mailchimp to MailerLite Migrator - Run Script
# Usage: ./run.sh

echo "🚀 Starting Mailchimp to MailerLite Migrator..."
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
if command -v mvn &> /dev/null; then
    echo "✅ Maven detected"
else
    echo "❌ Maven not found. Please install Maven 3.6 or later."
    exit 1
fi

echo ""
echo "📦 Building application..."
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ Build successful"
else
    echo "❌ Build failed"
    exit 1
fi

echo ""
echo "🔥 Starting application on http://localhost:8080"
echo ""
echo "📋 Once the application starts:"
echo "   1. Open http://localhost:8080 in your browser"
echo "   2. Enter your Mailchimp API key and MailerLite API token"
echo "   3. Follow the migration wizard"
echo ""
echo "💡 To stop the application, press Ctrl+C"
echo ""

java -jar target/mailchimp-to-mailerlite-migrator-0.0.1-SNAPSHOT.jar