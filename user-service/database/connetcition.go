package database

import (
	"database/sql"
	"log"

	_ "github.com/lib/pq" // PostgreSQL sürücüsü
)

// Connect veritabanına bağlanır ve tabloyu oluşturur.
func Connect(connectionString string) (*sql.DB, error) {
	db, err := sql.Open("postgres", connectionString)
	if err != nil {
		return nil, err
	}

	if err = db.Ping(); err != nil {
		return nil, err
	}

	log.Println("Veritabanı bağlantısı başarılı.")

	// `users` tablosunu oluştur
	createTableSQL := `
    CREATE TABLE IF NOT EXISTS users (
        id SERIAL PRIMARY KEY,
        username VARCHAR(50) UNIQUE NOT NULL,
        email VARCHAR(100) UNIQUE NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
    );`

	_, err = db.Exec(createTableSQL)
	if err != nil {
		log.Fatalf("Tablo oluşturulamadı: %v", err)
		return nil, err
	}

	log.Println("`users` tablosu hazır.")
	return db, nil
}