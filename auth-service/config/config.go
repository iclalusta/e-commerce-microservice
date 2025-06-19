package config

import (
	"fmt"
	"log"
	"os"

	"github.com/joho/godotenv"
)

// Config, uygulama yapılandırmasını tutar.
type Config struct {
	DBConnectionString string
	JWTSecret          string
}

// LoadConfig, .env dosyasını ve ortam değişkenlerini yükler.
func LoadConfig() (*Config, error) {
	godotenv.Load()

	dbUser := os.Getenv("DB_USER")
	dbPassword := os.Getenv("DB_PASSWORD")
	dbHost := os.Getenv("DB_HOST")
	dbPort := os.Getenv("DB_PORT")
	dbName := os.Getenv("DB_NAME")
	jwtSecret := os.Getenv("JWT_SECRET")

	if dbUser == "" || dbHost == "" || dbName == "" || jwtSecret == "" {
		log.Fatal("Gerekli ortam değişkenleri (DB_*, JWT_SECRET) tanımlanmamış.")
	}

	connectionString := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=disable",
		dbHost, dbPort, dbUser, dbPassword, dbName)

	return &Config{
		DBConnectionString: connectionString,
		JWTSecret:          jwtSecret,
	}, nil
}