package config

import (
	"log"
	"os"

	"github.com/joho/godotenv"
)

type Config struct {
	ServicePort string
	DBHost      string
	DBPort      string
	DBUser      string
	DBPassword  string
	DBName      string
	JWTSecret   string // Secret key for JWT signing
}

// LoadConfig loads configuration from environment variables or .env file
func LoadConfig() *Config {
	err := godotenv.Load() // Load .env file if it exists
	if err != nil {
		log.Println("No .env file found, assuming environment variables are set.")
	}

	cfg := &Config{
		ServicePort: getEnv("AUTH_SERVICE_PORT", "8081"),
		DBHost:      getEnv("AUTH_DB_HOST", "localhost"),
		DBPort:      getEnv("AUTH_DB_PORT", "5433"), // Typically different from User DB port
		DBUser:      getEnv("AUTH_DB_USER", "authuser"),
		DBPassword:  getEnv("AUTH_DB_PASSWORD", "authpassword"),
		DBName:      getEnv("AUTH_DB_NAME", "authdb"),
		JWTSecret:   getEnv("JWT_SECRET", "supersecretjwtkey"), // IMPORTANT: Change this in production
	}

	// Basic validation
	if cfg.DBHost == "" || cfg.DBPort == "" || cfg.DBUser == "" || cfg.DBPassword == "" || cfg.DBName == "" || cfg.JWTSecret == "" {
		log.Fatalf("Missing required environment variables for database or JWT secret.")
	}

	return cfg
}

// getEnv retrieves environment variable or returns fallback
func getEnv(key, fallback string) string {
	if value, ok := os.LookupEnv(key); ok {
		return value
	}
	return fallback
}
