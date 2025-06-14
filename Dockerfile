# Start with a base image containing Java runtime
FROM openjdk:21-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the built jar file into the container
COPY target/transaction-system-0.0.1-SNAPSHOT.jar /app/transaction-system.jar

# Expose the port the application runs on
EXPOSE 8080

# Set the command to run the application
CMD ["java", "-jar", "transaction-system.jar"]