package main

import (
	"log"
	"net/http"
	"user-service/config"
	"user-service/database"
	"user-service/handlers"

	"github.com/gorilla/mux"
	_ "github.com/lib/pq"
)

func main() {
	// Yapılandırmayı yükle
	cfg, err := config.LoadConfig()
	if err != nil {
		log.Fatalf("Yapılandırma yüklenemedi: %v", err)
	}

	// Veritabanı bağlantısını başlat
	db, err := database.Connect(cfg.DBConnectionString)
	if err != nil {
		log.Fatalf("Veritabanına bağlanılamadı: %v", err)
	}
	defer db.Close()

	// Router ve Handler'ları oluştur
	r := mux.NewRouter()
	userHandler := handlers.NewUserHandler(db)

	// `/user` önekine sahip bir alt router oluştur
	userRouter := r.PathPrefix("/user").Subrouter()

	// Rotaları tanımla
	userRouter.HandleFunc("/register", userHandler.RegisterUser).Methods("POST")
	userRouter.HandleFunc("/email/{email}", userHandler.GetUserByEmail).Methods("GET")

	// Sunucuyu başlat
	port := "8082"
	log.Printf("User service %s portunda başlatılıyor...", port)
	if err := http.ListenAndServe(":"+port, r); err != nil {
		log.Fatalf("Sunucu başlatılamadı: %v", err)
	}
}