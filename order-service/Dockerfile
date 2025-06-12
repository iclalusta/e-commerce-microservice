# --- Build Stage ---
# This stage uses a full JDK and Maven to build your application's .jar file
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# --- Run Stage ---
# This stage uses a lightweight Java Runtime Environment (JRE) to run the application
# It results in a smaller and more secure final image.
FROM openjdk:17-jdk-slim
WORKDIR /app
# Copy only the built .jar file from the 'build' stage
COPY --from=build /app/target/order-service-0.0.1-SNAPSHOT.jar ./order-service.jar
# Expose the port your application runs on
EXPOSE 8082
# The command to run your application when the container starts
ENTRYPOINT ["java", "-jar", "order-service.jar"]