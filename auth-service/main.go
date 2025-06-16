package main

import (
	"log"
	"net/http"
	"auth-service/config"
	"auth-service/database"
	"auth-service/handlers"

	"github.com/gorilla/mux"
	_ "github.com/lib/pq"
)

func main() {
	cfg, err := config.LoadConfig()
	if err != nil {
		log.Fatalf("Yapılandırma yüklenemedi: %v", err)
	}

	db, err := database.Connect(cfg.DBConnectionString)
	if err != nil {
		log.Fatalf("Veritabanına bağlanılamadı: %v", err)
	}
	defer db.Close()

	r := mux.NewRouter()
	authHandler := handlers.NewAuthHandler(db, cfg.JWTSecret)

	// `/auth` önekine sahip bir alt router oluştur
	authRouter := r.PathPrefix("/auth").Subrouter()

	// Rotaları tanımla
	authRouter.HandleFunc("/login", authHandler.Login).Methods("POST")
	authRouter.HandleFunc("/validate", authHandler.ValidateToken).Methods("GET") // Validate endpoint'i

	port := "8081"
	log.Printf("Auth service %s portunda başlatılıyor...", port)
	if err := http.ListenAndServe(":"+port, r); err != nil {
		log.Fatalf("Sunucu başlatılamadı: %v", err)
	}
}