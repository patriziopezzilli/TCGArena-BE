#!/bin/bash

# Deployment script for TCG Arena Backend
# Usage: ./deploy.sh [environment]
# Environment: local, test, prod (default: prod)

set -e

ENVIRONMENT=${1:-prod}
PROJECT_NAME="tcg-arena-backend"
JAR_FILE="target/*.jar"

echo "🚀 Starting deployment for environment: $ENVIRONMENT"

# Clean and build
echo "📦 Building application..."
./mvnw clean package -DskipTests

# Check if JAR exists
if [ ! -f $JAR_FILE ]; then
    echo "❌ JAR file not found. Build failed."
    exit 1
fi

echo "✅ Build successful"

# Environment specific configurations
case $ENVIRONMENT in
    local)
        echo "🏠 Running locally..."
        java -jar $JAR_FILE --spring.profiles.active=local
        ;;
    test)
        echo "🧪 Running tests..."
        ./mvnw test
        ;;
    prod)
        echo "🌐 Preparing for production deployment..."

        # Here you would add commands for your deployment platform
        # For example, for Render:
        # echo "Deploying to Render..."
        # render deploy

        # For Docker:
        # echo "Building Docker image..."
        # docker build -t $PROJECT_NAME .
        # docker tag $PROJECT_NAME $PROJECT_NAME:latest

        echo "✅ Production build ready"
        echo "📋 Next steps:"
        echo "   1. Push to your Git repository"
        echo "   2. Deploy via your cloud platform (Render, Heroku, etc.)"
        echo "   3. Update environment variables"
        ;;
    *)
        echo "❌ Unknown environment: $ENVIRONMENT"
        echo "Usage: $0 [local|test|prod]"
        exit 1
        ;;
esac

echo "🎉 Deployment script completed successfully!"