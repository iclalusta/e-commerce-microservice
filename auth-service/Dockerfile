# İlk Aşama: Bağımlılıkları İndirmek ve Uygulamayı Derlemek
FROM golang:1.24-alpine AS builder

WORKDIR /app

COPY go.mod .
COPY go.sum .

RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build -o /app/auth-service main.go

# İkinci Aşama: Daha Küçük Bir Çalışma Ortamı Oluşturmak
FROM alpine:latest

WORKDIR /app

COPY --from=builder /app/auth-service .

EXPOSE 8081 

CMD ["./auth-service"]