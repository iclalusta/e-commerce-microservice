# Stage 1: Build the Go binary
FROM golang:1.24-alpine AS builder

RUN apk add --no-cache build-base
WORKDIR /app

COPY go.mod go.sum ./
RUN go mod download

COPY . .

# Çıktı dosyasına "auth-service" adını veriyoruz
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -o /app/auth-service .

# Stage 2: Create the final, minimal image
FROM alpine:latest

WORKDIR /root/
RUN apk add --no-cache ca-certificates

# "auth-service" dosyasını kopyalıyoruz
COPY --from=builder /app/auth-service .

# "auth-service" dosyasını çalıştırıyoruz
CMD ["./auth-service"]