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
}

// LoadConfig, .env dosyasını ve ortam değişkenlerini yükler.
func LoadConfig() (*Config, error) {
	// Docker dışında lokalde çalışırken .env dosyasını yüklemeye çalışır.
	// Hata vermezse devam eder, çünkü asıl olan ortam değişkenleridir.
	godotenv.Load()

	dbUser := os.Getenv("DB_USER")
	dbPassword := os.Getenv("DB_PASSWORD")
	dbHost := os.Getenv("DB_HOST")
	dbPort := os.Getenv("DB_PORT")
	dbName := os.Getenv("DB_NAME")

	// Değişkenlerin varlığını kontrol et
	if dbUser == "" || dbHost == "" || dbPort == "" || dbName == "" {
		log.Fatal("Veritabanı için gerekli ortam değişkenleri (DB_USER, DB_HOST, DB_PORT, DB_NAME) tanımlanmamış.")
	}

	// PostgreSQL bağlantı dizesini oluştur
	connectionString := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=disable",
		dbHost, dbPort, dbUser, dbPassword, dbName)

	return &Config{
		DBConnectionString: connectionString,
	}, nil
}
