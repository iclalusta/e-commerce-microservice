package database

import (
	"database/sql"
	"log"
	"auth-service/config"

	_ "github.com/lib/pq"
)

func Connect(cfg *config.Config) (*sql.DB, error) {
	db, err := sql.Open("postgres", cfg.DBConnectionString)
	if err != nil {
		return nil, err
	}

	if err = db.Ping(); err != nil {
		return nil, err
	}
	log.Println("Auth Service veritabanı bağlantısı başarılı.")

	createTableSQL := `
    CREATE TABLE IF NOT EXISTS auth_users (
        id SERIAL PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        email VARCHAR(100) UNIQUE NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
    );`
	_, err = db.Exec(createTableSQL)
	if err != nil {
		log.Fatalf("`auth_users` tablosu oluşturulamadı: %v", err)
		return nil, err
	}
	log.Println("`auth_users` tablosu hazır.")
	return db, nil
}