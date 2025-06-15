# Build stage
FROM maven:3-amazoncorretto-21-debian-bookworm AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=build /app/target/cart_service-0.0.1-SNAPSHOT.jar app.jar
# Expose the default application port
EXPOSE 8084
ENTRYPOINT ["java","-jar","/app/app.jar"]
