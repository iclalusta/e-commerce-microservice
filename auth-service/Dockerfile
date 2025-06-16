# Stage 1: Build the Go binary
FROM golang:1.21-alpine AS builder

WORKDIR /app

# Copy go.mod and go.sum and download dependencies
# Eğer go.mod dosyanız yoksa, vendor dizinini kullanmak için `go mod vendor` komutunu çalıştırın.
COPY go.mod go.sum ./
RUN go mod tidy
RUN go mod vendor

# Copy the entire source code
COPY . .

# Build the application
RUN CGO_ENABLED=0 GOOS=linux go build -o /auth-service .

# Stage 2: Create the final, minimal image
FROM alpine:latest

WORKDIR /root/

# Copy the compiled binary from the builder stage
COPY --from=builder /auth-service .

# Expose port 8081 for internal communication
EXPOSE 8081

# The command to run the application
CMD ["./auth-service"]