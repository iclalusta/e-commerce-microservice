package main

import (
	"auth-service/config"
	"auth-service/database"
	"auth-service/handlers"
	"log"
	"net/http"

	"github.com/gorilla/mux"
	_ "github.com/lib/pq"
)

func main() {
	cfg, err := config.LoadConfig()
	if err != nil {
		log.Fatalf("Yapılandırma yüklenemedi: %v", err)
	}

	db, err := database.Connect(cfg)
	if err != nil {
		log.Fatalf("Veritabanına bağlanılamadı: %v", err)
	}
	defer db.Close()

	r := mux.NewRouter()
	authHandler := handlers.NewAuthHandler(db, cfg.JWTSecret)

	authRouter := r.PathPrefix("/api/auth").Subrouter()
	authRouter.HandleFunc("/register", authHandler.Register).Methods("POST")
	authRouter.HandleFunc("/login", authHandler.Login).Methods("POST")
	authRouter.HandleFunc("/validate", authHandler.ValidateToken).Methods("POST") // yeni eklendi

	port := "8081"
	log.Printf("Auth service %s portunda başlatılıyor...", port)
	log.Fatal(http.ListenAndServe(":"+port, r))
}
