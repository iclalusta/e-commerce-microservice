# Stage 1: Build with Maven
FROM maven:3.8.5-openjdk-17 AS build

WORKDIR /app

# Copy the POM file and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the application source code
COPY src ./src

# Package the application
RUN mvn package -DskipTests

# Stage 2: Create the final image
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Run the JAR file
ENTRYPOINT ["java","-jar","app.jar"]