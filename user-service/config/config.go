package config

import (
	"log"
	"os"

	"github.com/joho/godotenv" // For loading .env file
)

type Config struct {
	ServicePort    string
	DBHost         string
	DBPort         string
	DBUser         string
	DBPassword     string
	DBName         string
	AuthServiceURL string // URL for the authentication service (e.g., http://auth-service:8081)
}

// LoadConfig loads configuration from environment variables or .env file
func LoadConfig() *Config {
	// Load .env file if it exists (for local development)
	err := godotenv.Load()
	if err != nil {
		log.Println("No .env file found, assuming environment variables are set.")
	}

	cfg := &Config{
		ServicePort:    getEnv("USER_SERVICE_PORT", "8080"),
		DBHost:         getEnv("DB_HOST", "localhost"),
		DBPort:         getEnv("DB_PORT", "5432"),
		DBUser:         getEnv("DB_USER", "user"),
		DBPassword:     getEnv("DB_PASSWORD", "password"),
		DBName:         getEnv("DB_NAME", "userdb"),
		AuthServiceURL: getEnv("AUTH_SERVICE_URL", "http://auth-service:8081"), // Default for Docker Compose
	}

	// Basic validation
	if cfg.DBHost == "" || cfg.DBPort == "" || cfg.DBUser == "" || cfg.DBPassword == "" || cfg.DBName == "" || cfg.AuthServiceURL == "" {
		log.Fatalf("Missing required environment variables for database or auth service URL.")
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
