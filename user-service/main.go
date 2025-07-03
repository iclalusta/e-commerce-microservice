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
	userHandler := handlers.NewUserHandler(db)

	userRouter := r.PathPrefix("/user").Subrouter()
	// Bu endpoint dışarıya açık değil, servisler arası iletişim için.
	userRouter.HandleFunc("/create", userHandler.CreateUser).Methods("POST")
	userRouter.HandleFunc("/all", userHandler.GetAllUsers).Methods("GET")  // yeni eklendi
	userRouter.HandleFunc("/{id}", userHandler.GetUserByID).Methods("GET") // yeni eklendi

	port := "8082"
	log.Printf("User service %s portunda başlatılıyor...", port)
	log.Fatal(http.ListenAndServe(":"+port, r))
}
