package database

import (
	"database/sql"
	"log"
	"user-service/config"

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
	log.Println("User Service veritabanı bağlantısı başarılı.")

	createTableSQL := `
    CREATE TABLE IF NOT EXISTS users (
        id INT PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        email VARCHAR(100) UNIQUE NOT NULL,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
    );`

	_, err = db.Exec(createTableSQL)
	if err != nil {
		log.Fatalf("`users` tablosu oluşturulamadı: %v", err)
		return nil, err
	}
	log.Println("`users` tablosu hazır.")
	return db, nil
}
