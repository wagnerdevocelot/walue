FROM clojure:openjdk-17-lein-slim-buster AS builder

WORKDIR /app

# Copy the project files
COPY project.clj /app/
COPY src /app/src/

# Download dependencies and build the uberjar
RUN lein deps
RUN lein uberjar

# Create a minimal runtime image
FROM openjdk:17-slim-buster

WORKDIR /app

# Copy the uberjar from the builder stage
COPY --from=builder /app/target/uberjar/walue-*-standalone.jar /app/walue.jar

# Expose the application port
EXPOSE 8080

# Set the entrypoint to run the application
ENTRYPOINT ["java", "-jar", "/app/walue.jar"]