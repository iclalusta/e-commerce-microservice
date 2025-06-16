# Stage 1: Build the Go binary
FROM golang:1.24-alpine AS builder
WORKDIR /app
COPY go.mod ./
COPY go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o /main

# Stage 2: Create a minimal final image
FROM alpine:latest
WORKDIR /
COPY --from=builder /main /main
EXPOSE 8081
CMD ["/main"]